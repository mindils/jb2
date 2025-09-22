package ru.mindils.jb2.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.mindils.jb2.app.entity.LLMModel;
import ru.mindils.jb2.app.repository.LLMModelRepository;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResilientLLMService {

  private static final Logger log = LoggerFactory.getLogger(ResilientLLMService.class);

  private final LLMModelRepository llmModelRepository;
  private final Map<Long, ChatClient> chatClientCache = new HashMap<>();

  private LLMModel currentModel;
  private ChatClient currentChatClient;

  public ResilientLLMService(LLMModelRepository llmModelRepository) {
    this.llmModelRepository = llmModelRepository;
    // Убираем инициализацию из конструктора
  }

  private void ensureInitialized() {
    if (currentModel == null || currentChatClient == null) {
      initializeDefaultModel();
    }
  }

  private void initializeDefaultModel() {
    try {
      log.debug("Initializing default LLM model...");

      // Попробуем найти модель по умолчанию
      currentModel = llmModelRepository.findDefault()
          .or(() -> {
            log.debug("No default model found, looking for available models...");
            return llmModelRepository.findAllAvailableOrderedByPriority().stream().findFirst();
          })
          .orElse(null);

      if (currentModel != null) {
        log.info("Found model: {} ({})", currentModel.getName(), currentModel.getModelName());
        currentChatClient = createChatClient(currentModel);
        log.info("Successfully initialized with model: {}", currentModel.getName());
      } else {
        log.error("No available LLM models found in database! Please create at least one model.");
        // Попробуем показать что есть в базе
        try {
          List<LLMModel> allModels = llmModelRepository.findAll();
          log.error("Total models in database: {}", allModels.size());
          for (LLMModel model : allModels) {
            log.error("Model: {} (enabled: {}, default: {}, available: {})",
                model.getName(), model.getEnabled(), model.getIsDefault(), model.isAvailable());
          }
        } catch (Exception e2) {
          log.error("Failed to query models for debugging", e2);
        }
      }
    } catch (Exception e) {
      log.error("Failed to initialize default model", e);
    }
  }

  public String callLLM(String prompt) {
    return callLLM(prompt, null);
  }

  public String callLLM(String prompt, OpenAiChatOptions additionalOptions) {
    ensureInitialized();

    if (currentModel == null || currentChatClient == null) {
      throw new RuntimeException("No LLM model available after initialization attempt");
    }

    List<LLMModel> availableModels = llmModelRepository.findAllAvailableOrderedByPriority();

    for (LLMModel model : availableModels) {
      try {
        ChatClient client = getChatClient(model);

        // Создаем опции для модели
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
            .model(model.getModelName())
            .temperature(model.getTemperature())
            .maxTokens(model.getMaxTokens());

        // Добавляем дополнительные опции если есть
        if (additionalOptions != null) {
          if (additionalOptions.getTemperature() != null) {
            optionsBuilder.temperature(additionalOptions.getTemperature());
          }
          if (additionalOptions.getMaxTokens() != null) {
            optionsBuilder.maxTokens(additionalOptions.getMaxTokens());
          }
        }

        String response = client.prompt()
            .user(prompt)
            .options(optionsBuilder.build())
            .call()
            .content();

        // Успешный ответ - записываем успех и делаем модель текущей
        llmModelRepository.recordSuccess(model);
        updateCurrentModel(model, client);

        log.debug("Successfully called LLM model: {}", model.getName());
        return response;

      } catch (Exception e) {
        log.warn("Failed to call LLM model: {} - {}", model.getName(), e.getMessage());
        llmModelRepository.recordFailure(model);

        // Удаляем из кэша если была ошибка
        chatClientCache.remove(model.getId());

        // Если это была текущая модель, сбросим её
        if (model.equals(currentModel)) {
          currentModel = null;
          currentChatClient = null;
        }

        // Проверяем специальные случаи для openRouter
        if (isRateLimitError(e) || isQuotaExceededError(e)) {
          log.warn("Rate limit or quota exceeded for model: {}", model.getName());
          continue; // Пробуем следующую модель
        }

        continue; // Пробуем следующую модель
      }
    }

    throw new RuntimeException("All available LLM models failed");
  }

  private ChatClient getChatClient(LLMModel model) {
    return chatClientCache.computeIfAbsent(model.getId(), id -> createChatClient(model));
  }

  private ChatClient createChatClient(LLMModel model) {
    try {
      HttpClient httpClient = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .connectTimeout(Duration.ofSeconds(model.getTimeoutSeconds()))
          .build();

      OpenAiApi api = OpenAiApi.builder()
          .baseUrl(model.getBaseUrl())
          .apiKey(model.getApiKey() != null ? new SimpleApiKey(model.getApiKey()) : new NoopApiKey())
          .webClientBuilder(WebClient.builder()
              .clientConnector(new JdkClientHttpConnector(httpClient)))
          .restClientBuilder(RestClient.builder()
              .requestFactory(new JdkClientHttpRequestFactory(httpClient)))
          .build();

      OpenAiChatModel chatModel = new OpenAiChatModel(
          api,
          OpenAiChatOptions.builder()
              .model(model.getModelName())
              .temperature(model.getTemperature() != null ? model.getTemperature() : 0.1f)
              .maxTokens(model.getMaxTokens() != null ? model.getMaxTokens() : 1000)
              .build()
      );

      return ChatClient.builder(chatModel).build();
    } catch (Exception e) {
      log.error("Failed to create chat client for model: {}", model.getName(), e);
      throw new RuntimeException("Failed to create chat client for model: " + model.getName(), e);
    }
  }

  private void updateCurrentModel(LLMModel model, ChatClient client) {
    this.currentModel = model;
    this.currentChatClient = client;
  }

  private boolean isRateLimitError(Exception e) {
    String message = e.getMessage();
    if (message == null) return false;
    return message.contains("429") ||
        message.contains("rate limit") ||
        message.contains("Rate limit") ||
        message.contains("too many requests");
  }

  private boolean isQuotaExceededError(Exception e) {
    String message = e.getMessage();
    if (message == null) return false;
    return message.contains("quota") ||
        message.contains("insufficient credits") ||
        message.contains("billing") ||
        message.contains("402");
  }

  public LLMModel getCurrentModel() {
    return currentModel;
  }

  public void resetModelFailures(Long modelId) {
    llmModelRepository.resetFailureCount(modelId);
    // Очищаем кэш чтобы пересоздать клиента
    chatClientCache.remove(modelId);

    // Если это была наша текущая модель, переинициализируем
    if (currentModel != null && currentModel.getId().equals(modelId)) {
      initializeDefaultModel();
    }
  }

  public List<LLMModel> getAvailableModels() {
    return llmModelRepository.findAllAvailableOrderedByPriority();
  }

  // Метод для прогрева всех моделей (можно вызывать при старте)
  public void warmupModels() {
    List<LLMModel> models = llmModelRepository.findAllAvailableOrderedByPriority();
    for (LLMModel model : models) {
      try {
        getChatClient(model);
        log.info("Warmed up model: {}", model.getName());
      } catch (Exception e) {
        log.warn("Failed to warm up model: {} - {}", model.getName(), e.getMessage());
      }
    }
  }

  /**
   * Диагностический метод для проверки состояния моделей
   */
  public void diagnoseModels() {
    try {
      log.info("=== LLM Models Diagnosis ===");

      List<LLMModel> allModels = llmModelRepository.findAll();
      log.info("Total models in database: {}", allModels.size());

      for (LLMModel model : allModels) {
        log.info("Model: {} | Enabled: {} | Default: {} | Available: {} | Priority: {}",
            model.getName(), model.getEnabled(), model.getIsDefault(),
            model.isAvailable(), model.getPriorityOrder());

        if (model.getAvailableAfter() != null) {
          log.info("  - Available after: {}", model.getAvailableAfter());
        }

        if (model.getFailureCount() != null && model.getFailureCount() > 0) {
          log.info("  - Failure count: {}", model.getFailureCount());
        }
      }

      List<LLMModel> availableModels = llmModelRepository.findAllAvailableOrderedByPriority();
      log.info("Available models: {}", availableModels.size());

      for (LLMModel model : availableModels) {
        log.info("Available: {} (priority: {})", model.getName(), model.getPriorityOrder());
      }

      if (currentModel != null) {
        log.info("Current model: {}", currentModel.getName());
      } else {
        log.warn("No current model set!");
      }

      log.info("=== End Diagnosis ===");

    } catch (Exception e) {
      log.error("Failed to run diagnosis", e);
    }
  }
}
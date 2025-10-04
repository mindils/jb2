package ru.mindils.jb2.app.service;

import io.jmix.core.UnconstrainedDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import ru.mindils.jb2.app.entity.LlmCallLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mindils.jb2.app.dto.LlmAnalysisResponse;
import ru.mindils.jb2.app.util.JsonExtractor;

import java.util.UUID;

@Service
public class SimpleLLMService {
  private static final Logger log = LoggerFactory.getLogger(SimpleLLMService.class);

  private final ObjectMapper objectMapper;
  private final UnconstrainedDataManager dataManager;
  private final ChatClient chatClient;
  private final String defaultModel;

  public SimpleLLMService(UnconstrainedDataManager dataManager,
                          ChatClient chatClient,
                          ObjectMapper objectMapper,
                          @Value("${spring.ai.openai.chat.options.model:gpt-4}") String defaultModel) {
    this.dataManager = dataManager;
    this.chatClient = chatClient;
    this.objectMapper = objectMapper;
    this.defaultModel = defaultModel;
  }

  /**
   * Главный метод для вызова LLM через litellm
   * При ошибке просто пробрасывает исключение - роутинг делает litellm
   */
  public LlmAnalysisResponse callLLM(String prompt, OpenAiChatOptions options) {
    String requestId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();

    // Определяем модель для лога
    String requestedModel = (options != null && options.getModel() != null) ?
        options.getModel() : defaultModel;

    log.info("Starting LLM call with requestId: {} for model: {}", requestId, requestedModel);

    // Создаем лог начала вызова
    LlmCallLog callLog = createCallLog(requestId, requestedModel, prompt, options);

    try {
      // Выполняем вызов и получаем полный ответ с метаданными
      ChatResponse chatResponse = executeCallWithMetadata(prompt, options);

      // Получаем текстовый контент из AssistantMessage
      String response = chatResponse.getResult().getOutput().getText();

      // Успешный вызов - логируем с метаданными
      long duration = System.currentTimeMillis() - startTime;
      logSuccessfulCall(callLog, response, duration, chatResponse.getMetadata());

      log.info("Successfully called LLM in {} ms, actual model: {}",
          duration, callLog.getActualModelUsed());

      // Парсим JSON и создаем DTO
      return parseResponseToDto(response, callLog.getId(), callLog.getActualModelUsed());

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;

      // Логируем ошибку
      logFailedCall(callLog, e, getHttpStatusCode(e), classifyError(e), duration);

      log.error("Failed to call LLM: {} - {}", e.getClass().getSimpleName(), e.getMessage());

      // Пробрасываем исключение дальше
      throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
    }
  }

  /**
   * Упрощенный метод без дополнительных опций
   */
  public LlmAnalysisResponse callLLM(String prompt) {
    return callLLM(prompt, null);
  }

  /**
   * Парсит ответ LLM в DTO с JSON и метаинформацией
   */
  private LlmAnalysisResponse parseResponseToDto(String rawResponse, Long llmCallId, String llmModel) {
    try {
      // Логируем исходный ответ для отладки
      if (JsonExtractor.containsMarkdownBlock(rawResponse)) {
        log.debug("Response contains markdown block, extracting JSON for call {}", llmCallId);
      }

      // Извлекаем чистый JSON из ответа
      String cleanedJson = JsonExtractor.extractJson(rawResponse);

      // Если извлеченный JSON отличается от исходного, логируем это
      if (!cleanedJson.equals(rawResponse.trim())) {
        log.debug("JSON extracted from markdown. Original length: {}, Cleaned length: {}",
            rawResponse.length(), cleanedJson.length());
      }

      // Парсим очищенный JSON
      JsonNode jsonNode = objectMapper.readTree(cleanedJson);
      log.debug("Successfully parsed JSON response for call {}", llmCallId);

      // Возвращаем успешный ответ с очищенным JSON
      return LlmAnalysisResponse.success(cleanedJson, jsonNode, llmCallId, llmModel);

    } catch (JsonProcessingException e) {
      String parseError = "Failed to parse JSON: " + e.getMessage();
      log.warn("Failed to parse JSON response for call {}: {}", llmCallId, e.getMessage());
      log.debug("Raw response that failed to parse: {}", rawResponse);

      // Возвращаем ошибку парсинга с исходным ответом
      return LlmAnalysisResponse.withParseError(rawResponse, llmCallId, llmModel, parseError);
    }
  }

  /**
   * Выполнение вызова к LLM через litellm с получением метаданных
   */
  private ChatResponse executeCallWithMetadata(String prompt, OpenAiChatOptions additionalOptions) {
    // Объединяем опции
    OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
        .model(additionalOptions != null && additionalOptions.getModel() != null ?
            additionalOptions.getModel() : defaultModel);

    if (additionalOptions != null) {
      if (additionalOptions.getTemperature() != null) {
        optionsBuilder.temperature(additionalOptions.getTemperature());
      }
      if (additionalOptions.getMaxTokens() != null) {
        optionsBuilder.maxTokens(additionalOptions.getMaxTokens());
      }
    }

    try {
      ChatResponse response = chatClient.prompt()
          .user(prompt)
          .options(optionsBuilder.build())
          .call()
          .chatResponse();

      // Проверяем наличие ответа
      if (response == null ||
          response.getResult() == null ||
          response.getResult().getOutput() == null) {
        throw new RuntimeException("Empty response from LLM");
      }

      // Получаем текстовый контент из AssistantMessage
      String content = response.getResult().getOutput().getText();

      if (content == null || content.isEmpty()) {
        throw new RuntimeException("Empty content in LLM response");
      }

      return response;

    } catch (HttpClientErrorException | HttpServerErrorException e) {
      throw new RuntimeException("HTTP " + e.getStatusCode().value() + ": " + e.getMessage(), e);
    } catch (ResourceAccessException e) {
      throw new RuntimeException("Network error: " + e.getMessage(), e);
    }
  }

  /**
   * Классификация ошибки для логирования
   */
  private String classifyError(Exception e) {
    String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

    if (message.contains("rate limit") || message.contains("429") ||
        message.contains("too many requests")) {
      return "RATE_LIMIT";
    }
    if (message.contains("401") || message.contains("unauthorized")) {
      return "AUTH_ERROR";
    }
    if (message.contains("quota") || message.contains("402") ||
        message.contains("insufficient credits") || message.contains("billing")) {
      return "QUOTA_EXCEEDED";
    }
    if (e instanceof ResourceAccessException || message.contains("timeout")) {
      return "TIMEOUT";
    }
    if (e instanceof HttpClientErrorException || e instanceof HttpServerErrorException) {
      return "API_ERROR";
    }

    return "UNKNOWN";
  }

  /**
   * Получение HTTP кода из исключения
   */
  private Integer getHttpStatusCode(Exception e) {
    if (e instanceof HttpClientErrorException) {
      return ((HttpClientErrorException) e).getStatusCode().value();
    }
    if (e instanceof HttpServerErrorException) {
      return ((HttpServerErrorException) e).getStatusCode().value();
    }
    if (e.getMessage() != null) {
      if (e.getMessage().contains("429")) return 429;
      if (e.getMessage().contains("401")) return 401;
      if (e.getMessage().contains("402")) return 402;
      if (e.getMessage().contains("500")) return 500;
      if (e.getMessage().contains("503")) return 503;
    }
    return null;
  }

  /**
   * Создание записи лога для вызова
   */
  private LlmCallLog createCallLog(String requestId, String modelName,
                                   String prompt, OpenAiChatOptions options) {
    Double temperature = null;
    Integer maxTokens = null;

    if (options != null) {
      temperature = options.getTemperature();
      maxTokens = options.getMaxTokens();
    }

    return LlmCallLog.startCall(requestId, modelName, prompt, temperature, maxTokens);
  }

  /**
   * Логирование успешного вызова с метаданными от litellm
   * ОБНОВЛЕНО: использует getCompletionTokens() вместо getGenerationTokens()
   */
  private void logSuccessfulCall(LlmCallLog callLog, String response,
                                 long duration, ChatResponseMetadata metadata) {
    callLog.completeWithSuccess(response, duration);

    // Извлекаем метаданные от litellm
    if (metadata != null) {
      // Реальная модель, которую использовал litellm
      String actualModel = metadata.getModel();
      if (actualModel != null && !actualModel.isEmpty()) {
        callLog.setActualModelUsed(actualModel);
      }

      // Информация об использовании токенов
      Usage usage = metadata.getUsage();
      if (usage != null) {
        // В Spring AI M6+ методы возвращают Integer, а не Long
        if (usage.getPromptTokens() != null) {
          callLog.setPromptTokens(usage.getPromptTokens().intValue());
        }
        // ОБНОВЛЕНО: используем getCompletionTokens() вместо getGenerationTokens()
        if (usage.getCompletionTokens() != null) {
          callLog.setCompletionTokens(usage.getCompletionTokens().intValue());
        }
        if (usage.getTotalTokens() != null) {
          callLog.setTotalTokens(usage.getTotalTokens().intValue());
        }
      }

      log.debug("LLM metadata - model: {}, prompt tokens: {}, completion tokens: {}, total: {}",
          actualModel,
          usage != null ? usage.getPromptTokens() : null,
          usage != null ? usage.getCompletionTokens() : null,
          usage != null ? usage.getTotalTokens() : null);
    }

    dataManager.save(callLog);
  }

  /**
   * Логирование неудачного вызова
   */
  private void logFailedCall(LlmCallLog callLog, Exception e,
                             Integer httpStatus, String errorType, long duration) {
    callLog.completeWithError(e, httpStatus, errorType, duration);
    dataManager.save(callLog);
  }
}
package ru.mindils.jb2.app.service;

import io.jmix.core.UnconstrainedDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import ru.mindils.jb2.app.entity.LLMModel;
import ru.mindils.jb2.app.entity.LlmCallLog;
import ru.mindils.jb2.app.repository.LLMModelRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SimpleLLMService {
  private static final Logger log = LoggerFactory.getLogger(SimpleLLMService.class);

  // Типы ошибок для классификации
  private static final String ERROR_RATE_LIMIT = "RATE_LIMIT";
  private static final String ERROR_QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
  private static final String AUTH_ERROR = "AUTH_ERROR";
  private static final String ERROR_TIMEOUT = "TIMEOUT";
  private static final String ERROR_API = "API_ERROR";
  private static final String ERROR_UNKNOWN = "UNKNOWN";

  // Временные интервалы недоступности для разных типов ошибок
  private static final int RATE_LIMIT_MINUTES = 5;
  private static final int QUOTA_EXCEEDED_HOURS = 24;
  private static final int API_ERROR_MINUTES = 1;

  private final LLMModelRepository modelRepository;
  private final UnconstrainedDataManager dataManager;
  private final ChatClientFactory chatClientFactory;

  public SimpleLLMService(LLMModelRepository modelRepository,
                          UnconstrainedDataManager dataManager,
                          ChatClientFactory chatClientFactory) {
    this.modelRepository = modelRepository;
    this.dataManager = dataManager;
    this.chatClientFactory = chatClientFactory;
  }

  /**
   * Главный метод для вызова LLM с автоматическим переключением между моделями
   */
  public String callLLM(String prompt, OpenAiChatOptions options) {
    String requestId = UUID.randomUUID().toString();
    log.info("Starting LLM call with requestId: {}", requestId);

    List<LLMModel> availableModels = modelRepository.findAllAvailableOrderedByPriority();

    if (availableModels.isEmpty()) {
      String error = "No available LLM models found";
      log.error(error);
      logFailedCall(requestId, null, prompt, options,
          new RuntimeException(error), null, ERROR_UNKNOWN, 0);
      throw new RuntimeException(error);
    }

    Exception lastException = null;
    int retryCount = 0;

    // Пробуем каждую доступную модель по приоритету
    for (LLMModel model : availableModels) {
      long startTime = System.currentTimeMillis();

      try {
        log.info("Trying model: {} ({})", model.getName(), model.getModelName());

        // Создаем лог начала вызова
        LlmCallLog callLog = createCallLog(requestId, model, prompt, options, retryCount);

        // Выполняем вызов
        String response = executeCall(model, prompt, options);

        // Успешный вызов - логируем и обновляем статистику
        long duration = System.currentTimeMillis() - startTime;
        logSuccessfulCall(callLog, response, duration);
        updateModelSuccess(model);

        log.info("Successfully called model {} in {} ms", model.getName(), duration);
        return response;

      } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        lastException = e;
        retryCount++;

        // Логируем ошибку
        logFailedCall(requestId, model, prompt, options, e,
            getHttpStatusCode(e), classifyError(e), duration);

        // Обрабатываем ошибку и обновляем модель
        handleModelError(model, e);

        log.warn("Failed to call model {}: {} - {}",
            model.getName(), e.getClass().getSimpleName(), e.getMessage());
      }
    }

    // Все модели не сработали
    String error = "All LLM models failed. Last error: " +
        (lastException != null ? lastException.getMessage() : "unknown");
    log.error(error);
    throw new RuntimeException(error, lastException);
  }

  /**
   * Упрощенный метод без дополнительных опций
   */
  public String callLLM(String prompt) {
    return callLLM(prompt, null);
  }

  /**
   * Выполнение вызова к конкретной модели
   */
  private String executeCall(LLMModel model, String prompt, OpenAiChatOptions additionalOptions) {
    ChatClient client = chatClientFactory.getOrCreateClient(model);

    // Объединяем опции модели и дополнительные
    OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
        .model(model.getModelName())
        .temperature(additionalOptions != null && additionalOptions.getTemperature() != null ?
            additionalOptions.getTemperature() : model.getTemperature())
        .maxTokens(additionalOptions != null && additionalOptions.getMaxTokens() != null ?
            additionalOptions.getMaxTokens() : model.getMaxTokens());

    try {
      String response = client.prompt()
          .user(prompt)
          .options(optionsBuilder.build())
          .call()
          .content();

      if (response == null || response.isEmpty()) {
        throw new RuntimeException("Empty response from LLM");
      }

      return response;

    } catch (HttpClientErrorException | HttpServerErrorException e) {
      // HTTP ошибки с кодом статуса
      if (e.getStatusCode() != HttpStatus.OK) {
        throw new RuntimeException("HTTP " + e.getStatusCode().value() + ": " + e.getMessage(), e);
      }
      throw e;
    } catch (ResourceAccessException e) {
      // Таймаут или проблемы с сетью
      throw new RuntimeException("Network error: " + e.getMessage(), e);
    }
  }

  /**
   * Классификация ошибки для определения типа
   */
  private String classifyError(Exception e) {
    String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

    if (message.contains("rate limit") || message.contains("429") ||
        message.contains("too many requests")) {
      return ERROR_RATE_LIMIT;
    }
    if (message.contains("401")) {
      return AUTH_ERROR;
    }

    if (message.contains("quota") || message.contains("402") ||
        message.contains("insufficient credits") || message.contains("billing")) {
      return ERROR_QUOTA_EXCEEDED;
    }

    if (e instanceof ResourceAccessException || message.contains("timeout")) {
      return ERROR_TIMEOUT;
    }

    if (e instanceof HttpClientErrorException || e instanceof HttpServerErrorException) {
      return ERROR_API;
    }

    return ERROR_UNKNOWN;
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
      // Пытаемся извлечь код из сообщения
      if (e.getMessage().contains("429")) return 429;
      if (e.getMessage().contains("402")) return 402;
      if (e.getMessage().contains("500")) return 500;
      if (e.getMessage().contains("503")) return 503;
    }
    return null;
  }

  /**
   * Обработка ошибки модели и обновление её статуса
   */
  private void handleModelError(LLMModel model, Exception e) {
    String errorType = classifyError(e);

    // Увеличиваем счетчик ошибок
    model.setFailureCount((model.getFailureCount() == null ? 0 : model.getFailureCount()) + 1);
    model.setLastFailure(OffsetDateTime.now());

    // Определяем, нужно ли делать модель временно недоступной
    OffsetDateTime availableAfter = null;

    switch (errorType) {
      case ERROR_RATE_LIMIT:
        // Rate limit - недоступна на 5 минут
        availableAfter = OffsetDateTime.now().plusMinutes(RATE_LIMIT_MINUTES);
        log.info("Model {} rate limited, will be available after {}",
            model.getName(), availableAfter);
        break;

      case AUTH_ERROR:
        // Квота исчерпана - недоступна на 24 часа
        availableAfter = OffsetDateTime.now().plusHours(QUOTA_EXCEEDED_HOURS);
        log.warn("Model {} quota exceeded, will be available after {}",
            model.getName(), availableAfter);
        break;

      case ERROR_QUOTA_EXCEEDED:
        // Квота исчерпана - недоступна на 24 часа
        availableAfter = OffsetDateTime.now().plusHours(QUOTA_EXCEEDED_HOURS);
        log.warn("Model {} quota exceeded, will be available after {}",
            model.getName(), availableAfter);
        break;

      case ERROR_API:
        // API ошибки - короткий таймаут только при повторных ошибках
        if (model.getFailureCount() > 3) {
          availableAfter = OffsetDateTime.now().plusMinutes(API_ERROR_MINUTES);
          log.info("Model {} has multiple API errors, cooling down until {}",
              model.getName(), availableAfter);
        }
        break;

      case ERROR_TIMEOUT:
        // Таймауты - не блокируем, просто логируем
        log.debug("Model {} timeout, will retry on next request", model.getName());
        break;

      default:
        // Неизвестные ошибки - блокируем после 5 подряд
        if (model.getFailureCount() > 5) {
          availableAfter = OffsetDateTime.now().plusMinutes(10);
          log.warn("Model {} has too many unknown errors, cooling down", model.getName());
        }
    }

    model.setAvailableAfter(availableAfter);
    modelRepository.save(model);
  }

  /**
   * Обновление статистики успешного вызова
   */
  private void updateModelSuccess(LLMModel model) {
    model.setFailureCount(0);
    model.setLastSuccess(OffsetDateTime.now());
    model.setAvailableAfter(null);
    modelRepository.save(model);
  }

  /**
   * Создание записи лога для вызова
   */
  private LlmCallLog createCallLog(String requestId, LLMModel model,
                                   String prompt, OpenAiChatOptions options,
                                   int retryCount) {

    LlmCallLog log = LlmCallLog.startCall(
        requestId,
        model,
        prompt,
        options != null ? options.getTemperature() : model.getTemperature(),
        options != null ? options.getMaxTokens() : model.getMaxTokens()
    );
    log.setRetryCount(retryCount);
    return log;
  }

  /**
   * Логирование успешного вызова
   */
  private void logSuccessfulCall(LlmCallLog callLog, String response, long duration) {
    callLog.completeWithSuccess(response, duration);
    dataManager.save(callLog);
  }

  /**
   * Логирование неудачного вызова
   */
  private void logFailedCall(String requestId, LLMModel model, String prompt,
                             OpenAiChatOptions options, Exception e,
                             Integer httpStatus, String errorType, long duration) {
    LlmCallLog log = dataManager.create(LlmCallLog.class);
    log.setRequestId(requestId);

    if (model != null) {
      log.setModelId(model.getId());
      log.setModelName(model.getModelName());
      log.setBaseUrl(model.getBaseUrl());
    }

    log.setPrompt(prompt);
    log.setPromptLength(prompt != null ? prompt.length() : 0);

    if (options != null) {
      log.setTemperature(options.getTemperature());
      log.setMaxTokens(options.getMaxTokens());
    }

    log.completeWithError(e, httpStatus, errorType, duration);
    dataManager.save(log);
  }

  /**
   * Метод для ручного сброса ошибок модели
   */
  public void resetModelFailures(Long modelId) {
    LLMModel model = modelRepository.findAll().stream()
        .filter(m -> m.getId().equals(modelId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

    model.setFailureCount(0);
    model.setLastFailure(null);
    model.setAvailableAfter(null);
    modelRepository.save(model);

    chatClientFactory.clearCache(modelId);
    log.info("Reset failures for model: {}", model.getName());
  }
}
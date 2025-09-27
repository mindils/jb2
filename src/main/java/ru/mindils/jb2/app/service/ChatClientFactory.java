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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import ru.mindils.jb2.app.entity.LLMModel;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatClientFactory {
  private static final Logger log = LoggerFactory.getLogger(ChatClientFactory.class);

  // Кэш клиентов для повторного использования
  private final Map<Long, ChatClient> clientCache = new ConcurrentHashMap<>();

  /**
   * Получить или создать клиент для модели
   */
  public ChatClient getOrCreateClient(LLMModel model) {
    return clientCache.computeIfAbsent(model.getId(), id -> {
      log.debug("Creating new ChatClient for model: {} ({})",
          model.getName(), model.getModelName());
      return createClient(model);
    });
  }

  /**
   * Создание нового клиента
   */
  private ChatClient createClient(LLMModel model) {
    try {
      // Настраиваем HTTP клиент с таймаутом
      HttpClient httpClient = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .connectTimeout(Duration.ofSeconds(
              model.getTimeoutSeconds() != null ? model.getTimeoutSeconds() : 30))
          .build();

      // Создаем API клиент
      OpenAiApi api = OpenAiApi.builder()
          .baseUrl(model.getBaseUrl())
          .apiKey(model.getApiKey() != null ?
              new SimpleApiKey(model.getApiKey()) :
              new NoopApiKey())
          .webClientBuilder(WebClient.builder()
              .clientConnector(new JdkClientHttpConnector(httpClient)))
          .restClientBuilder(RestClient.builder()
              .requestFactory(new JdkClientHttpRequestFactory(httpClient)))
          .build();

      // Создаем модель чата с дефолтными настройками
      OpenAiChatModel chatModel = new OpenAiChatModel(
          api,
          OpenAiChatOptions.builder()
              .model(model.getModelName())
              .temperature(model.getTemperature() != null ? model.getTemperature() : 0.1)
              .maxTokens(model.getMaxTokens() != null ? model.getMaxTokens() : 1000)
              .build()
      );

      log.info("Successfully created ChatClient for model: {}", model.getName());
      return ChatClient.builder(chatModel).build();

    } catch (Exception e) {
      log.error("Failed to create ChatClient for model: {}", model.getName(), e);
      throw new RuntimeException("Failed to create client for model: " + model.getName(), e);
    }
  }

  /**
   * Очистить кэш для конкретной модели
   */
  public void clearCache(Long modelId) {
    ChatClient removed = clientCache.remove(modelId);
    if (removed != null) {
      log.debug("Cleared cached client for model ID: {}", modelId);
    }
  }

  /**
   * Очистить весь кэш
   */
  public void clearAllCache() {
    int size = clientCache.size();
    clientCache.clear();
    log.info("Cleared all {} cached clients", size);
  }

  /**
   * Проверка здоровья клиента (можно использовать для прогрева)
   */
  public boolean testClient(LLMModel model) {
    try {
      ChatClient client = getOrCreateClient(model);
      String response = client.prompt()
          .user("Say 'OK' if you are working")
          .options(OpenAiChatOptions.builder()
              .model(model.getModelName())
              .maxTokens(10)
              .temperature(0.0)
              .build())
          .call()
          .content();

      boolean success = response != null && !response.isEmpty();
      log.info("Health check for model {} - {}",
          model.getName(), success ? "SUCCESS" : "FAILED");
      return success;

    } catch (Exception e) {
      log.warn("Health check failed for model {}: {}",
          model.getName(), e.getMessage());
      return false;
    }
  }
}
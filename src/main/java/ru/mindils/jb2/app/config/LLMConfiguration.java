package ru.mindils.jb2.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class LLMConfiguration {

  @Value("${spring.ai.openai.base-url:http://localhost:4000}")
  private String baseUrl;

  @Value("${spring.ai.openai.api-key:sk-1234}")
  private String apiKey;

  @Value("${spring.ai.openai.chat.options.timeout:60s}")
  private Duration timeout;

  @Bean
  public OpenAiApi openAiApi() {
    return OpenAiApi.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)
        .webClientBuilder(WebClient.builder()
            .clientConnector(new JdkClientHttpConnector(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build())))
        .restClientBuilder(RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build())))
        .build();
  }

  @Bean
  public OpenAiChatModel openAiChatModel(OpenAiApi api,
                                         @Value("${spring.ai.openai.chat.options.model:gpt-4}") String model,
                                         @Value("${spring.ai.openai.chat.options.temperature:0.7}") Double temperature,
                                         @Value("${spring.ai.openai.chat.options.max-tokens:1000}") Integer maxTokens) {
    return new OpenAiChatModel(
        api,
        OpenAiChatOptions.builder()
            .model(model)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .build()
    );
  }

  @Bean
  public ChatClient chatClient(OpenAiChatModel model) {
    return ChatClient.builder(model).build();
  }
}
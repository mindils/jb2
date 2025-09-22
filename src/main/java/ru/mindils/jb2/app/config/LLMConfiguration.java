package ru.mindils.jb2.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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

  @Bean
  @Deprecated
  public OpenAiApi openAiApi() {
    return OpenAiApi.builder()
        .baseUrl("http://localhost:1234")
        .apiKey(new NoopApiKey())
        .webClientBuilder(WebClient.builder()
            .clientConnector(new JdkClientHttpConnector(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build())))
        .restClientBuilder(RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build())))
        .build();
  }

  @Bean
  @Deprecated
  public OpenAiChatModel openAiChatModel(OpenAiApi api) {
    return new OpenAiChatModel(
        api,
        OpenAiChatOptions.builder()
            .model("qwen3-30b-a3b-instruct-2507-mlx")
            .temperature(0.1)
            .maxTokens(1000)
            .build()
    );
  }

  @Bean
  @Deprecated
  public ChatClient chatClient(OpenAiChatModel model) {
    return ChatClient.builder(model).build();
  }
}
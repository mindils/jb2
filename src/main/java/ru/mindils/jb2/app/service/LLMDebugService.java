package ru.mindils.jb2.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LLMDebugService {

  private static final Logger log = LoggerFactory.getLogger(LLMDebugService.class);
  private final ChatClient chatClient;

  public LLMDebugService(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public String testSimpleCall() {
    try {
      log.info("Starting simple LLM test call...");

      String response = chatClient
          .prompt()
          .user("ping")
          .call()
          .content();

      log.info("LLM response received: {}", response);
      return response;

    } catch (Exception e) {
      log.error("Error during LLM call: {}", e.getMessage(), e);
      throw new RuntimeException("LLM test failed", e);
    }
  }
}
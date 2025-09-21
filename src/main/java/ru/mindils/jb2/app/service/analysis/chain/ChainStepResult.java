package ru.mindils.jb2.app.service.analysis.chain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Результат выполнения шага анализа
 */
public record ChainStepResult(
    boolean shouldContinue,    // продолжать ли обработку
    String stopReason,         // причина остановки (если shouldContinue = false)
    JsonNode stepData,         // данные полученные на этом шаге
    String llmResponse         // сырой ответ от LLM (для логирования)
) {

  public static ChainStepResult success(JsonNode stepData, String llmResponse) {
    return new ChainStepResult(true, null, stepData, llmResponse);
  }

  public static ChainStepResult stop(String reason, JsonNode stepData, String llmResponse) {
    return new ChainStepResult(false, reason, stepData, llmResponse);
  }
}
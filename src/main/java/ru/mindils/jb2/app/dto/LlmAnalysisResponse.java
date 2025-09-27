package ru.mindils.jb2.app.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmAnalysisResponse(
    String rawResponse,
    JsonNode jsonNode,
    Long llmCallId,
    String llmModel,
    String jsonParseError
) {
  public static LlmAnalysisResponse success(String rawResponse, JsonNode jsonNode, Long llmCallId, String llmModel) {
    return new LlmAnalysisResponse(rawResponse, jsonNode, llmCallId, llmModel, null);
  }

  public static LlmAnalysisResponse withParseError(String rawResponse, Long llmCallId, String llmModel, String parseError) {
    return new LlmAnalysisResponse(rawResponse, null, llmCallId, llmModel, parseError);
  }

  public boolean hasValidJson() {
    return jsonNode != null && jsonParseError == null;
  }

  public boolean hasParseError() {
    return jsonParseError != null;
  }
}

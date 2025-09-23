package ru.mindils.jb2.app.service.analysis.chain;

import ru.mindils.jb2.app.service.analysis.VacancyScorer;

import java.util.HashMap;
import java.util.Map;

/**
 * Упрощенный результат выполнения цепочки анализа
 */
public record ChainAnalysisResult(
    String vacancyId,
    AnalysisChainConfig chainConfig,
    boolean success,
    String errorMessage,
    String stoppedAt,           // на каком шаге остановились
    String stopReason,          // причина остановки
    Map<String, ChainStepResult> stepResults,  // результаты каждого шага
    VacancyScorer.VacancyScore finalScore      // итоговый скор (только totalScore и rating)
) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String vacancyId;
    private AnalysisChainConfig chainConfig;
    private boolean success = true;
    private String errorMessage;
    private String stoppedAt;
    private String stopReason;
    private Map<String, ChainStepResult> stepResults = new HashMap<>();
    private VacancyScorer.VacancyScore finalScore;

    public Builder vacancyId(String vacancyId) {
      this.vacancyId = vacancyId;
      return this;
    }

    public Builder chainConfig(AnalysisChainConfig chainConfig) {
      this.chainConfig = chainConfig;
      return this;
    }

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder stoppedAt(String stoppedAt) {
      this.stoppedAt = stoppedAt;
      return this;
    }

    public Builder stopReason(String stopReason) {
      this.stopReason = stopReason;
      return this;
    }

    public Builder addStepResult(String stepId, ChainStepResult result) {
      this.stepResults.put(stepId, result);
      return this;
    }

    public Builder finalScore(VacancyScorer.VacancyScore finalScore) {
      this.finalScore = finalScore;
      return this;
    }

    public ChainAnalysisResult build() {
      return new ChainAnalysisResult(
          vacancyId, chainConfig, success, errorMessage,
          stoppedAt, stopReason, stepResults, finalScore
      );
    }
  }
}
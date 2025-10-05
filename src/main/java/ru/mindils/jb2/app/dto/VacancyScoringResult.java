package ru.mindils.jb2.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат оценки вакансии
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class VacancyScoringResult {

  /**
   * Итоговая оценка вакансии
   */
  private int totalScore;

  /**
   * Позитивные факторы (что подходит)
   */
  @Builder.Default
  private List<String> positiveFactors = new ArrayList<>();

  /**
   * Негативные факторы (что не подходит)
   */
  @Builder.Default
  private List<String> negativeFactors = new ArrayList<>();

  public VacancyScoringResult() {
    this.positiveFactors = new ArrayList<>();
    this.negativeFactors = new ArrayList<>();
  }

  public void addPositive(String factor) {
    positiveFactors.add(factor);
  }

  public void addNegative(String factor) {
    negativeFactors.add(factor);
  }

  public void addScore(int points) {
    this.totalScore += points;
  }
}
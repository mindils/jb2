package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyRating;

/**
 * Упрощенный калькулятор скора вакансии
 */
@Component
public class VacancyScorer {

  private static final Logger log = LoggerFactory.getLogger(VacancyScorer.class);
  private static final int MAX_TOTAL_SCORE = 225;

  /**
   * Рассчитать скор вакансии на основе JsonNode с результатами шагов
   */
  public VacancyScore calculateScore(VacancyAnalysis analysis) {
    if (analysis == null || analysis.getStepResults() == null) {
      log.warn("No analysis data for scoring");
      return new VacancyScore(0, VacancyRating.VERY_POOR);
    }

    JsonNode stepResults = analysis.getStepResults();
    log.info("Calculating score for vacancy: {}", analysis.getId());

    int totalScore = 0;

    // 1. Первичный анализ (максимум 100)
    totalScore += calculatePrimaryScore(stepResults.get("primary"));

    // 2. Социальный анализ (максимум 50)
    totalScore += calculateSocialScore(stepResults.get("social"));

    // 3. Технический анализ (максимум 50)
    totalScore += calculateTechnicalScore(stepResults.get("technical"));

    // 4. Зарплата (максимум 25)
    totalScore += calculateSalaryScore(stepResults.get("salary"));

    VacancyRating rating = determineRating(totalScore);

    log.info("Calculated score for vacancy {}: {} points ({})",
        analysis.getId(), totalScore, rating);

    return new VacancyScore(totalScore, rating);
  }

  /**
   * Первичный анализ: Java + Jmix + AI (максимум 100)
   */
  private int calculatePrimaryScore(JsonNode primary) {
    if (primary == null) return 0;

    int score = 0;
    if (getBooleanValue(primary, "java")) score += 50;
    if (getBooleanValue(primary, "jmix")) score += 30;
    if (getBooleanValue(primary, "ai")) score += 20;

    return score;
  }

  /**
   * Социальный анализ: формат работы + значимость (максимум 50)
   */
  private int calculateSocialScore(JsonNode social) {
    if (social == null) return 0;

    int score = 0;

    // Формат работы (30 баллов)
    String workMode = getStringValue(social, "work_mode");
    score += switch (workMode) {
      case "remote" -> 30;
      case "flexible" -> 28;
      case "hybrid_flexible" -> 25;
      case "hybrid" -> 20;
      case "hybrid_2_3" -> 18;
      case "hybrid_3_2" -> 15;
      case "hybrid_4_1" -> 10;
      case "office" -> 5;
      default -> 0;
    };

    // Социальная значимость (20 баллов)
    if (getBooleanValue(social, "socially_significant")) {
      score += 20;
    }

    return score;
  }

  /**
   * Технический анализ: роль + уровень + стек (максимум 50)
   */
  private int calculateTechnicalScore(JsonNode technical) {
    if (technical == null) return 0;

    int score = 0;

    // Тип роли (20 баллов)
    String roleType = getStringValue(technical, "role_type");
    score += switch (roleType) {
      case "backend" -> 20;
      case "frontend_plus_backend" -> 18;
      case "devops_with_dev" -> 15;
      default -> 0;
    };

    // Уровень позиции (15 баллов)
    String positionLevel = getStringValue(technical, "position_level");
    score += switch (positionLevel) {
      case "architect" -> 15;
      case "principal" -> 14;
      case "senior" -> 13;
      case "lead" -> 12;
      case "middle" -> 8;
      case "junior" -> 5;
      default -> 0;
    };

    // Технологический стек (15 баллов)
    String stack = getStringValue(technical, "stack");
    score += calculateStackScore(stack);

    return score;
  }

  /**
   * Анализ зарплаты (максимум 25)
   */
  private int calculateSalaryScore(JsonNode salary) {
    if (salary == null) return 0;

    int score = 0;
    if (getBooleanValue(salary, "has_salary")) {
      score += 5; // бонус за указание зарплаты
      int salaryFrom = getIntValue(salary, "salary_from");
      score += calculateSalaryLevelScore(salaryFrom);
    }

    return score;
  }

  private int calculateStackScore(String stack) {
    if (stack == null || stack.trim().isEmpty()) return 0;

    int score = 0;
    String[] technologies = stack.toLowerCase().split("\\|");

    for (String tech : technologies) {
      tech = tech.trim();
      score += switch (tech) {
        case "spring" -> 5;
        case "microservices" -> 4;
        case "database" -> 3;
        case "python" -> 2;
        case "devops" -> 1;
        default -> 0;
      };
    }

    return Math.min(score, 15);
  }

  private int calculateSalaryLevelScore(int salaryFrom) {
    if (salaryFrom >= 400000) return 20;
    if (salaryFrom >= 300000) return 16;
    if (salaryFrom >= 250000) return 13;
    if (salaryFrom >= 200000) return 10;
    if (salaryFrom >= 150000) return 7;
    if (salaryFrom >= 100000) return 4;
    return 0;
  }

  private VacancyRating determineRating(int totalScore) {
    double percentage = (double) totalScore / MAX_TOTAL_SCORE * 100;

    if (percentage >= 80) return VacancyRating.EXCELLENT;
    if (percentage >= 60) return VacancyRating.GOOD;
    if (percentage >= 40) return VacancyRating.MODERATE;
    if (percentage >= 20) return VacancyRating.POOR;
    return VacancyRating.VERY_POOR;
  }

  // Utility методы для безопасного извлечения данных
  private boolean getBooleanValue(JsonNode node, String fieldName) {
    if (node == null) return false;
    JsonNode field = node.get(fieldName);
    return field != null && field.asBoolean(false);
  }

  private String getStringValue(JsonNode node, String fieldName) {
    if (node == null) return "";
    JsonNode field = node.get(fieldName);
    return field != null ? field.asText("") : "";
  }

  private int getIntValue(JsonNode node, String fieldName) {
    if (node == null) return 0;
    JsonNode field = node.get(fieldName);
    return field != null ? field.asInt(0) : 0;
  }

  /**
   * Упрощенный результат скоринга - только скор и рейтинг
   */
  public static record VacancyScore(
      int totalScore,
      VacancyRating rating
  ) {
    @com.fasterxml.jackson.annotation.JsonIgnore
    public double getScorePercentage() {
      return (double) totalScore / MAX_TOTAL_SCORE * 100;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDescription() {
      return String.format("%s (%d баллов, %.1f%%)",
          getRatingDescription(), totalScore, getScorePercentage());
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String getRatingDescription() {
      return switch (rating) {
        case EXCELLENT -> "Отличная вакансия";
        case GOOD -> "Хорошая вакансия";
        case MODERATE -> "Средняя вакансия";
        case POOR -> "Слабая вакансия";
        case VERY_POOR -> "Очень слабая вакансия";
      };
    }
  }

  public static int getMaxPossibleScore() {
    return MAX_TOTAL_SCORE;
  }
}
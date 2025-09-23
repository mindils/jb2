package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyRating;

import java.util.Optional;

/**
 * Улучшенный калькулятор скора вакансии
 * Простая и понятная логика начисления баллов
 */
@Component
public class VacancyScorer {

  private static final Logger log = LoggerFactory.getLogger(VacancyScorer.class);

  // Максимальные баллы по категориям
  private static final int MAX_PRIMARY_SCORE = 100;      // Java(50) + Jmix(30) + AI(20)
  private static final int MAX_WORK_MODE_SCORE = 30;     // Формат работы
  private static final int MAX_SOCIAL_SCORE = 20;        // Социальная значимость
  private static final int MAX_TECHNICAL_SCORE = 50;     // Роль(20) + Уровень(15) + Стек(15)
  private static final int MAX_SALARY_SCORE = 25;        // Зарплата
  private static final int MAX_TOTAL_SCORE = 225;        // Общий максимум

  /**
   * Рассчитать скор вакансии
   */
  public VacancyScore calculateScore(VacancyAnalysis analysis) {
    if (analysis == null) {
      log.warn("VacancyAnalysis is null");
      return createEmptyScore();
    }

    if (analysis.getStepResults() == null) {
      log.warn("Step results are null for vacancy: {}", analysis.getId());
      return createEmptyScore();
    }

    log.info("Calculating score for vacancy: {}", analysis.getId());

    ScoreDetails details = new ScoreDetails();
    int totalScore = 0;

    // 1. Первичный анализ (обязательный)
    totalScore += calculatePrimaryScore(analysis, details);

    // 2. Социальный анализ (формат работы + значимость)
    totalScore += calculateWorkModeScore(analysis, details);
    totalScore += calculateSocialSignificanceScore(analysis, details);

    // 3. Технический анализ
    totalScore += calculateTechnicalScore(analysis, details);

    // 4. Зарплата (дополнительные баллы)
    totalScore += calculateSalaryScore(analysis, details);

    // Определяем рейтинг
    VacancyRating rating = determineRating(totalScore);

    details.totalScore = totalScore;
    details.rating = rating;

    log.info("Calculated score for vacancy {}: {} points ({})",
        analysis.getId(), totalScore, rating);

    return new VacancyScore(totalScore, rating, details);
  }

  /**
   * Первичный анализ: Java + Jmix + AI (максимум 100 баллов)
   */
  private int calculatePrimaryScore(VacancyAnalysis analysis, ScoreDetails details) {
    JsonNode primary = getStepResult(analysis, "primary");
    if (primary == null) {
      log.debug("No primary analysis results for vacancy: {}", analysis.getId());
      return 0;
    }

    int score = 0;

    // Java - основной критерий (50 баллов)
    if (getBooleanValue(primary, "java")) {
      details.javaScore = 50;
      score += 50;
      log.debug("Java position detected: +50 points");
    }

    // Jmix - специализация (30 баллов)
    if (getBooleanValue(primary, "jmix")) {
      details.jmixScore = 30;
      score += 30;
      log.debug("Jmix position detected: +30 points");
    }

    // AI - современные технологии (20 баллов)
    if (getBooleanValue(primary, "ai")) {
      details.aiScore = 20;
      score += 20;
      log.debug("AI position detected: +20 points");
    }

    details.primaryTotal = score;
    return score;
  }

  /**
   * Формат работы (максимум 30 баллов)
   */
  private int calculateWorkModeScore(VacancyAnalysis analysis, ScoreDetails details) {
    JsonNode social = getStepResult(analysis, "social");
    if (social == null) return 0;

    String workMode = getStringValue(social, "work_mode");
    int score = switch (workMode) {
      case "remote" -> 30;              // Полностью удаленно
      case "flexible" -> 28;            // Гибкий график
      case "hybrid_flexible" -> 25;     // Гибкий гибрид
      case "hybrid" -> 20;              // Обычный гибрид
      case "hybrid_2_3" -> 18;          // 2 офис / 3 дома
      case "hybrid_3_2" -> 15;          // 3 офис / 2 дома
      case "hybrid_4_1" -> 10;          // 4 офис / 1 дома
      case "office" -> 5;               // Только офис
      default -> 0;                     // Неизвестно
    };

    details.workModeScore = score;
    if (score > 0) {
      log.debug("Work mode '{}': +{} points", workMode, score);
    }

    return score;
  }

  /**
   * Социальная значимость (максимум 20 баллов)
   */
  private int calculateSocialSignificanceScore(VacancyAnalysis analysis, ScoreDetails details) {
    JsonNode social = getStepResult(analysis, "social");
    if (social == null) return 0;

    if (getBooleanValue(social, "socially_significant")) {
      details.socialSignificanceScore = 20;
      log.debug("Socially significant project: +20 points");
      return 20;
    }

    return 0;
  }

  /**
   * Технический анализ: роль + уровень + стек (максимум 50 баллов)
   */
  private int calculateTechnicalScore(VacancyAnalysis analysis, ScoreDetails details) {
    JsonNode technical = getStepResult(analysis, "technical");
    if (technical == null) return 0;

    int score = 0;

    // Тип роли (20 баллов)
    String roleType = getStringValue(technical, "role_type");
    int roleScore = switch (roleType) {
      case "backend" -> 20;
      case "frontend_plus_backend" -> 18;
      case "devops_with_dev" -> 15;
      default -> 0;
    };
    details.roleTypeScore = roleScore;
    score += roleScore;

    // Уровень позиции (15 баллов)
    String positionLevel = getStringValue(technical, "position_level");
    int levelScore = switch (positionLevel) {
      case "architect" -> 15;
      case "principal" -> 14;
      case "senior" -> 13;
      case "lead" -> 12;
      case "middle" -> 8;
      case "junior" -> 5;
      default -> 0;
    };
    details.positionLevelScore = levelScore;
    score += levelScore;

    // Технологический стек (15 баллов)
    String stack = getStringValue(technical, "stack");
    int stackScore = calculateStackScore(stack);
    details.stackScore = stackScore;
    score += stackScore;

    details.technicalTotal = score;
    if (score > 0) {
      log.debug("Technical analysis: role={}, level={}, stack={} -> {} points",
          roleType, positionLevel, stack, score);
    }

    return score;
  }

  /**
   * Расчет баллов за технологический стек
   */
  private int calculateStackScore(String stack) {
    if (stack == null || stack.trim().isEmpty()) return 0;

    int score = 0;
    String[] technologies = stack.toLowerCase().split("\\|");

    for (String tech : technologies) {
      tech = tech.trim();
      score += switch (tech) {
        case "spring" -> 5;      // Spring экосистема
        case "microservices" -> 4; // Микросервисы
        case "database" -> 3;    // Базы данных
        case "python" -> 2;      // Дополнительный язык
        case "devops" -> 1;      // DevOps инструменты
        default -> 0;
      };
    }

    return Math.min(score, 15); // Максимум 15 баллов за стек
  }

  /**
   * Анализ зарплаты (максимум 25 баллов)
   */
  private int calculateSalaryScore(VacancyAnalysis analysis, ScoreDetails details) {
    JsonNode salary = getStepResult(analysis, "salary");
    if (salary == null) return 0;

    int score = 0;

    // Бонус за указание зарплаты (5 баллов)
    if (getBooleanValue(salary, "has_salary")) {
      score += 5;

      // Дополнительные баллы за уровень зарплаты (20 баллов)
      int salaryFrom = getIntValue(salary, "salary_from");
      score += calculateSalaryLevelScore(salaryFrom);
    }

    details.salaryScore = score;
    if (score > 0) {
      log.debug("Salary analysis: +{} points", score);
    }

    return score;
  }

  /**
   * Расчет баллов за уровень зарплаты (в рублях)
   */
  private int calculateSalaryLevelScore(int salaryFrom) {
    if (salaryFrom >= 400000) return 20;       // 400k+ - отличная
    if (salaryFrom >= 300000) return 16;       // 300-400k - очень хорошая
    if (salaryFrom >= 250000) return 13;       // 250-300k - хорошая
    if (salaryFrom >= 200000) return 10;       // 200-250k - нормальная
    if (salaryFrom >= 150000) return 7;        // 150-200k - средняя
    if (salaryFrom >= 100000) return 4;        // 100-150k - низкая
    return 0;                                  // <100k - очень низкая
  }

  /**
   * Определение рейтинга по общему скору
   */
  private VacancyRating determineRating(int totalScore) {
    double percentage = (double) totalScore / MAX_TOTAL_SCORE * 100;

    if (percentage >= 80) return VacancyRating.EXCELLENT;    // 80%+
    if (percentage >= 60) return VacancyRating.GOOD;         // 60-80%
    if (percentage >= 40) return VacancyRating.MODERATE;     // 40-60%
    if (percentage >= 20) return VacancyRating.POOR;         // 20-40%
    return VacancyRating.VERY_POOR;                          // <20%
  }

  /**
   * Безопасное получение результата шага
   */
  private JsonNode getStepResult(VacancyAnalysis analysis, String stepId) {
    try {
      JsonNode stepResults = analysis.getStepResults();
      if (stepResults != null && stepResults.isObject()) {
        return stepResults.get(stepId);
      }
    } catch (Exception e) {
      log.warn("Error accessing step result '{}' for vacancy {}: {}",
          stepId, analysis.getId(), e.getMessage());
    }
    return null;
  }

  /**
   * Безопасное получение boolean значения
   */
  private boolean getBooleanValue(JsonNode node, String fieldName) {
    if (node == null) return false;
    JsonNode field = node.get(fieldName);
    return field != null && field.asBoolean(false);
  }

  /**
   * Безопасное получение string значения
   */
  private String getStringValue(JsonNode node, String fieldName) {
    if (node == null) return "";
    JsonNode field = node.get(fieldName);
    return field != null ? field.asText("") : "";
  }

  /**
   * Безопасное получение int значения
   */
  private int getIntValue(JsonNode node, String fieldName) {
    if (node == null) return 0;
    JsonNode field = node.get(fieldName);
    return field != null ? field.asInt(0) : 0;
  }

  /**
   * Создание пустого скора при ошибках
   */
  private VacancyScore createEmptyScore() {
    return new VacancyScore(0, VacancyRating.VERY_POOR, new ScoreDetails());
  }

  /**
   * Результат скоринга
   */
  public static record VacancyScore(
      int totalScore,
      VacancyRating rating,
      ScoreDetails details
  ) {
    public double getScorePercentage() {
      return (double) totalScore / MAX_TOTAL_SCORE * 100;
    }

    public String getDescription() {
      return String.format("%s (%d баллов, %.1f%%)",
          getRatingDescription(), totalScore, getScorePercentage());
    }

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

  /**
   * Детализация скора
   */
  public static class ScoreDetails {
    // Первичный анализ
    public int javaScore = 0;
    public int jmixScore = 0;
    public int aiScore = 0;
    public int primaryTotal = 0;

    // Социальный анализ
    public int workModeScore = 0;
    public int socialSignificanceScore = 0;

    // Технический анализ
    public int roleTypeScore = 0;
    public int positionLevelScore = 0;
    public int stackScore = 0;
    public int technicalTotal = 0;

    // Зарплата
    public int salaryScore = 0;

    // Итого
    public int totalScore = 0;
    public VacancyRating rating = VacancyRating.VERY_POOR;

    public String getBreakdownText() {
      StringBuilder sb = new StringBuilder();
      sb.append("Первичный: ").append(primaryTotal);
      if (workModeScore > 0 || socialSignificanceScore > 0) {
        sb.append(", Социальный: ").append(workModeScore + socialSignificanceScore);
      }
      if (technicalTotal > 0) {
        sb.append(", Технический: ").append(technicalTotal);
      }
      if (salaryScore > 0) {
        sb.append(", Зарплата: ").append(salaryScore);
      }
      return sb.toString();
    }

    public boolean isJavaPosition() {
      return javaScore > 0;
    }

    public String getMainScoreReason() {
      if (javaScore == 0) return "Не Java позиция";
      if (workModeScore <= 5) return "Только офисная работа";
      if (primaryTotal >= 80) return "Отличное техническое соответствие";
      if (salaryScore >= 15) return "Высокая зарплата";
      if (workModeScore >= 25) return "Хорошие условия работы";
      return "Среднее соответствие критериям";
    }
  }

  /**
   * Получить максимально возможный скор
   */
  public static int getMaxPossibleScore() {
    return MAX_TOTAL_SCORE;
  }
}
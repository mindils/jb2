package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

import java.util.Set;

/**
 * Калькулятор итогового скора вакансии на основе stepResults
 */
@Component
public class VacancyScorer {

  private static final Logger log = LoggerFactory.getLogger(VacancyScorer.class);

  /**
   * Рассчитать скор на основе новой структуры stepResults
   */
  public VacancyScore calculateScore(VacancyAnalysis analysis) {
    if (analysis.getStepResults() == null) {
      log.warn("No step results found for vacancy: {}", analysis.getId());
      return new VacancyScore(0, Rating.VERY_POOR, new ScoreBreakdown());
    }

    ScoreBreakdown breakdown = new ScoreBreakdown();
    int totalScore = 0;

    // Первичный анализ (обязательный)
    totalScore += calculatePrimaryScore(analysis, breakdown);

    // Социальный анализ (если выполнялся)
    totalScore += calculateSocialScore(analysis, breakdown);

    // Технический анализ (если выполнялся)
    totalScore += calculateTechnicalScore(analysis, breakdown);

    // Зарплатный анализ (дополнительный)
    totalScore += calculateSalaryScore(analysis, breakdown);

    // Исследование компании (дополнительный)
    totalScore += calculateCompanyScore(analysis, breakdown);

    // Бонусы за полноту анализа
    totalScore += calculateCompletenessBonus(analysis, breakdown);

    // Определяем рейтинг
    Rating rating = determineRating(totalScore);

    log.debug("Calculated score for vacancy {}: {} ({})",
        analysis.getId(), totalScore, rating);

    return new VacancyScore(totalScore, rating, breakdown);
  }

  /**
   * Первичный анализ: Java, Jmix, AI (максимум 80 баллов)
   */
  private int calculatePrimaryScore(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    JsonNode primaryResult = analysis.getStepResult("primary");
    if (primaryResult == null) {
      return 0;
    }

    int score = 0;

    // Java - главный критерий (40 баллов)
    if (primaryResult.path("java").asBoolean(false)) {
      breakdown.javaScore = 40;
      score += 40;
    }

    // Jmix - специализация (25 баллов)
    if (primaryResult.path("jmix").asBoolean(false)) {
      breakdown.jmixScore = 25;
      score += 25;
    }

    // AI - тренд (15 баллов)
    if (primaryResult.path("ai").asBoolean(false)) {
      breakdown.aiScore = 15;
      score += 15;
    }

    breakdown.primaryTotal = score;
    return score;
  }

  /**
   * Социальный анализ: формат работы, домены (максимум 35 баллов)
   */
  private int calculateSocialScore(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    JsonNode socialResult = analysis.getStepResult("social");
    if (socialResult == null) {
      return 0;
    }

    int score = 0;

    // Формат работы (25 баллов)
    String workMode = socialResult.path("work_mode").asText("");
    breakdown.workModeScore = calculateWorkModeScore(workMode);
    score += breakdown.workModeScore;

    // Социальная значимость (10 баллов)
    if (socialResult.path("socially_significant").asBoolean(false)) {
      breakdown.socialSignificanceScore = 10;
      score += 10;
    }

    breakdown.socialTotal = score;
    return score;
  }

  /**
   * Расчет баллов за формат работы
   */
  private int calculateWorkModeScore(String workMode) {
    return switch (workMode) {
      case "remote" -> 25;                    // Полностью удаленно
      case "flexible" -> 23;                  // Гибкий график
      case "hybrid_flexible" -> 20;           // Гибкий гибрид
      case "hybrid" -> 18;                    // Обычный гибрид
      case "hybrid_2_3" -> 16;                // 2 дня офис / 3 дома
      case "hybrid_3_2" -> 14;                // 3 дня офис / 2 дома
      case "hybrid_4_1" -> 10;                // 4 дня офис / 1 дома
      case "office" -> 3;                     // Только офис
      default -> 0;                           // Неизвестно
    };
  }

  /**
   * Технический анализ: роль, уровень, стек (максимум 45 баллов)
   */
  private int calculateTechnicalScore(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    JsonNode technicalResult = analysis.getStepResult("technical");
    if (technicalResult == null) {
      return 0;
    }

    int score = 0;

    // Тип роли (15 баллов)
    String roleType = technicalResult.path("role_type").asText("");
    breakdown.roleTypeScore = calculateRoleTypeScore(roleType);
    score += breakdown.roleTypeScore;

    // Уровень позиции (15 баллов)
    String positionLevel = technicalResult.path("position_level").asText("");
    breakdown.positionLevelScore = calculatePositionLevelScore(positionLevel);
    score += breakdown.positionLevelScore;

    // Технологический стек (15 баллов)
    String stack = technicalResult.path("stack").asText("");
    breakdown.stackScore = calculateStackScore(stack);
    score += breakdown.stackScore;

    breakdown.technicalTotal = score;
    return score;
  }

  /**
   * Расчет баллов за тип роли
   */
  private int calculateRoleTypeScore(String roleType) {
    return switch (roleType) {
      case "backend" -> 15;                   // Backend разработка
      case "frontend_plus_backend" -> 13;     // Full-stack с backend
      case "devops_with_dev" -> 10;           // DevOps с разработкой
      case "other" -> 0;                      // Не разработка
      default -> 0;
    };
  }

  /**
   * Расчет баллов за уровень позиции
   */
  private int calculatePositionLevelScore(String positionLevel) {
    return switch (positionLevel) {
      case "architect" -> 15;                 // Архитектор
      case "principal" -> 14;                 // Principal
      case "senior" -> 13;                    // Senior
      case "lead" -> 12;                      // Team Lead
      case "middle" -> 8;                     // Middle
      case "junior" -> 4;                     // Junior
      default -> 0;
    };
  }

  /**
   * Расчет баллов за технологический стек
   */
  private int calculateStackScore(String stack) {
    if (stack == null || stack.isEmpty()) {
      return 0;
    }

    int score = 0;
    Set<String> technologies = Set.of(stack.toLowerCase().split("\\|"));

    // Spring экосистема (5 баллов)
    if (technologies.contains("spring")) {
      score += 5;
    }

    // Микросервисы (4 балла)
    if (technologies.contains("microservices")) {
      score += 4;
    }

    // Базы данных (3 балла)
    if (technologies.contains("database")) {
      score += 3;
    }

    // Python (дополнительный язык) (2 балла)
    if (technologies.contains("python")) {
      score += 2;
    }

    // DevOps инструменты (1 балл)
    if (technologies.contains("devops")) {
      score += 1;
    }

    // Максимум 15 баллов за стек
    return Math.min(score, 15);
  }

  /**
   * Анализ зарплаты (максимум 20 баллов)
   */
  private int calculateSalaryScore(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    JsonNode salaryResult = analysis.getStepResult("salary");
    if (salaryResult == null) {
      return 0;
    }

    int score = 0;

    // Наличие указанной зарплаты (5 баллов)
    if (salaryResult.path("has_salary").asBoolean(false)) {
      score += 5;

      // Уровень зарплаты (15 баллов)
      int salaryFrom = salaryResult.path("salary_from").asInt(0);
      score += calculateSalaryLevelScore(salaryFrom);
    }

    breakdown.salaryScore = score;
    return score;
  }

  /**
   * Расчет баллов за уровень зарплаты (для России, в рублях)
   */
  private int calculateSalaryLevelScore(int salaryFrom) {
    if (salaryFrom >= 400000) return 15;       // 400k+ - отличная зарплата
    if (salaryFrom >= 300000) return 12;       // 300-400k - хорошая зарплата
    if (salaryFrom >= 200000) return 9;        // 200-300k - нормальная зарплата
    if (salaryFrom >= 150000) return 6;        // 150-200k - средняя зарплата
    if (salaryFrom >= 100000) return 3;        // 100-150k - низкая зарплата
    return 0;                                  // <100k - очень низкая
  }

  /**
   * Исследование компании (максимум 15 баллов)
   */
  private int calculateCompanyScore(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    JsonNode companyResult = analysis.getStepResult("company_research");
    if (companyResult == null) {
      return 0;
    }

    int score = 0;

    // Найдена информация о компании (5 баллов)
    if (companyResult.path("found_info").asBoolean(false)) {
      score += 5;
    }

    // Известная компания (5 баллов)
    if (companyResult.path("is_known_company").asBoolean(false)) {
      score += 5;
    }

    // Хорошая репутация (5 баллов)
    if (companyResult.path("good_reputation").asBoolean(false)) {
      score += 5;
    }

    breakdown.companyScore = score;
    return score;
  }

  /**
   * Бонус за полноту анализа (максимум 10 баллов)
   */
  private int calculateCompletenessBonus(VacancyAnalysis analysis, ScoreBreakdown breakdown) {
    int completedSteps = 0;

    // Основные шаги
    if (analysis.hasStepResult("primary")) completedSteps++;
    if (analysis.hasStepResult("social")) completedSteps++;
    if (analysis.hasStepResult("technical")) completedSteps++;

    // Дополнительные шаги
    if (analysis.hasStepResult("salary")) completedSteps++;
    if (analysis.hasStepResult("company_research")) completedSteps++;

    // Бонус: 2 балла за каждый выполненный шаг свыше 3
    int bonus = Math.max(0, (completedSteps - 3) * 2);
    breakdown.completenessBonus = Math.min(bonus, 10);

    return breakdown.completenessBonus;
  }

  /**
   * Определение рейтинга на основе общего скора
   */
  private Rating determineRating(int totalScore) {
    if (totalScore >= 140) return Rating.EXCELLENT;    // 85%+ от максимума
    if (totalScore >= 110) return Rating.GOOD;         // 65-85%
    if (totalScore >= 80) return Rating.MODERATE;      // 45-65%
    if (totalScore >= 50) return Rating.POOR;          // 25-45%
    return Rating.VERY_POOR;                           // <25%
  }

  /**
   * Результат скоринга вакансии
   */
  public static record VacancyScore(
      int totalScore,
      Rating rating,
      ScoreBreakdown breakdown
  ) {

    /**
     * Получить максимально возможный скор
     */
    public static int getMaxPossibleScore() {
      return 80 +    // Primary (Java + Jmix + AI)
          35 +    // Social (work mode + significance)
          45 +    // Technical (role + level + stack)
          20 +    // Salary
          15 +    // Company
          10;     // Completeness bonus
      // = 205 максимум
    }

    /**
     * Получить процент от максимального скора
     */
    public double getScorePercentage() {
      return (double) totalScore / getMaxPossibleScore() * 100;
    }

    /**
     * Получить текстовое описание результата
     */
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
   * Рейтинг вакансии
   */
  public enum Rating {
    EXCELLENT,    // Отлично (85%+)
    GOOD,         // Хорошо (65-85%)
    MODERATE,     // Средне (45-65%)
    POOR,         // Плохо (25-45%)
    VERY_POOR     // Очень плохо (<25%)
  }

  /**
   * Детализация скора по категориям
   */
  public static class ScoreBreakdown {
    // Первичный анализ
    public int javaScore = 0;
    public int jmixScore = 0;
    public int aiScore = 0;
    public int primaryTotal = 0;

    // Социальный анализ
    public int workModeScore = 0;
    public int socialSignificanceScore = 0;
    public int socialTotal = 0;

    // Технический анализ
    public int roleTypeScore = 0;
    public int positionLevelScore = 0;
    public int stackScore = 0;
    public int technicalTotal = 0;

    // Дополнительные критерии
    public int salaryScore = 0;
    public int companyScore = 0;
    public int completenessBonus = 0;

    /**
     * Получить общий скор
     */
    public int getTotalScore() {
      return primaryTotal + socialTotal + technicalTotal +
          salaryScore + companyScore + completenessBonus;
    }

    /**
     * Получить строковое представление разбивки
     */
    public String getBreakdownText() {
      StringBuilder sb = new StringBuilder();
      sb.append("Первичный: ").append(primaryTotal);
      if (socialTotal > 0) sb.append(", Социальный: ").append(socialTotal);
      if (technicalTotal > 0) sb.append(", Технический: ").append(technicalTotal);
      if (salaryScore > 0) sb.append(", Зарплата: ").append(salaryScore);
      if (companyScore > 0) sb.append(", Компания: ").append(companyScore);
      if (completenessBonus > 0) sb.append(", Бонус: ").append(completenessBonus);
      return sb.toString();
    }

    /**
     * Проверить, является ли это Java-вакансией
     */
    public boolean isJavaPosition() {
      return javaScore > 0;
    }

    /**
     * Получить основную причину высокого/низкого скора
     */
    public String getMainScoreReason() {
      if (javaScore == 0) return "Не Java позиция";
      if (workModeScore <= 3) return "Только офисная работа";
      if (primaryTotal >= 60) return "Отличное техническое соответствие";
      if (salaryScore >= 12) return "Высокая зарплата";
      if (socialTotal >= 30) return "Хорошие условия работы";
      return "Среднее соответствие критериям";
    }
  }
}
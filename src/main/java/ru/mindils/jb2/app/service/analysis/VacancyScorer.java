package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.VacancyScoringResult;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysis;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.mindils.jb2.app.service.analysis.ScoringRules.*;

/**
 * Калькулятор оценки вакансии на основе результатов LLM анализа
 * Возвращает оценку + описание позитивных и негативных факторов
 */
@Component
public class VacancyScorer {

  private static final Logger log = LoggerFactory.getLogger(VacancyScorer.class);

  /**
   * Рассчитывает общую оценку вакансии с описанием факторов
   *
   * @param analyses список результатов анализа вакансии
   * @return результат с оценкой и описанием
   */
  public VacancyScoringResult calculateScore(List<VacancyLlmAnalysis> analyses) {
    VacancyScoringResult result = new VacancyScoringResult();

    if (analyses == null || analyses.isEmpty()) {
      log.warn("No analysis data provided for scoring");
      return result;
    }

    log.info("Calculating score based on {} analysis results", analyses.size());

    // Группируем анализы по типу
    Map<VacancyLlmAnalysisType, JsonNode> analysisMap = analyses.stream()
        .filter(a -> a.getAnalyzeData() != null)
        .collect(Collectors.toMap(
            a -> VacancyLlmAnalysisType.fromId(a.getAnalyzeType()),
            VacancyLlmAnalysis::getAnalyzeData,
            (existing, replacement) -> replacement
        ));

    // Суммируем оценки и собираем описания
    calculatePrimaryScore(analysisMap.get(VacancyLlmAnalysisType.JAVA_PRIMARY), result);
    calculateTechnicalScore(analysisMap.get(VacancyLlmAnalysisType.TECHNICAL), result);
    calculateCompensationScore(analysisMap.get(VacancyLlmAnalysisType.COMPENSATION), result);
    calculateBenefitsScore(analysisMap.get(VacancyLlmAnalysisType.BENEFITS), result);
    calculateEquipmentScore(analysisMap.get(VacancyLlmAnalysisType.EQUIPMENT), result);
    calculateIndustryScore(analysisMap.get(VacancyLlmAnalysisType.INDUSTRY), result);
    calculateWorkConditionsScore(analysisMap.get(VacancyLlmAnalysisType.WORK_CONDITIONS), result);
    calculateStopFactorsScore(analysisMap.get(VacancyLlmAnalysisType.STOP_FACTORS), result);

    log.info("Total calculated score: {} points, positive factors: {}, negative factors: {}",
        result.getTotalScore(), result.getPositiveFactors().size(), result.getNegativeFactors().size());

    return result;
  }

  private void calculatePrimaryScore(JsonNode primary, VacancyScoringResult result) {
    if (primary == null) return;

    if (getBooleanValue(primary, "java")) {
      applyRule(JAVA_PRIMARY_JAVA, result);
    }
    if (getBooleanValue(primary, "jmix")) {
      applyRule(JAVA_PRIMARY_JMIX, result);
    }
  }

  private void calculateTechnicalScore(JsonNode technical, VacancyScoringResult result) {
    if (technical == null) return;

    // Тип роли
    String roleType = getStringValue(technical, "role_type");
    switch (roleType) {
      case "backend" -> applyRule(TECHNICAL_ROLE_BACKEND, result);
      case "fullstack" -> applyRule(TECHNICAL_ROLE_FULLSTACK, result);
      case "devops" -> applyRule(TECHNICAL_ROLE_DEVOPS, result);
      case "other" -> applyRule(TECHNICAL_ROLE_OTHER, result);
      case "none" -> applyRule(TECHNICAL_ROLE_NONE, result);
    }

    // Уровень позиции
    String positionLevel = getStringValue(technical, "position_level");
    switch (positionLevel) {
      case "architect" -> applyRule(TECHNICAL_LEVEL_ARCHITECT, result);
      case "principal" -> applyRule(TECHNICAL_LEVEL_PRINCIPAL, result);
      case "senior" -> applyRule(TECHNICAL_LEVEL_SENIOR, result);
      case "lead" -> applyRule(TECHNICAL_LEVEL_LEAD, result);
      case "middle" -> applyRule(TECHNICAL_LEVEL_MIDDLE, result);
      case "junior" -> applyRule(TECHNICAL_LEVEL_JUNIOR, result);
      case "none" -> applyRule(TECHNICAL_LEVEL_NONE, result);
    }

    // Стек технологий
    String stack = getStringValue(technical, "stack");
    if (!stack.isEmpty() && !stack.equals("none")) {
      for (String tech : stack.toLowerCase().split("\\|")) {
        switch (tech.trim()) {
          case "spring" -> applyRule(TECHNICAL_STACK_SPRING, result);
          case "microservices" -> applyRule(TECHNICAL_STACK_MICROSERVICES, result);
          case "database" -> applyRule(TECHNICAL_STACK_DATABASE, result);
          case "python" -> applyRule(TECHNICAL_STACK_PYTHON, result);
          case "devops" -> applyRule(TECHNICAL_STACK_DEVOPS, result);
          case "frontend" -> applyRule(TECHNICAL_STACK_FRONTEND, result);
        }
      }
    } else if (stack.equals("none")) {
      applyRule(TECHNICAL_STACK_NONE, result);
    }

    // AI присутствие
    String aiPresence = getStringValue(technical, "ai_presence");
    switch (aiPresence) {
      case "allowed" -> applyRule(TECHNICAL_AI_ALLOWED, result);
      case "project_optional" -> applyRule(TECHNICAL_AI_PROJECT_OPTIONAL, result);
      case "project_required" -> applyRule(TECHNICAL_AI_PROJECT_REQUIRED, result);
      case "none" -> applyRule(TECHNICAL_AI_NONE, result);
    }

    // Jmix платформа
    if (getBooleanValue(technical, "jmix")) {
      applyRule(JAVA_PRIMARY_JMIX, result);
    }
  }

  private void calculateCompensationScore(JsonNode compensation, VacancyScoringResult result) {
    if (compensation == null) return;

    String salaryRange = getStringValue(compensation, "salaryRange");
    boolean hasBonuses = getBooleanValue(compensation, "bonusesAvailable");

    // Обрабатываем диапазон зарплаты
    switch (salaryRange) {
      case "high_400plus" -> applyRule(COMPENSATION_HIGH_400, result);
      case "upper_350_400" -> applyRule(hasBonuses ? COMPENSATION_350_400_BONUS : COMPENSATION_350_400, result);
      case "middle_300_350" -> applyRule(hasBonuses ? COMPENSATION_300_350_BONUS : COMPENSATION_300_350, result);
      case "lower_250_300" -> applyRule(COMPENSATION_250_300, result);
      case "below_250" -> applyRule(COMPENSATION_BELOW_250, result);
      case "none" -> applyRule(COMPENSATION_NONE, result);
    }

    // Обрабатываем тип зарплаты (белая/серая/не указано)
    String salaryType = getStringValue(compensation, "salaryType");
    switch (salaryType) {
      case "white" -> applyRule(COMPENSATION_TYPE_WHITE, result);
      case "gray" -> applyRule(COMPENSATION_TYPE_GRAY, result);
      case "none" -> applyRule(COMPENSATION_TYPE_NONE, result);
    }

    // Обрабатываем опционы/акции
    if (getBooleanValue(compensation, "equityOffered")) {
      applyRule(COMPENSATION_EQUITY, result);
    }
  }

  private void calculateBenefitsScore(JsonNode benefits, VacancyScoringResult result) {
    if (benefits == null) return;

    if (getBooleanValue(benefits, "health_insurance")) applyRule(BENEFITS_HEALTH_INSURANCE, result);
    if (getBooleanValue(benefits, "extended_vacation")) applyRule(BENEFITS_EXTENDED_VACATION, result);
    if (getBooleanValue(benefits, "wellness")) applyRule(BENEFITS_WELLNESS, result);
    if (getBooleanValue(benefits, "remote_compensation")) applyRule(BENEFITS_REMOTE_COMPENSATION, result);
    if (getBooleanValue(benefits, "education")) applyRule(BENEFITS_EDUCATION, result);
    if (getBooleanValue(benefits, "conferences")) applyRule(BENEFITS_CONFERENCES, result);
    if (getBooleanValue(benefits, "internal_training")) applyRule(BENEFITS_INTERNAL_TRAINING, result);
    if (getBooleanValue(benefits, "paid_sick_leave")) applyRule(BENEFITS_PAID_SICK_LEAVE, result);
  }

  private void calculateEquipmentScore(JsonNode equipment, VacancyScoringResult result) {
    if (equipment == null) return;

    // BYOD (можно ли со своим ноутбуком)
    String byodAllowed = getStringValue(equipment, "byod_allowed");
    switch (byodAllowed) {
      case "yes" -> applyRule(EQUIPMENT_BYOD_YES, result);
      case "no" -> applyRule(EQUIPMENT_BYOD_NO, result);
      case "none" -> applyRule(EQUIPMENT_BYOD_NONE, result);
    }

    // macOS упоминание
    String macosMentioned = getStringValue(equipment, "macos_mentioned");
    switch (macosMentioned) {
      case "provided" -> applyRule(EQUIPMENT_MACOS_PROVIDED, result);
      case "allowed" -> applyRule(EQUIPMENT_MACOS_ALLOWED, result);
      case "both" -> applyRule(EQUIPMENT_MACOS_BOTH, result);
      case "none" -> applyRule(EQUIPMENT_MACOS_NONE, result);
    }

    // Компенсация техники
    String compensation = getStringValue(equipment, "equipment_compensation");
    switch (compensation) {
      case "full" -> applyRule(EQUIPMENT_COMPENSATION_FULL, result);
      case "partial" -> applyRule(EQUIPMENT_COMPENSATION_PARTIAL, result);
      case "none" -> applyRule(EQUIPMENT_COMPENSATION_NONE, result);
    }
  }

  private void calculateIndustryScore(JsonNode industry, VacancyScoringResult result) {
    if (industry == null) return;

    // Обрабатываем категорию компании
    String companyCategory = getStringValue(industry, "company_category");
    switch (companyCategory) {
      case "safe" -> applyRule(INDUSTRY_COMPANY_SAFE, result);
      case "neutral" -> applyRule(INDUSTRY_COMPANY_NEUTRAL, result);
      case "problematic" -> applyRule(INDUSTRY_COMPANY_PROBLEMATIC, result);
      case "toxic" -> applyRule(INDUSTRY_COMPANY_TOXIC, result);
    }

    // Обрабатываем категорию проекта (только если отличается от компании)
    String projectCategory = getStringValue(industry, "project_category");
    if (!projectCategory.equals(companyCategory) && !projectCategory.isEmpty()) {
      switch (projectCategory) {
        case "safe" -> applyRule(INDUSTRY_PROJECT_SAFE, result);
        case "problematic" -> applyRule(INDUSTRY_PROJECT_PROBLEMATIC, result);
        case "toxic" -> applyRule(INDUSTRY_PROJECT_TOXIC, result);
        // neutral не обрабатываем - это нейтрально
      }
    }

    // Опционально: добавляем бонусы за конкретные направления
    String companyDirection = getStringValue(industry, "company_direction");
    String projectDirection = getStringValue(industry, "project_direction");

    // Берем direction проекта, если он отличается, иначе компании
    String mainDirection = !projectDirection.isEmpty() && !projectDirection.equals(companyDirection)
        ? projectDirection
        : companyDirection;

    // Дополнительные баллы за особо значимые направления
    switch (mainDirection) {
      case "healthcare" -> applyRule(INDUSTRY_DIRECTION_HEALTHCARE, result);
      case "education" -> applyRule(INDUSTRY_DIRECTION_EDUCATION, result);
      case "energy" -> applyRule(INDUSTRY_DIRECTION_ENERGY, result);
      case "harmful" -> applyRule(INDUSTRY_DIRECTION_HARMFUL, result);
      // остальные направления (b2b_tech, fintech, consumer, advertising)
      // уже учтены через категорию, дополнительных баллов не даем
    }
  }

  private void calculateWorkConditionsScore(JsonNode workConditions, VacancyScoringResult result) {
    if (workConditions == null) return;

    String workFormat = getStringValue(workConditions, "workFormat");
    String canWorkAbroad = getStringValue(workConditions, "canWorkAbroad");

    // Обрабатываем формат работы
    switch (workFormat) {
      case "remote" -> {
        // Для удаленки смотрим географические ограничения
        if ("yes".equals(canWorkAbroad)) {
          applyRule(WORK_CONDITIONS_REMOTE_GLOBAL, result); // 100 баллов - можно из любой страны
        } else if ("no".equals(canWorkAbroad)) {
          applyRule(WORK_CONDITIONS_REMOTE_RESTRICTED, result); // 30 баллов - только из РФ
        } else {
          applyRule(WORK_CONDITIONS_REMOTE, result); // 20 баллов - не указано
        }
      }
      case "hybrid_flexible" -> applyRule(WORK_CONDITIONS_HYBRID_FLEXIBLE, result); // 20 баллов - до 1 дня в офисе
      case "hybrid_regular" -> applyRule(WORK_CONDITIONS_HYBRID, result); // 20 баллов - 1-2 дня в офисе
      case "hybrid_frequent" -> applyRule(WORK_CONDITIONS_HYBRID_FREQUENT, result); // 0 баллов - 3+ дня в офисе
      case "office_only" -> applyRule(WORK_CONDITIONS_OFFICE, result); // -100 баллов - только офис
      // "none" - не обрабатываем, нет информации
    }

    // Обрабатываем релокацию
    String relocation = getStringValue(workConditions, "relocationRequired");
    switch (relocation) {
      case "optional" -> applyRule(WORK_CONDITIONS_RELOCATION_ASSISTED, result); // -100 баллов - опциональная релокация
      case "required" -> applyRule(WORK_CONDITIONS_RELOCATION_REQUIRED, result); // -100 баллов - обязательная релокация
      // "none" - не обрабатываем, релокация не требуется
    }
  }

  private void calculateStopFactorsScore(JsonNode stopFactors, VacancyScoringResult result) {
    if (stopFactors == null) return;

    if (getBooleanValue(stopFactors, "toxic_culture")) {
      applyRule(STOP_FACTORS_TOXIC_CULTURE, result);
    }
    if (getBooleanValue(stopFactors, "banned_domain")) {
      applyRule(STOP_FACTORS_BANNED_DOMAIN, result);
    }
  }

  // ============ UTILITY МЕТОДЫ ============

  /**
   * Применяет правило: добавляет оценку и описание в результат
   * Если description = null, то добавляется только оценка без описания
   */
  private void applyRule(Rule rule, VacancyScoringResult result) {
    result.addScore(rule.getScore());

    // Добавляем описание только если оно задано
    if (rule.getDescription() != null) {
      if (rule.getScore() > 0) {
        result.addPositive(rule.getDescription());
      } else if (rule.getScore() < 0) {
        result.addNegative(rule.getDescription());
      }
    }
  }

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
}
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
    calculateSocialScore(analysisMap.get(VacancyLlmAnalysisType.SOCIAL), result);
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
      applyRule(JAVA, result);
    }
    if (getBooleanValue(primary, "jmix")) {
      applyRule(JMIX, result);
    }
  }

  private void calculateSocialScore(JsonNode social, VacancyScoringResult result) {
    if (social == null) return;

    String workMode = getStringValue(social, "work_mode");
    switch (workMode) {
      case "remote" -> applyRule(REMOTE, result);
      case "flexible" -> applyRule(FLEXIBLE, result);
      case "hybrid_flexible" -> applyRule(HYBRID_FLEXIBLE, result);
      case "hybrid" -> applyRule(HYBRID, result);
      case "hybrid_2_3" -> applyRule(HYBRID_2_3, result);
      case "hybrid_3_2" -> applyRule(HYBRID_3_2, result);
      case "hybrid_4_1" -> applyRule(HYBRID_4_1, result);
      case "office" -> applyRule(OFFICE, result);
    }

    if (getBooleanValue(social, "socially_significant")) {
      applyRule(SOCIALLY_SIGNIFICANT, result);
    }
  }

  private void calculateTechnicalScore(JsonNode technical, VacancyScoringResult result) {
    if (technical == null) return;

    // Тип роли
    String roleType = getStringValue(technical, "role_type");
    switch (roleType) {
      case "backend" -> applyRule(BACKEND, result);
      case "frontend_plus_backend" -> applyRule(FRONTEND_BACKEND, result);
      case "devops_with_dev" -> applyRule(DEVOPS_WITH_DEV, result);
      case "other" -> applyRule(OTHER_ROLE, result);
    }

    // Уровень позиции
    String positionLevel = getStringValue(technical, "position_level");
    switch (positionLevel) {
      case "architect" -> applyRule(ARCHITECT, result);
      case "principal" -> applyRule(PRINCIPAL, result);
      case "senior" -> applyRule(SENIOR, result);
      case "lead" -> applyRule(LEAD, result);
      case "middle" -> applyRule(MIDDLE, result);
      case "junior" -> applyRule(JUNIOR, result);
    }

    // Стек технологий
    String stack = getStringValue(technical, "stack");
    if (!stack.isEmpty()) {
      for (String tech : stack.toLowerCase().split("\\|")) {
        switch (tech.trim()) {
          case "spring" -> applyRule(SPRING_STACK, result);
          case "microservices" -> applyRule(MICROSERVICES, result);
          case "database" -> applyRule(DATABASE, result);
          case "python" -> applyRule(PYTHON, result);
          case "devops" -> applyRule(DEVOPS_STACK, result);
          case "frontend" -> applyRule(FRONTEND_STACK, result);
        }
      }
    }

    // AI присутствие
    String aiPresence = getStringValue(technical, "ai_presence");
    if (!aiPresence.isEmpty()) {
      for (String aiType : aiPresence.split("\\|")) {
        switch (aiType.trim()) {
          case "allowed_for_dev" -> applyRule(AI_ALLOWED, result);
          case "llm_project_optional" -> applyRule(AI_PROJECT_OPTIONAL, result);
          case "llm_project_required" -> applyRule(AI_PROJECT_REQUIRED, result);
        }
      }
    }

    if (getBooleanValue(technical, "jmix")) {
      applyRule(JMIX, result);
    }
  }

  private void calculateCompensationScore(JsonNode compensation, VacancyScoringResult result) {
    if (compensation == null) return;

    boolean salarySpecified = getBooleanValue(compensation, "salarySpecified");
    if (!salarySpecified) {
      applyRule(SALARY_NOT_SPECIFIED, result);
      return;
    }

    String salaryRange = getStringValue(compensation, "salaryRange");
    boolean hasBonuses = getBooleanValue(compensation, "bonusesAvailable");

    switch (salaryRange) {
      case "high_400plus" -> applyRule(SALARY_HIGH_400, result);
      case "upper_350_400" -> applyRule(hasBonuses ? SALARY_350_400_BONUS : SALARY_350_400, result);
      case "middle_300_350" -> applyRule(hasBonuses ? SALARY_300_350_BONUS : SALARY_300_350, result);
      case "lower_250_300" -> applyRule(SALARY_250_300, result);
      case "below_250" -> applyRule(SALARY_BELOW_250, result);
    }

    boolean isWhite = getBooleanValue(compensation, "salaryWhite");
    applyRule(isWhite ? WHITE_SALARY : GRAY_SALARY, result);

    if (getBooleanValue(compensation, "equityOffered")) {
      applyRule(EQUITY, result);
    }
  }

  private void calculateBenefitsScore(JsonNode benefits, VacancyScoringResult result) {
    if (benefits == null) return;

    if (getBooleanValue(benefits, "healthInsurance")) applyRule(HEALTH_INSURANCE, result);
    if (getBooleanValue(benefits, "extendedVacation")) applyRule(EXTENDED_VACATION, result);
    if (getBooleanValue(benefits, "wellnessCompensation")) applyRule(WELLNESS, result);
    if (getBooleanValue(benefits, "coworkingCompensation")) applyRule(COWORKING, result);
    if (getBooleanValue(benefits, "educationCompensation")) applyRule(EDUCATION, result);
    if (getBooleanValue(benefits, "conferencesBudget")) applyRule(CONFERENCES, result);
    if (getBooleanValue(benefits, "internalTraining")) applyRule(INTERNAL_TRAINING, result);
    if (getBooleanValue(benefits, "paidSickLeave")) applyRule(PAID_SICK_LEAVE, result);
  }

  private void calculateEquipmentScore(JsonNode equipment, VacancyScoringResult result) {
    if (equipment == null) return;

    String equipmentType = getStringValue(equipment, "equipmentType");
    switch (equipmentType) {
      case "macbook_pro" -> applyRule(MACBOOK_PRO, result);
      case "windows_laptop" -> applyRule(WINDOWS_LAPTOP, result);
      case "byod" -> {
        String compensation = getStringValue(equipment, "byodCompensation");
        switch (compensation) {
          case "full" -> applyRule(BYOD_FULL, result);
          case "partial" -> applyRule(BYOD_PARTIAL, result);
          case "none" -> applyRule(BYOD_NO_COMP, result);
        }
      }
    }

    String additional = getStringValue(equipment, "additionalEquipment");
    if (additional != null && !additional.equals("none")) {
      if (additional.contains("monitors")) applyRule(MONITORS, result);
      if (additional.contains("peripherals")) applyRule(PERIPHERALS, result);
    }
  }

  private void calculateIndustryScore(JsonNode industry, VacancyScoringResult result) {
    if (industry == null) return;

    String companyCategory = getStringValue(industry, "company_category");
    switch (companyCategory) {
      case "positive" -> applyRule(POSITIVE_COMPANY, result);
      case "neutral" -> applyRule(NEUTRAL_COMPANY, result);
      case "problematic" -> applyRule(PROBLEMATIC_COMPANY, result);
      case "toxic" -> applyRule(TOXIC_COMPANY, result);
    }

    // Категория проекта (если отличается от компании)
    String projectCategory = getStringValue(industry, "project_category");
    if (!projectCategory.equals(companyCategory) && !projectCategory.isEmpty()) {
      Rule projectRule = switch (projectCategory) {
        case "positive" -> new Rule(40, "Позитивный проект");
        case "problematic" -> new Rule(-25, "Проблемный проект");
        case "toxic" -> new Rule(-75, "Токсичный проект");
        default -> null;
      };
      if (projectRule != null) {
        applyRule(projectRule, result);
      }
    }
  }

  private void calculateWorkConditionsScore(JsonNode workConditions, VacancyScoringResult result) {
    if (workConditions == null) return;

    String workFormat = getStringValue(workConditions, "workFormat");
    switch (workFormat) {
      case "remote_global" -> applyRule(REMOTE_GLOBAL, result);
      case "remote_restricted" -> applyRule(REMOTE_RESTRICTED, result);
      case "hybrid_frequent" -> applyRule(HYBRID_FREQUENT, result);
    }

    String relocation = getStringValue(workConditions, "relocationRequired");
    switch (relocation) {
      case "assisted" -> applyRule(RELOCATION_ASSISTED, result);
      case "required_no_help", "mandatory_specific" -> applyRule(RELOCATION_REQUIRED, result);
    }
  }

  private void calculateStopFactorsScore(JsonNode stopFactors, VacancyScoringResult result) {
    if (stopFactors == null) return;

    if (getBooleanValue(stopFactors, "toxicCulture")) {
      applyRule(TOXIC_CULTURE, result);
    }
    if (getBooleanValue(stopFactors, "bannedDomain")) {
      applyRule(BANNED_DOMAIN, result);
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
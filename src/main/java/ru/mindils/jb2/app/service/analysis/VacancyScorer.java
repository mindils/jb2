package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysis;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Калькулятор оценки вакансии на основе результатов LLM анализа
 * Использует гибкую систему весов для каждого параметра
 */
@Component
public class VacancyScorer {

  private static final Logger log = LoggerFactory.getLogger(VacancyScorer.class);

  // ============ ВЕСА ДЛЯ ПАРАМЕТРОВ ============

  // Primary (основные требования)
  private static final int JAVA_SCORE = 100;
  private static final int JMIX_SCORE = 50;
  private static final int AI_ALLOWED_SCORE = 30;
  private static final int AI_PROJECT_OPTIONAL = 40;
  private static final int AI_PROJECT_REQUIRED = -50;

  // Social (формат работы)
  private static final int REMOTE_SCORE = 100;
  private static final int FLEXIBLE_SCORE = 90;
  private static final int HYBRID_FLEXIBLE_SCORE = 80;
  private static final int HYBRID_SCORE = 50;
  private static final int HYBRID_2_3_SCORE = 60;
  private static final int HYBRID_3_2_SCORE = 40;
  private static final int HYBRID_4_1_SCORE = 20;
  private static final int OFFICE_SCORE = -100;
  private static final int SOCIALLY_SIGNIFICANT_SCORE = 50;

  // Technical (роль и уровень)
  private static final int BACKEND_SCORE = 50;
  private static final int FRONTEND_BACKEND_SCORE = 40;
  private static final int DEVOPS_WITH_DEV_SCORE = 30;
  private static final int OTHER_ROLE_SCORE = -100;

  private static final int ARCHITECT_SCORE = 80;
  private static final int PRINCIPAL_SCORE = 70;
  private static final int SENIOR_SCORE = 60;
  private static final int LEAD_SCORE = 50;
  private static final int MIDDLE_SCORE = 30;
  private static final int JUNIOR_SCORE = 10;

  // Technical (стек технологий) - за каждую технологию
  private static final int SPRING_STACK_SCORE = 30;
  private static final int MICROSERVICES_STACK_SCORE = 20;
  private static final int DATABASE_STACK_SCORE = 15;
  private static final int PYTHON_STACK_SCORE = 10;
  private static final int DEVOPS_STACK_SCORE = 10;
  private static final int FRONTEND_STACK_SCORE = -10;

  // Compensation (зарплата)
  private static final int SALARY_NOT_SPECIFIED = 0;
  private static final int SALARY_HIGH_400_BASE = 100;
  private static final int SALARY_350_400_BASE = 50;
  private static final int SALARY_350_400_WITH_BONUS = 60;
  private static final int SALARY_300_350_BASE = -50;
  private static final int SALARY_300_350_WITH_BONUS = 20;
  private static final int SALARY_250_300 = -100;
  private static final int SALARY_BELOW_250 = -200;
  private static final int WHITE_SALARY_SCORE = 50;
  private static final int GRAY_SALARY_SCORE = -200;
  private static final int EQUITY_SCORE = 30;

  // Benefits (льготы) - за каждую льготу
  private static final int HEALTH_INSURANCE_SCORE = 30;
  private static final int EXTENDED_VACATION_SCORE = 40;
  private static final int WELLNESS_COMPENSATION_SCORE = 25;
  private static final int COWORKING_COMPENSATION_SCORE = 35;
  private static final int EDUCATION_COMPENSATION_SCORE = 50;
  private static final int CONFERENCES_BUDGET_SCORE = 40;
  private static final int INTERNAL_TRAINING_SCORE = 20;
  private static final int PAID_SICK_LEAVE_SCORE = 25;

  // Equipment (оборудование)
  private static final int MACBOOK_PRO_SCORE = 50;
  private static final int WINDOWS_LAPTOP_SCORE = 20;
  private static final int BYOD_NO_COMPENSATION = -50;
  private static final int BYOD_PARTIAL_COMPENSATION = 10;
  private static final int BYOD_FULL_COMPENSATION = 30;
  private static final int MONITORS_SCORE = 20;
  private static final int PERIPHERALS_SCORE = 15;

  // Industry (отрасль)
  private static final int POSITIVE_COMPANY_SCORE = 80;
  private static final int NEUTRAL_COMPANY_SCORE = 0;
  private static final int PROBLEMATIC_COMPANY_SCORE = -50;
  private static final int TOXIC_COMPANY_SCORE = -150;

  // Work Conditions (условия работы)
  private static final int REMOTE_GLOBAL_SCORE = 50;
  private static final int REMOTE_RESTRICTED_SCORE = 30;
  private static final int RELOCATION_REQUIRED_SCORE = -100;
  private static final int RELOCATION_ASSISTED_SCORE = -30;

  // Stop Factors (стоп-факторы)
  private static final int TOXIC_CULTURE_SCORE = -300;
  private static final int BANNED_DOMAIN_SCORE = -500;

  /**
   * Рассчитывает общую оценку вакансии на основе результатов LLM анализа
   *
   * @param analyses список результатов анализа вакансии
   * @return итоговая оценка (может быть отрицательной)
   */
  public int calculateScore(List<VacancyLlmAnalysis> analyses) {
    if (analyses == null || analyses.isEmpty()) {
      log.warn("No analysis data provided for scoring");
      return 0;
    }

    log.info("Calculating score based on {} analysis results", analyses.size());

    // Группируем анализы по типу для удобного доступа
    Map<VacancyLlmAnalysisType, JsonNode> analysisMap = analyses.stream()
        .filter(a -> a.getAnalyzeData() != null)
        .collect(Collectors.toMap(
            a -> VacancyLlmAnalysisType.fromId(a.getAnalyzeType()),
            VacancyLlmAnalysis::getAnalyzeData,
            (existing, replacement) -> replacement // При дублях берем последний
        ));

    int totalScore = 0;

    // Суммируем оценки по каждому типу анализа
    totalScore += calculatePrimaryScore(analysisMap.get(VacancyLlmAnalysisType.JAVA_PRIMARY));
    totalScore += calculateSocialScore(analysisMap.get(VacancyLlmAnalysisType.SOCIAL));
    totalScore += calculateTechnicalScore(analysisMap.get(VacancyLlmAnalysisType.TECHNICAL));
    totalScore += calculateCompensationScore(analysisMap.get(VacancyLlmAnalysisType.COMPENSATION));
    totalScore += calculateBenefitsScore(analysisMap.get(VacancyLlmAnalysisType.BENEFITS));
    totalScore += calculateEquipmentScore(analysisMap.get(VacancyLlmAnalysisType.EQUIPMENT));
    totalScore += calculateIndustryScore(analysisMap.get(VacancyLlmAnalysisType.INDUSTRY));
    totalScore += calculateWorkConditionsScore(analysisMap.get(VacancyLlmAnalysisType.WORK_CONDITIONS));
    totalScore += calculateStopFactorsScore(analysisMap.get(VacancyLlmAnalysisType.STOP_FACTORS));

    log.info("Total calculated score: {} points", totalScore);
    return totalScore;
  }

  private int calculatePrimaryScore(JsonNode primary) {
    if (primary == null) return 0;
    int score = 0;

    if (getBooleanValue(primary, "java")) score += JAVA_SCORE;
    if (getBooleanValue(primary, "jmix")) score += JMIX_SCORE;

    log.debug("Primary score: {}", score);
    return score;
  }

  private int calculateSocialScore(JsonNode social) {
    if (social == null) return 0;
    int score = 0;

    String workMode = getStringValue(social, "work_mode");
    score += switch (workMode) {
      case "remote" -> REMOTE_SCORE;
      case "flexible" -> FLEXIBLE_SCORE;
      case "hybrid_flexible" -> HYBRID_FLEXIBLE_SCORE;
      case "hybrid" -> HYBRID_SCORE;
      case "hybrid_2_3" -> HYBRID_2_3_SCORE;
      case "hybrid_3_2" -> HYBRID_3_2_SCORE;
      case "hybrid_4_1" -> HYBRID_4_1_SCORE;
      case "office" -> OFFICE_SCORE;
      default -> 0;
    };

    if (getBooleanValue(social, "socially_significant")) {
      score += SOCIALLY_SIGNIFICANT_SCORE;
    }

    log.debug("Social score: {}", score);
    return score;
  }

  private int calculateTechnicalScore(JsonNode technical) {
    if (technical == null) return 0;
    int score = 0;

    String roleType = getStringValue(technical, "role_type");
    score += switch (roleType) {
      case "backend" -> BACKEND_SCORE;
      case "frontend_plus_backend" -> FRONTEND_BACKEND_SCORE;
      case "devops_with_dev" -> DEVOPS_WITH_DEV_SCORE;
      case "other" -> OTHER_ROLE_SCORE;
      default -> 0;
    };

    String positionLevel = getStringValue(technical, "position_level");
    score += switch (positionLevel) {
      case "architect" -> ARCHITECT_SCORE;
      case "principal" -> PRINCIPAL_SCORE;
      case "senior" -> SENIOR_SCORE;
      case "lead" -> LEAD_SCORE;
      case "middle" -> MIDDLE_SCORE;
      case "junior" -> JUNIOR_SCORE;
      default -> 0;
    };

    String stack = getStringValue(technical, "stack");
    if (stack != null && !stack.isEmpty()) {
      for (String tech : stack.toLowerCase().split("\\|")) {
        score += switch (tech.trim()) {
          case "spring" -> SPRING_STACK_SCORE;
          case "microservices" -> MICROSERVICES_STACK_SCORE;
          case "database" -> DATABASE_STACK_SCORE;
          case "python" -> PYTHON_STACK_SCORE;
          case "devops" -> DEVOPS_STACK_SCORE;
          case "frontend" -> FRONTEND_STACK_SCORE;
          default -> 0;
        };
      }
    }

    String aiPresence = getStringValue(technical, "ai_presence");
    if (aiPresence != null && !aiPresence.isEmpty()) {
      for (String aiType : aiPresence.split("\\|")) {
        score += switch (aiType.trim()) {
          case "allowed_for_dev" -> AI_ALLOWED_SCORE;
          case "llm_project_optional" -> AI_PROJECT_OPTIONAL;
          case "llm_project_required" -> AI_PROJECT_REQUIRED;
          default -> 0;
        };
      }
    }

    if (getBooleanValue(technical, "jmix")) {
      score += JMIX_SCORE;
    }

    log.debug("Technical score: {}", score);
    return score;
  }

  private int calculateCompensationScore(JsonNode compensation) {
    if (compensation == null) return 0;
    int score = 0;

    boolean salarySpecified = getBooleanValue(compensation, "salarySpecified");
    if (!salarySpecified) {
      return SALARY_NOT_SPECIFIED;
    }

    String salaryRange = getStringValue(compensation, "salaryRange");
    boolean hasBonuses = getBooleanValue(compensation, "bonusesAvailable");

    score += switch (salaryRange) {
      case "high_400plus" -> SALARY_HIGH_400_BASE;
      case "upper_350_400" -> hasBonuses ? SALARY_350_400_WITH_BONUS : SALARY_350_400_BASE;
      case "middle_300_350" -> hasBonuses ? SALARY_300_350_WITH_BONUS : SALARY_300_350_BASE;
      case "lower_250_300" -> SALARY_250_300;
      case "below_250" -> SALARY_BELOW_250;
      default -> 0;
    };

    boolean isWhite = getBooleanValue(compensation, "salaryWhite");
    score += isWhite ? WHITE_SALARY_SCORE : GRAY_SALARY_SCORE;

    if (getBooleanValue(compensation, "equityOffered")) {
      score += EQUITY_SCORE;
    }

    log.debug("Compensation score: {}", score);
    return score;
  }

  private int calculateBenefitsScore(JsonNode benefits) {
    if (benefits == null) return 0;
    int score = 0;

    if (getBooleanValue(benefits, "healthInsurance")) score += HEALTH_INSURANCE_SCORE;
    if (getBooleanValue(benefits, "extendedVacation")) score += EXTENDED_VACATION_SCORE;
    if (getBooleanValue(benefits, "wellnessCompensation")) score += WELLNESS_COMPENSATION_SCORE;
    if (getBooleanValue(benefits, "coworkingCompensation")) score += COWORKING_COMPENSATION_SCORE;
    if (getBooleanValue(benefits, "educationCompensation")) score += EDUCATION_COMPENSATION_SCORE;
    if (getBooleanValue(benefits, "conferencesBudget")) score += CONFERENCES_BUDGET_SCORE;
    if (getBooleanValue(benefits, "internalTraining")) score += INTERNAL_TRAINING_SCORE;
    if (getBooleanValue(benefits, "paidSickLeave")) score += PAID_SICK_LEAVE_SCORE;

    log.debug("Benefits score: {}", score);
    return score;
  }

  private int calculateEquipmentScore(JsonNode equipment) {
    if (equipment == null) return 0;
    int score = 0;

    String equipmentType = getStringValue(equipment, "equipmentType");
    score += switch (equipmentType) {
      case "macbook_pro" -> MACBOOK_PRO_SCORE;
      case "windows_laptop" -> WINDOWS_LAPTOP_SCORE;
      case "byod" -> {
        String compensation = getStringValue(equipment, "byodCompensation");
        yield switch (compensation) {
          case "full" -> BYOD_FULL_COMPENSATION;
          case "partial" -> BYOD_PARTIAL_COMPENSATION;
          case "none" -> BYOD_NO_COMPENSATION;
          default -> 0;
        };
      }
      default -> 0;
    };

    String additional = getStringValue(equipment, "additionalEquipment");
    if (additional != null && !additional.equals("none")) {
      if (additional.contains("monitors")) score += MONITORS_SCORE;
      if (additional.contains("peripherals")) score += PERIPHERALS_SCORE;
    }

    log.debug("Equipment score: {}", score);
    return score;
  }

  private int calculateIndustryScore(JsonNode industry) {
    if (industry == null) return 0;
    int score = 0;

    String companyCategory = getStringValue(industry, "company_category");
    score += switch (companyCategory) {
      case "positive" -> POSITIVE_COMPANY_SCORE;
      case "neutral" -> NEUTRAL_COMPANY_SCORE;
      case "problematic" -> PROBLEMATIC_COMPANY_SCORE;
      case "toxic" -> TOXIC_COMPANY_SCORE;
      default -> 0;
    };

    String projectCategory = getStringValue(industry, "project_category");
    if (!projectCategory.equals(companyCategory) && !projectCategory.isEmpty()) {
      score += switch (projectCategory) {
        case "positive" -> POSITIVE_COMPANY_SCORE / 2;
        case "neutral" -> 0;
        case "problematic" -> PROBLEMATIC_COMPANY_SCORE / 2;
        case "toxic" -> TOXIC_COMPANY_SCORE / 2;
        default -> 0;
      };
    }

    log.debug("Industry score: {}", score);
    return score;
  }

  private int calculateWorkConditionsScore(JsonNode workConditions) {
    if (workConditions == null) return 0;
    int score = 0;

    String workFormat = getStringValue(workConditions, "workFormat");
    score += switch (workFormat) {
      case "remote_global" -> REMOTE_GLOBAL_SCORE;
      case "remote_restricted" -> REMOTE_RESTRICTED_SCORE;
      case "hybrid_flexible" -> 0;
      case "hybrid_regular" -> 0;
      case "hybrid_frequent" -> -20;
      case "office_only" -> 0;
      default -> 0;
    };

    String relocation = getStringValue(workConditions, "relocationRequired");
    score += switch (relocation) {
      case "none" -> 0;
      case "assisted" -> RELOCATION_ASSISTED_SCORE;
      case "required_no_help" -> RELOCATION_REQUIRED_SCORE;
      case "mandatory_specific" -> RELOCATION_REQUIRED_SCORE;
      default -> 0;
    };

    log.debug("Work conditions score: {}", score);
    return score;
  }

  private int calculateStopFactorsScore(JsonNode stopFactors) {
    if (stopFactors == null) return 0;
    int score = 0;

    if (getBooleanValue(stopFactors, "toxicCulture")) score += TOXIC_CULTURE_SCORE;
    if (getBooleanValue(stopFactors, "bannedDomain")) score += BANNED_DOMAIN_SCORE;

    log.debug("Stop factors score: {}", score);
    return score;
  }

  // ============ UTILITY МЕТОДЫ ============

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
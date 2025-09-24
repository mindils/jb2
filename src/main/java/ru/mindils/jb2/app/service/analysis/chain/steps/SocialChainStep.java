package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.ResilientLLMService;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class SocialChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(SocialChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public SocialChainStep(ResilientLLMService llmService,
                         ObjectMapper objectMapper,
                         AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "social";
  }

  @Override
  public String getDescription() {
    return "Социальный анализ: формат работы, домены, социальная значимость";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing social analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0)
              .maxTokens(150)
              .build());

      JsonNode raw = objectMapper.readTree(llmResponse);
      String workMode = asTextOr(raw, "work_mode", "unknown");
      String domains = asTextOr(raw, "domains", "unknown");
      boolean sociallySignificant = asBooleanOr(raw, "socially_significant", false);

      ObjectNode analysisResult = objectMapper.createObjectNode()
          .put("work_mode", workMode)
          .put("domains", domains)
          .put("socially_significant", sociallySignificant);

      analysisResultManager.updateStepResult(currentAnalysis, "social", analysisResult);

      if ("office".equals(workMode) && !sociallySignificant) {
        return ChainStepResult.stop(
            "Только офисная работа в коммерческом проекте - не подходит",
            analysisResult,
            llmResponse
        );
      }

      if (analysisResultManager.shouldStopPipeline(currentAnalysis, "social")) {
        return ChainStepResult.stop(
            "Сработали правила остановки для шага 'social'",
            analysisResult,
            llmResponse
        );
      }

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in social analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed social analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
            Analyze the IT job posting (in Russian) and extract work format and project domain information. Return ONLY flat JSON without additional text.

            Job posting:
            Title: {name}
            Description: {description}
            Key skills: {skills}
            Salary: {salary}
            City: {city}
            Metro: {metro}
            Experience: {experience}
            Schedule: {schedule}
            Employment: {employment}
            Professional roles: {professionalRoles}
            Work format: {workFormat}
            Premium: {premium}
            Internship: {internship}
            
            Analysis criteria:
            
            1. WORK MODE (work_mode) - single value:
               • "remote" - полностью удаленная работа, remote work, дистанционно, из дома
               • "office" - только офис, on-site only, без удаленки
               • "hybrid" - гибрид, hybrid, совмещение офиса и удаленки (без указания конкретных дней)
               • "hybrid_2_3" - гибрид 2 дня офис / 3 дня дома
               • "hybrid_3_2" - гибрид 3 дня офис / 2 дня дома
               • "hybrid_4_1" - гибрид 4 дня офис / 1 день дома
               • "hybrid_flexible" - гибрид с гибким графиком посещения офиса
               • "flexible" - полностью гибкий график (сам выбираешь когда офис/дом)
               • "unknown" - формат не указан или неясен
            
            2. PROJECT DOMAINS (domains) - pipe-separated string of applicable domains:
               • "healthcare" - медицина, здравоохранение, DICOM, медицинские системы, телемедицина, фарма
               • "education" - образование, EdTech, обучение, e-learning, университеты
               • "ecology" - экология, окружающая среда, зеленые технологии
               • "energy" - энергетика, электроэнергия, ЖКХ, utilities
               • "government" - госсектор, госуслуги, муниципальные услуги
               • "social" - социальные услуги, пенсии, пособия, социальная защита
               • "transport" - транспорт, логистика для населения, общественный транспорт
               • "safety" - безопасность, МЧС, пожарная безопасность
               • "employment" - трудоустройство, поиск работы, HR для населения
               • "insurance" - страхование (медицинское, социальное, автострахование)
               • "finance" - финансовые услуги для населения, банки, пенсионные фонды
               • "agriculture" - сельское хозяйство, продовольственная безопасность
               • "science" - научные исследования, R&D
               • "humanitarian" - гуманитарная помощь, благотворительность, НКО
               • "commercial" - коммерческий проект без социальной значимости
               • "unknown" - направление не определено
            
            3. SOCIALLY SIGNIFICANT (socially_significant) - boolean:
               • true - если проект имеет социальную значимость (любой домен кроме "commercial" и "unknown")
               • false - если проект чисто коммерческий или неизвестен
            
            Response format (strict flat JSON):
            {
              "work_mode": string,
              "domains": string,
              "socially_significant": boolean
            }
            """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", truncateText(valueOrEmpty(vacancy.getDescription()), 2000))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{city}", valueOrEmpty(vacancy.getCity()))
        .replace("{metro}", valueOrEmpty(vacancy.getMetro()))
        .replace("{experience}", getJsonFieldName(vacancy.getExperience()))
        .replace("{schedule}", getJsonFieldName(vacancy.getSchedule()))
        .replace("{employment}", getJsonFieldName(vacancy.getEmployment()))
        .replace("{professionalRoles}", valueOrEmpty(vacancy.getProfessionalRolesStr()))
        .replace("{workFormat}", valueOrEmpty(vacancy.getWorkFormatStr()))
        .replace("{premium}", String.valueOf(vacancy.getPremium()))
        .replace("{internship}", String.valueOf(vacancy.getInternship()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }

  private String getJsonFieldName(JsonNode node) {
    return node != null ? node.path("name").asText("") : "";
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }

  private String asTextOr(JsonNode node, String field, String def) {
    JsonNode v = node != null ? node.get(field) : null;
    return v != null && !v.isNull() ? v.asText(def) : def;
  }

  private boolean asBooleanOr(JsonNode node, String field, boolean def) {
    JsonNode v = node != null ? node.get(field) : null;
    return v != null && !v.isNull() ? v.asBoolean(def) : def;
  }
}

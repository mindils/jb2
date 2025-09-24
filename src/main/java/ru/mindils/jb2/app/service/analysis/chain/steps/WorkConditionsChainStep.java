package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class WorkConditionsChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(WorkConditionsChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public WorkConditionsChainStep(ResilientLLMService llmService,
                                 ObjectMapper objectMapper,
                                 AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "workConditions";
  }

  @Override
  public String getDescription() {
    return "Анализ условий работы и требований к релокации";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing work conditions analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0) // Для стабильных результатов
              .maxTokens(200)   // Достаточно для анализа условий
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // Используем AnalysisResultManager для сохранения результата
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in work conditions analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed work conditions analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
            Проанализируй описание IT-вакансии и определи условия работы и требования к релокации.
            Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
            
            Описание вакансии:
            Название: {name}
            Описание: {description}
            Ключевые навыки: {skills}
            
            КРИТЕРИИ АНАЛИЗА:
            
            1. ФОРМАТ РАБОТЫ (workFormat):
            ✅ "remote_global" - удаленка из любой точки мира, without geographical restrictions
            ✅ "remote_restricted" - удаленка из определенных стран/регионов
            ✅ "hybrid_flexible" - гибрид ≤4 дня в месяц в офисе, mostly remote
            ✅ "hybrid_regular" - гибрид 1-2 дня в неделю в офисе
            ✅ "hybrid_frequent" - гибрид 3+ дня в неделю в офисе
            ✅ "office_only" - 100% работа в офисе
            
            2. ТРЕБОВАНИЯ К РЕЛОКАЦИИ (relocationRequired):
            ✅ "none" - не требуется, можно работать из РФ/текущего местоположения
            ✅ "assisted" - помогают с релокацией, visa sponsorship
            ✅ "required_no_help" - требуется переезд, но без помощи компании
            ✅ "mandatory_specific" - обязательная релокация в конкретную страну/город
            
            3. ГЕОГРАФИЧЕСКИЕ ОГРАНИЧЕНИЯ (geoRestrictions):
            ✅ "none" - нет географических ограничений
            ✅ "timezone" - ограничения по часовым поясам
            ✅ "country_list" - работа из определенного списка стран
            ✅ "region_specific" - только из определенного региона (EU, US, etc.)
            
            ❌ НЕ УЧИТЫВАТЬ:
            - Командировки и business trips
            - Временные выезды в офис
            - Корпоративные мероприятия
            - Onboarding в офисе
            
            Формат ответа (строгий JSON):
            {
              "workFormat": "remote_global|remote_restricted|hybrid_flexible|hybrid_regular|hybrid_frequent|office_only",
              "relocationRequired": "none|assisted|required_no_help|mandatory_specific",
              "geoRestrictions": "none|timezone|country_list|region_specific"
            }
            """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2500))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}
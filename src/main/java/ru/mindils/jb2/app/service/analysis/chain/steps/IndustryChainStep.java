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
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class IndustryChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(IndustryChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;
  private final HtmlToMarkdownConverter htmlConverter;

  public IndustryChainStep(ResilientLLMService llmService,
                           ObjectMapper objectMapper,
                           AnalysisResultManager analysisResultManager,
                           HtmlToMarkdownConverter htmlConverter
                           ) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
    this.htmlConverter = htmlConverter;
  }

  @Override
  public String getStepId() {
    return "industry";
  }

  @Override
  public String getDescription() {
    return "Анализ отрасли и социальной значимости";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing industry analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0)
              .maxTokens(300)
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in industry analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed industry analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
            Проанализируй описание IT-вакансии и определи отрасль компании и проекта. 
            Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
            
            Описание вакансии:
            Название: {name}
            Описание: {description}
            Описание(Бренд): {descriptionBranded}
            Ключевые навыки: {skills}
            Зарплата: {salary}
            Компания: {employer}
            Компания (Бренд): {employerBranded}
            Компания индустрия: {employerIndustries}
            
            1. КАТЕГОРИЯ КОМПАНИИ (company_category) - одно значение:
               • "positive" - медицина, образование, экология, наука, космос, социальные проекты, доступность, сельское хозяйство
               • "neutral" - B2B SaaS, госуслуги, логистика, производство, AI инфраструктура, глубокие технологии
               • "problematic" - финансы, страхование, недвижимость, юридические услуги, маркетинг
               • "toxic" - e-commerce, игры, развлечения, реклама, знакомства, микрофинансы, табак/алкоголь, оружие
            
            2. КАТЕГОРИЯ ПРОЕКТА (project_category) - одно значение:
               Если проект описан отдельно - применить ту же логику
               Если не отличается от компании - указать ту же категорию
            
            3. НАПРАВЛЕНИЕ КОМПАНИИ (company_direction) - строка через "|":
               Конкретные отрасли из списка:
               "healthcare|education|ecology|science|space|social_impact|accessibility|agriculture|b2b_saas|govtech|logistics|manufacturing|ai_infrastructure|deeptech|fintech|insurtech|proptech|legaltech|martech|ecommerce|gaming|entertainment|adtech|dating|microcredit|tobacco_alcohol|weapons"
            
            4. НАПРАВЛЕНИЕ ПРОЕКТА (project_direction) - строка через "|":
               Если проект отличается от компании - указать его направления
               Если не отличается - указать пустую строку ""
            
            Формат ответа (строгий JSON):
            {
              "company_category": string,
              "project_category": string,
              "company_direction": string,
              "project_direction": string
            }
            """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", vacancy.getEmployer().getIndustriesStr());
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
}
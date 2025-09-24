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
public class EquipmentChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(EquipmentChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;
  private final HtmlToMarkdownConverter htmlConverter;

  public EquipmentChainStep(ResilientLLMService llmService,
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
    return "equipment";
  }

  @Override
  public String getDescription() {
    return "Анализ предоставляемого оборудования и технических средств";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing equipment analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0)
              .maxTokens(250)
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in equipment analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed equipment analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии и определи технические аспекты, особенно предоставляемое оборудование.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Зарплата: {salary}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        
        КРИТЕРИИ АНАЛИЗА ОБОРУДОВАНИЯ:
        
        ✅ ИЩИ В ТЕКСТЕ:
        - MacBook Pro, Mac, Apple ноутбук → "macbook_pro"
        - Windows ноутбук, ПК, рабочая станция → "windows_laptop"
        - BYOD, "свое оборудование", "собственный ноутбук" → определи уровень компенсации
        - Мониторы, "дополнительный монитор", "два экрана" → "monitors"
        - Периферия, клавиатура, мышь, "оборудование по запросу" → "peripherals"
        - "компенсация", "возмещение расходов" → уровень компенсации
        
        ❌ НЕ ВКЛЮЧАЙ:
        - Программное обеспечение (IDE, ПО)
        - Интернет и связь
        - Офисную мебель
        - Общие упоминания "современное оборудование" без конкретики
        
        УРОВНИ КОМПЕНСАЦИИ BYOD:
        - "full" = полная компенсация, возмещение 100%
        - "partial" = частичная компенсация, доплата
        - "none" = без компенсации
        
        Формат ответа (строгий JSON):
        {
          "equipmentType": "macbook_pro|windows_laptop|byod|not_specified",
          "equipmentProvided": boolean,
          "byodCompensation": "full|partial|none|not_applicable",
          "additionalEquipment": "monitors|peripherals|monitors_peripherals|none",
          "equipmentMentioned": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
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
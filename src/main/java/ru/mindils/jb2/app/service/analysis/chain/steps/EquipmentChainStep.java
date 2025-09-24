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
public class EquipmentChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(EquipmentChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public EquipmentChainStep(ResilientLLMService llmService,
                            ObjectMapper objectMapper,
                            AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
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
              .temperature(0.0) // Для стабильных результатов
              .maxTokens(250)   // Достаточно для анализа оборудования
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // Используем AnalysisResultManager для сохранения результата
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
                Ключевые навыки: {skills}
                
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
                  "additionalEquipment": "monitors|peripherals|monitors|peripherals|none",
                  "equipmentMentioned": boolean
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
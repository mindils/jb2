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
public class CompensationChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(CompensationChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public CompensationChainStep(ResilientLLMService llmService,
                               ObjectMapper objectMapper,
                               AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "compensation";
  }

  @Override
  public String getDescription() {
    return "Анализ компенсации и структуры зарплаты";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing compensation analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0) // Для стабильных результатов
              .maxTokens(250)   // Достаточно для анализа зарплаты
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // Используем AnalysisResultManager для сохранения результата
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in compensation analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed compensation analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
            Проанализируй описание IT-вакансии и определи параметры компенсации и зарплаты.
            Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
            
            Описание вакансии:
            Название: {name}
            Описание: {description}
            Ключевые навыки: {skills}
            
            КРИТЕРИИ АНАЛИЗА:
            
            1. УКАЗАНА ЛИ ЗАРПЛАТА (salarySpecified):
            ✅ true - если есть конкретные цифры, диапазоны, или четкие указания
            ✅ false - если зарплата не указана, "по договоренности", "конкурентная"
            
            2. ДИАПАЗОН БАЗОВОЙ ЗАРПЛАТЫ (salaryRange):
            ✅ "high_400plus" - базовая зарплата ≥400к рублей в месяц
            ✅ "upper_350_400" - базовая зарплата 350-400к рублей в месяц  
            ✅ "middle_300_350" - базовая зарплата 300-350к рублей в месяц
            ✅ "lower_250_300" - базовая зарплата 250-300к рублей в месяц
            ✅ "below_250" - базовая зарплата менее 250к рублей в месяц
            ✅ "not_specified" - зарплата не указана
            
            3. БЕЛАЯ ЗАРПЛАТА (salaryWhite):
            ✅ true - официальное трудоустройство, белая зарплата, ТК РФ
            ✅ false - серая схема, самозанятость без ТК, договор ГПХ
            
            4. ПРЕМИИ И БОНУСЫ (bonusesAvailable):
            ✅ true - упоминаются премии, бонусы, 13-я зарплата, KPI выплаты
            ✅ false - только фиксированная зарплата, нет упоминаний бонусов
            
            5. АКЦИИ И ОПЦИОНЫ (equityOffered):
            ✅ true - stock options, RSU, акции компании, equity compensation
            ✅ false - нет упоминаний акций или опционов
            
            ❌ НЕ УЧИТЫВАТЬ как зарплату:
            - Социальный пакет и льготы
            - ДМС и страховки  
            - Корпоративное обучение
            - Компенсации за оборудование
            - Питание и развлечения
            
            ❌ НЕ СЧИТАТЬ белой зарплатой:
            - Работа как ИП или самозанятый
            - Договоры ГПХ без трудовых гарантий
            - Упоминание "налоги за ваш счет"
            
            Формат ответа (строгий JSON):
            {
              "salarySpecified": boolean,
              "salaryRange": "high_400plus|upper_350_400|middle_300_350|lower_250_300|below_250|not_specified",
              "salaryWhite": boolean,
              "bonusesAvailable": boolean,
              "equityOffered": boolean,
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
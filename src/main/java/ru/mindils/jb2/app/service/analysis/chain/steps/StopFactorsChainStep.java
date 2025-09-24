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
public class StopFactorsChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(StopFactorsChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public StopFactorsChainStep(ResilientLLMService llmService,
                              ObjectMapper objectMapper,
                              AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "stopFactors";
  }

  @Override
  public String getDescription() {
    return "Проверка критических стоп-факторов";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing stop factors analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0) // Критически важно для стабильности
              .maxTokens(300)   // Достаточно для детального анализа
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // Сохраняем результат
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      // Проверяем наличие стоп-факторов
      boolean hasStopFactors = analysisResult.path("stopFactorFound").asBoolean(false);

      if (hasStopFactors) {
        String stopReason = buildStopReason(analysisResult);
        log.warn("Stop factors found for vacancy {}: {}", vacancy.getId(), stopReason);
        return ChainStepResult.stop(stopReason, analysisResult, llmResponse);
      }

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in stop factors analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed stop factors analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
                Проанализируй описание IT-вакансии на наличие критических стоп-факторов.
                Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
                
                Описание вакансии:
                Название: {name}
                Описание: {description}
                Ключевые навыки: {skills}
                
                КРИТЕРИИ СТОП-ФАКТОРОВ:
                
                1. СЕРАЯ/ЧЕРНАЯ ЗАРПЛАТА (graySalary):
                ✅ ИЩИТЕ ПРИЗНАКИ:
                - "зарплата в конверте", "часть зарплаты наличными"
                - "зарплата по договоренности без официального оформления"
                - "работа без трудового договора", "самозанятость обязательна"
                - "серая/черная схема", "минимальная официальная зарплата"
                
                2. 100% ОФИСНАЯ РАБОТА (officeOnly):
                ✅ ИЩИТЕ ПРИЗНАКИ:
                - "только офис", "исключительно офисная работа"
                - "remote/удаленка запрещена", "без возможности работать из дома"
                - "обязательное присутствие в офисе 5 дней"
                - Отсутствие слов: hybrid, remote, гибкий, удаленно, из дома
                ❌ НЕ ВКЛЮЧАЙТЕ если есть:
                - "гибридный формат", "можно удаленно", "home office"
                - "частично удаленно", "по договоренности"
                
                3. ТОКСИЧНАЯ КУЛЬТУРА (toxicCulture):
                ✅ ИЩИТЕ ПРИЗНАКИ:
                - "переработки", "готовность работать сверхурочно"
                - "высокие требования к стрессоустойчивости"
                - "жесткие дедлайны", "аврально", "горящие проекты"
                - "микроменеджмент", "строгий контроль"
                - "высокая текучка", "быстрая замена сотрудников"
                
                4. ЗАПРЕЩЕННЫЕ ДОМЕНЫ (bannedDomain):
                ✅ ИЩИТЕ ПРИЗНАКИ:
                - Gambling: "казино", "ставки", "игорный", "букмекер", "азартные игры"
                - Микрокредитование: "микрозайм", "быстрые кредиты", "займ до зарплаты"
                - Оружие: "военная промышленность", "оборонка", "оружие"
                - Adult: "adult", "18+", "эротический", "порно"
                - Алкоголь/табак: продвижение алкоголя/табака (не общепит)
                - Мошенничество: "пирамида", "сетевой маркетинг", подозрительные схемы
                
                ❌ НЕ ВКЛЮЧАЙТЕ:
                - Банки, финтех (кроме микрозаймов)
                - Рестораны/бары (продажа алкоголя в рамках общепита)
                - IT-компании с разными клиентами
                - Общие фразы без конкретики
                
                ВАЖНО: Если хотя бы один фактор = true, то stopFactorFound = true
                
                Формат ответа (строгий JSON):
                {
                  "graySalary": boolean,
                  "officeOnly": boolean,
                  "toxicCulture": boolean,
                  "bannedDomain": boolean,
                  "stopFactorFound": boolean
                }
                """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2500))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  private String buildStopReason(JsonNode analysisResult) {
    StringBuilder reason = new StringBuilder("Обнаружены стоп-факторы: ");

    if (analysisResult.path("graySalary").asBoolean()) {
      reason.append("серая зарплата, ");
    }
    if (analysisResult.path("officeOnly").asBoolean()) {
      reason.append("только офисная работа, ");
    }
    if (analysisResult.path("toxicCulture").asBoolean()) {
      reason.append("токсичная культура, ");
    }
    if (analysisResult.path("bannedDomain").asBoolean()) {
      reason.append("запрещенная отрасль, ");
    }

    // Удаляем последнюю запятую
    String result = reason.toString();
    if (result.endsWith(", ")) {
      result = result.substring(0, result.length() - 2);
    }

    return result;
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}
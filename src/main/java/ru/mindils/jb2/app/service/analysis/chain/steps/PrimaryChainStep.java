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
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class PrimaryChainStep extends AbstractChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(PrimaryChainStep.class);

  private final ResilientLLMService llmService;

  // Поле для хранения информации о принудительном перезапуске
  private volatile boolean forceReanalyzeFlag = false;

  public PrimaryChainStep(ResilientLLMService llmService,
                          ObjectMapper objectMapper,
                          AnalysisResultManager analysisResultManager) {
    super(objectMapper, analysisResultManager);
    this.llmService = llmService;
  }

  @Override
  public String getStepId() {
    return "primary";
  }

  @Override
  public String getDescription() {
    return "Первичный анализ: определение Java стек";
  }

  @Override
  protected boolean shouldForceReanalyze() {
    return forceReanalyzeFlag;
  }

  @Override
  protected String determineStopReason(JsonNode cachedResult) {
    // Проверяем специфическую для первичного анализа логику остановки
    if (cachedResult != null && !cachedResult.path("java").asBoolean(false)) {
      return "Вакансия не является Java-позицией (кэшированный результат)";
    }
    return "Условия остановки сработали на основе кэшированных данных первичного анализа";
  }

  @Override
  protected ChainStepResult executeNewAnalysis(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing fresh primary analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      // Используем новый resilient сервис вместо прямого вызова ChatClient
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0) // Для более стабильных результатов
              .maxTokens(100)    // Уменьшаем для простого JSON ответа
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      return saveAnalysisResult(currentAnalysis, analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in primary analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed primary analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
            Analyze the IT job posting (in Russian) and determine category matches. Return ONLY JSON without additional text.

            Job posting:
            Title: {name}
            Description: {description}
            Key skills: {skills}
            
            Analysis criteria:
            
            1. JAVA (java: true) - position requires DIRECT Java development:
               ✅ INCLUDE only if candidate will write Java code:
                  • Java SE/EE, Spring (Boot/Framework/Data/Security/Cloud)
                  • Hibernate, JPA, JDBC
                  • Maven, Gradle
                  • Backend/microservices in Java
                  • REST API/GraphQL in Java
                  • JUnit, Mockito, TestNG
                  • Java-specific tools: Kafka (Java), RabbitMQ (Java), Elasticsearch (Java client)
            
               ❌ EXCLUDE (java: false):
                  • Android development (even if Java is mentioned)
                  • Kotlin for Android
                  • JavaScript/TypeScript/Node.js/CoffeeScript (NOT Java!)
                  • If Java is in company stack but position is for Python/Go/C#/PHP/Ruby developer
                  • If Java mentioned as "будет плюсом" / "желательно" / "nice to have" but main stack is different
                  • DevOps/QA positions without Java development
            
            Response format (strict JSON):
            {
              "java": boolean
            }
            """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  /**
   * Установить флаг принудительного перезапуска
   * Этот метод может быть вызван из сервиса перед выполнением анализа
   */
  public void setForceReanalyze(boolean forceReanalyze) {
    this.forceReanalyzeFlag = forceReanalyze;
    log.debug("Set forceReanalyze flag to: {} for PrimaryChainStep", forceReanalyze);
  }
}
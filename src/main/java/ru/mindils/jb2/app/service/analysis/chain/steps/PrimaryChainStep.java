package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class PrimaryChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(PrimaryChainStep.class);
  private static final String LLM_MODEL = "qwen3-30b-a3b-instruct-2507-mlx";

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public PrimaryChainStep(ChatClient chatClient, ObjectMapper objectMapper, AnalysisResultManager analysisResultManager) {
    this.chatClient = chatClient;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "primary";
  }

  @Override
  public String getDescription() {
    return "Первичный анализ: определение Java, Jmix, AI";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing primary analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      String llmResponse = chatClient.prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder().model(LLM_MODEL).build())
          .call()
          .content();

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // ОБНОВЛЕННЫЙ КОД - сохраняем в новую структуру
      analysisResultManager.updateStepResult(currentAnalysis, "primary", analysisResult);

      // Проверяем условие остановки
      if (analysisResultManager.shouldStopPipeline(currentAnalysis, "primary")) {
        return ChainStepResult.stop(
            "Вакансия не является Java-позицией",
            analysisResult,
            llmResponse
        );
      }

      return ChainStepResult.success(analysisResult, llmResponse);

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
            
            2. JMIX (jmix: true) - mentions Jmix platform:
               ✅ INCLUDE:
                  • Jmix, Jmix Studio, Jmix framework
                  • CUBA Platform (Jmix predecessor)
                  • Haulmont Jmix
            
               ❌ EXCLUDE:
                  • Similar names not related to Jmix
            
            3. AI (ai: true) - position requires AI/ML work:
               ✅ INCLUDE:
                  • Искусственный интеллект, машинное обучение, нейронные сети, нейросети
                  • AI, ML, Machine Learning, Deep Learning, Neural Networks
                  • LLM, GPT, ChatGPT, Claude, Gemini, LangChain, YandexGPT, GigaChat
                  • TensorFlow, PyTorch, Keras, scikit-learn, JAX
                  • Data Science with ML focus
                  • NLP, Computer Vision, Reinforcement Learning
                  • AI Engineer, ML Engineer, AI Researcher, ML-инженер
                  • AI solutions development/integration
            
               ❌ EXCLUDE:
                  • Simple analytics without ML
                  • BI without ML components
                  • Data Engineering without ML
            
            Response format (strict JSON):
            {
              "java": boolean,
              "jmix": boolean,
              "ai": boolean
            }
            """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}
package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

@Component
public class PrimaryAnalysisStrategy implements AnalysisStrategy {

  private static final String ANALYSIS_PROMPT = """
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
      
      Analysis rules:
      1. Analyze ALL parts of the job posting comprehensively
      2. Identify PRIMARY requirements, not secondary ones
      3. If technology mentioned as "желательно" / "будет плюсом" / "опционально" - it's NOT a primary requirement
      4. Focus on what employee WILL DO, not company's general stack
      5. For conflicts (e.g., "Java или Python") - set true only if Java is clearly prioritized
      6. Case-insensitive matching for both Russian and English terms
      7. When in doubt, set false
      8. Understand Russian job terminology: "разработчик", "программист", "инженер", "стек", etc.
      
      Response format (strict JSON):
      {
        "java": boolean,
        "jmix": boolean,
        "ai": boolean
      }
      """;

  @Override
  public AnalysisType getAnalysisType() {
    return AnalysisType.PRIMARY;
  }

  @Override
  public String getPrompt(Vacancy vacancy) {
    return ANALYSIS_PROMPT
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  @Override
  public void updateAnalysis(VacancyAnalysis analysis, JsonNode llmResponse) {
    analysis.setJmix(llmResponse.get("jmix").asText());
    analysis.setJava(llmResponse.get("java").asText());
    analysis.setAi(llmResponse.get("ai").asText());
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
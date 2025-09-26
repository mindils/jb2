package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.service.analysis.prompt.PromptGenerator;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class JavaPrimaryPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public JavaPrimaryPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.JAVA_PRIMARY;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Analyze the IT job posting (in Russian) and determine category matches. Return ONLY JSON without additional text.
        
        Job posting:
        Title: {name}
        Description: {description}
        DescriptionBranded: {descriptionBranded}
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
        .replace("{description}", htmlConverter.convertToMarkdown(vacancy.getDescription()))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(vacancy.getBrandedDescription()))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }
}

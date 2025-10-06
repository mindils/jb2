package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
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
        
        1. JAVA (java: true/false) - Определите, будет ли Java использоваться в проекте/позиции.
        
        КЛЮЧЕВОЙ ПРИНЦИП: Если Java упоминается в требованиях или технологическом стеке позиции - это значит, что она ИСПОЛЬЗУЕТСЯ в проекте, и разработчик с высокой вероятностью будет с ней работать.
        
        ✅ ВКЛЮЧАТЬ (java: true) - Java упоминается как технология проекта:
           • Java указана в требованиях к кандидату (любой уровень)
           • "Знание Java" / "Опыт Java" (даже без указания лет)
           • "Знание Java ИЛИ стремление освоить" - Java в стеке проекта
           • Java + другие языки (Node.js, Python, Go) - мультиязычный проект
           • Spring Boot/Framework/Data/Security/Cloud
           • Hibernate, JPA, JDBC, Maven, Gradle
           • Backend/микросервисы на Java
           • REST API/GraphQL на Java
           • Kafka, RabbitMQ, Elasticsearch в Java-проектах
           • "Fullstack" с упоминанием Java в стеке
           • Java в списке Key skills
        
        ❌ ИСКЛЮЧАТЬ (java: false) ТОЛЬКО в явных случаях:
           • "Знание Java будет небольшим плюсом" (явно необязательно)
           • Android/Kotlin mobile разработка
           • Позиция ЯВНО не на Java (чистый frontend/DevOps/QA) БЕЗ упоминания Java
           • JavaScript/TypeScript путаница (НЕ Java!)
        
        ПРАВИЛО: Если Java упомянута в требованиях или стеке - она ИСПОЛЬЗУЕТСЯ в проекте → TRUE
        Исключение только если ЯВНО сказано "необязательно" или это не Java (Android/JavaScript).
        
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

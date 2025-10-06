package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class TechnicalPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public TechnicalPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.TECHNICAL;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй IT-вакансию. Верни JSON про технические требования.
        
        ДАННЫЕ:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Навыки: {skills}
        
        ПРАВИЛО: Анализируй только на основе текста. Если неясно - выбирай "none" или самое подходящее.
        
        === ЧТО ОПРЕДЕЛИТЬ ===
        
        1. role_type (тип роли):
        • "backend" = Backend разработка, API, микросервисы, БД
        • "fullstack" = Frontend + Backend, Full-stack
        • "devops" = DevOps, SRE, Platform Engineer с разработкой
        • "other" = QA, тестировщик, аналитик, PM
        • "none" = непонятно
        
        2. position_level (уровень):
        • "junior" = Junior, 0-2 года
        • "middle" = Middle, 2-5 лет
        • "senior" = Senior, 5+ лет
        • "lead" = Lead, Team Lead
        • "principal" = Principal, Staff Engineer
        • "architect" = Архитектор
        • "none" = не указано
        
        3. stack (технологии через "|"):
        Выбери ВСЕ подходящие и соедини через "|":
        • "spring" = Spring Framework, Spring Boot, Spring Cloud
        • "python" = Python, Django, Flask, FastAPI
        • "microservices" = микросервисы, distributed systems
        • "database" = PostgreSQL, MySQL, MongoDB, Redis
        • "frontend" = React, Vue, Angular, JavaScript, TypeScript
        • "devops" = Docker, Kubernetes, CI/CD, AWS, GCP, Azure
        • "none" = технологии не упомянуты
        
        Примеры:
        - Если есть Spring + PostgreSQL → "spring|database"
        - Если есть React + Node.js → "frontend"
        - Если только описание без технологий → "none"
        
        4. jmix (платформа Jmix):
        • true = упоминается Jmix, CUBA Platform, Haulmont
        • false = не упоминается
        
        5. ai_presence (работа с AI/LLM):
        • "allowed" = можно использовать Copilot, ChatGPT для разработки
        • "project_optional" = есть AI-проекты, участие по желанию
        • "project_required" = работа с LLM/AI обязательна
        • "none" = AI не упоминается
        
        ❌ НЕ выдумывай:
        • Если роль непонятна → "none"
        • Если уровень не указан → "none"
        • Если технологии не названы → "none"
        • Если AI не упомянут → "none"
        
        === ОТВЕТ (только JSON) ===
        {
          "role_type": "backend|fullstack|devops|other|none",
          "position_level": "junior|middle|senior|lead|principal|architect|none",
          "stack": "spring|python|microservices|database|frontend|devops|none",
          "jmix": true|false,
          "ai_presence": "allowed|project_optional|project_required|none"
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}
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
        Проанализируй описание IT-вакансии (на русском языке) и извлеки техническое соответствие. Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        Компания индустрия: {employerIndustries}
        
        Критерии анализа:
        
        1. ТИП РОЛИ (role_type) - единственное значение:
           • "backend" - Backend Developer, Full-stack Developer с акцентом на серверную разработку/микросервисы/API/БД
           • "frontend_plus_backend" - Frontend Developer с backend-задачами, Backend с небольшим Frontend, Full-stack с существенной долей бэка
           • "devops_with_dev" - DevOps/SRE с элементами разработки, Platform Engineer
           • "other" - QA, Manual Tester, Business Analyst, Project Manager, другие не-разработческие роли
        
        2. УРОВЕНЬ ПОЗИЦИИ (position_level) - единственное значение:
           • "junior" - Junior (0-2 года опыта, начальный уровень)
           • "middle" - Middle (2-5 лет опыта)
           • "senior" - Senior (5+ лет опыта, самостоятельная работа)
           • "lead" - Lead/Team Lead (может включать >30% менеджмента)
           • "principal" - Principal/Staff (технический эксперт без менеджмента)
           • "architect" - Architect (проектирование систем)
        
        3. ТЕХНОЛОГИЧЕСКИЙ СТЕК (stack) - строка через "|" из применимых групп:
           • "spring" - Spring Framework, Spring Boot, Spring Cloud, Spring Security, Spring Data и другие Spring-технологии
           • "python" - Python и связанные технологии (Django, Flask, FastAPI, pandas, etc.)
           • "microservices" - микросервисная архитектура, distributed systems, service mesh, event-driven architecture
           • "database" - базы данных PostgreSQL, MySQL, MongoDB, Redis, ClickHouse и другие СУБД
           • "frontend" - фронтенд технологии React, Vue, Angular, JavaScript, TypeScript, HTML/CSS
           • "devops" - инфраструктурные инструменты Docker, Kubernetes, Linux, CI/CD, AWS/GCP/Azure, Jenkins
        
        4. JMIX ПЛАТФОРМА (jmix) - boolean:
           • true - встречается платформа Jmix / CUBA Platform / Haulmont Jmix
           • false - не встречается
        
        5. ИИ / LLM ПРИСУТСТВИЕ (ai_presence) - строка через "|" из применимых групп:
           • "none" - отсутствуют сигналы об ИИ/LLM
           • "allowed_for_dev" - явно разрешено/поощряется использовать LLM/генеративный ИИ в разработке (Copilot/Code Assist/обучающие практики)
           • "llm_project_optional" - компания/команда ведёт проект(ы) с LLM/GenAI, участие возможно при желании/росте, требования к ИИ опциональные
           • "llm_project_required" - работа по LLM/GenAI требуется и опыт обязателен
        
        Формат ответа (строгий плоский JSON):
        {
          "role_type": string,
          "position_level": string,
          "stack": string,
          "jmix": boolean,
          "ai_presence": string
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}
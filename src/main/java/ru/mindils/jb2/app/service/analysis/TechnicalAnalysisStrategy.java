package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

@Component
public class TechnicalAnalysisStrategy implements AnalysisStrategy {

  private final ObjectMapper om;

  public TechnicalAnalysisStrategy(ObjectMapper om) {
    this.om = om;
  }

  private static final String TECHNICAL_ANALYSIS_PROMPT = """
      Проанализируй описание IT-вакансии (на русском языке) и извлеки техническое соответствие. Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
      
      Описание вакансии:
      Название: {name}
      Описание: {description}
      Ключевые навыки: {skills}
      
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
      
      ВКЛЮЧАТЬ: "Jmix", "Jmix Studio", "Jmix framework", "CUBA Platform", "Haulmont Jmix"
      ИСКЛЮЧАТЬ: похожие написания, не относящиеся к фреймворку Jmix
      
      5. ИИ / LLM ПРИСУТСТВИЕ (ai_presence) - единственное значение:
         • "none" - отсутствуют сигналы об ИИ/LLM
         • "allowed_for_dev" - явно разрешено/поощряется использовать LLM/генеративный ИИ в разработке (Copilot/Code Assist/обучающие практики)
         • "llm_project_optional" - компания/команда ведёт проект(ы) с LLM/GenAI, участие возможно при желании/росте, требования к ИИ опциональные
         • "llm_project_required" - работа по LLM/GenAI требуется и опыт обязателен
      
      Правила анализа:
      1. role_type: выбери ОДНО наиболее подходящее значение на основе описанных обязанностей
      2. position_level: оцени уровень по требованиям к опыту и ответственности
      3. stack: может быть несколько групп, разделённых "|" (например, "spring|microservices|database")
      4. Если технологии из группы не упоминаются, не включай эту группу в результат
      5. jmix: ищи точные совпадения с указанными терминами
      6. ai_presence: анализируй контекст использования ИИ/LLM в работе
      7. Если стек не определён, используй пустую строку ""
      8. Анализируй фактическое описание проекта, а не только заявления компании
      9. Поиск терминов регистронезависимый для русских и английских терминов
      
      Формат ответа (строгий плоский JSON):
      {
        "role_type": string,
        "position_level": string,
        "stack": string,
        "jmix": boolean,
        "ai_presence": string
      }
      """;

  @Override
  public AnalysisType getAnalysisType() {
    return AnalysisType.TECHNICAL;
  }

  @Override
  public String getPrompt(Vacancy vacancy) {
    return TECHNICAL_ANALYSIS_PROMPT
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  @Override
  public void updateAnalysis(VacancyAnalysis analysis, JsonNode llmResponse) {
    // Берём существующий extra или создаём пустой объект
    ObjectNode root = (analysis.getExtra() != null && analysis.getExtra().isObject())
        ? ((ObjectNode) analysis.getExtra()).deepCopy()
        : om.createObjectNode();

    root.set("technical", llmResponse);

    analysis.setExtra(root);
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}
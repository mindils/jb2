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
public class TechnicalChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(TechnicalChainStep.class);

  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public TechnicalChainStep(ResilientLLMService llmService,
                            ObjectMapper objectMapper,
                            AnalysisResultManager analysisResultManager) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
  }

  @Override
  public String getStepId() {
    return "technical";
  }

  @Override
  public String getDescription() {
    return "Технический анализ: роль, уровень, стек технологий";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing technical analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      // Используем новый resilient сервис
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0) // Для более стабильных результатов
              .maxTokens(200)    // Увеличиваем для более детального анализа
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // Используем AnalysisResultManager для сохранения результата
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      // Проверяем условие остановки
      if (analysisResultManager.shouldStopPipeline(currentAnalysis, getStepId())) {
        return ChainStepResult.stop(
            "Роль не относится к разработке - не подходит",
            analysisResult,
            llmResponse
        );
      }

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in technical analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed technical analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
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
        
        5. ИИ / LLM ПРИСУТСТВИЕ (ai_presence) - единственное значение:
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
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}
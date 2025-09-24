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
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class BenefitsChainStep implements ChainAnalysisStep {

  private static final Logger log = LoggerFactory.getLogger(BenefitsChainStep.class);

  private final HtmlToMarkdownConverter htmlConverter;
  private final ResilientLLMService llmService;
  private final ObjectMapper objectMapper;
  private final AnalysisResultManager analysisResultManager;

  public BenefitsChainStep(ResilientLLMService llmService,
                           ObjectMapper objectMapper,
                           AnalysisResultManager analysisResultManager,
                           HtmlToMarkdownConverter htmlConverter) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
    this.analysisResultManager = analysisResultManager;
    this.htmlConverter = htmlConverter;
  }

  @Override
  public String getStepId() {
    return "benefits";
  }

  @Override
  public String getDescription() {
    return "Анализ льгот и дополнительных преимуществ";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    log.info("Executing benefits analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);
      String llmResponse = llmService.callLLM(prompt,
          OpenAiChatOptions.builder()
              .temperature(0.0)
              .maxTokens(250)
              .build());

      JsonNode analysisResult = objectMapper.readTree(llmResponse);
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), analysisResult);

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in benefits analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed benefits analysis", e);
    }
  }

  private String buildPrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии и определи наличие льгот и дополнительных преимуществ.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание (бренд): {brandedDescription}
        Ключевые навыки: {skills}
        Описание компании: {employer}
        Описание компании(бренд): {employerBranded}
        
        Критерии анализа (указывай true только при явном упоминании):
        
        ✅ healthInsurance - Медицинские льготы:
        - ДМС (добровольное медицинское страхование)
        - Медицинское страхование/полис
        - Стоматология, стоматологическое страхование
        - Частная медицина, клиники
        - Страховка для семьи/детей
        
        ✅ extendedVacation - Расширенный отпуск:
        - Отпуск 28+ дней (больше стандартных)
        - Дополнительные выходные/отгулы
        - Дни рождения как выходной
        - Дополнительный отпуск за стаж
        
        ✅ wellnessCompensation - Компенсация здоровья и спорта:
        - Компенсация фитнеса/спортзала/бассейна
        - Абонемент в спортклуб
        - Массаж, SPA процедуры
        - Психологическая поддержка/терапия
        - Wellness программы, здоровый образ жизни
        
        ✅ coworkingCompensation - Компенсация рабочего места:
        - Компенсация коворкинга/аренды офиса
        - Оплата интернета дома
        - Компенсация мобильной связи
        - Доплата за электричество при удаленке
        
        ✅ educationCompensation - Компенсация внешнего обучения:
        - Оплата курсов/тренингов/сертификаций
        - Компенсация книг/подписок на обучающие платформы
        - Языковые курсы
        - MBA/дополнительное образование
        - Udemy, Coursera, Pluralsight и подобные
        
        ✅ conferencesBudget - Бюджет на мероприятия:
        - Оплата конференций/форумов
        - Командировки на IT-события
        - Участие в митапах/воркшопах
        - Бюджет на networking события
        
        ✅ internalTraining - Внутреннее обучение:
        - Корпоративные курсы/тренинги
        - Менторство/наставничество
        - Внутренние воркшопы/семинары
        - Обмен знаниями внутри команды
        - Tech talks внутри компании
        
        ✅ paidSickLeave - Оплачиваемые больничные:
        - Больничные сверх государственных
        - 100% оплата больничного
        - Дополнительные дни при болезни
        - Оплата больничного с первого дня
        
        ❌ НЕ учитывать:
        - Общие фразы типа "дружный коллектив"
        - Стандартные условия без конкретики
        - Возможности карьерного роста без льгот
        
        Формат ответа (строгий JSON):
        {
          "healthInsurance": boolean,
          "extendedVacation": boolean,
          "wellnessCompensation": boolean,
          "coworkingCompensation": boolean,
          "educationCompensation": boolean,
          "conferencesBudget": boolean,
          "internalTraining": boolean,
          "paidSickLeave": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{brandedDescription}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }

  private String getJsonFieldName(JsonNode node) {
    return node != null ? node.path("name").asText("") : "";
  }
}

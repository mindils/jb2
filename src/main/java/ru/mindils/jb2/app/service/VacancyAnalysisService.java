package ru.mindils.jb2.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;

import java.util.Optional;


@Service
public class VacancyAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyAnalysisService.class);

  @PersistenceContext
  private EntityManager em;

  private final ChatClient chatClient;
  @org.springframework.beans.factory.annotation.Autowired
  private final DataManager dataManager;
  private final ObjectMapper objectMapper;

  private static final String LLM_MODEL = "qwen3-30b-a3b-instruct-2507-mlx";
  private static final String SOCIAL_ANALYSIS_PROMPT = """
      Analyze the IT job posting (in Russian) and extract work format and project domain information. Return ONLY flat JSON without additional text.
      
      Job posting:
      Title: {name}
      Description: {description}
      Key skills: {skills}
      
      Analysis criteria:
      
      1. WORK MODE (work_mode) - single value:
         • "remote" - полностью удаленная работа, remote work, дистанционно, из дома
         • "office" - только офис, on-site only, без удаленки
         • "hybrid" - гибрид, hybrid, совмещение офиса и удаленки (без указания конкретных дней)
         • "hybrid_2_3" - гибрид 2 дня офис / 3 дня дома
         • "hybrid_3_2" - гибрид 3 дня офис / 2 дня дома
         • "hybrid_4_1" - гибрид 4 дня офис / 1 день дома
         • "hybrid_flexible" - гибрид с гибким графиком посещения офиса
         • "flexible" - полностью гибкий график (сам выбираешь когда офис/дом)
         • "unknown" - формат не указан или неясен
      
      2. PROJECT DOMAINS (domains) - pipe-separated string of applicable domains:
         • "healthcare" - медицина, здравоохранение, DICOM, медицинские системы, телемедицина, фарма
         • "education" - образование, EdTech, обучение, e-learning, университеты
         • "ecology" - экология, окружающая среда, зеленые технологии
         • "energy" - энергетика, электроэнергия, ЖКХ, utilities
         • "government" - госсектор, госуслуги, муниципальные услуги
         • "social" - социальные услуги, пенсии, пособия, социальная защита
         • "transport" - транспорт, логистика для населения, общественный транспорт
         • "safety" - безопасность, МЧС, пожарная безопасность
         • "employment" - трудоустройство, поиск работы, HR для населения
         • "insurance" - страхование (медицинское, социальное, автострахование)
         • "finance" - финансовые услуги для населения, банки, пенсионные фонды
         • "agriculture" - сельское хозяйство, продовольственная безопасность
         • "science" - научные исследования, R&D
         • "humanitarian" - гуманитарная помощь, благотворительность, НКО
         • "commercial" - коммерческий проект без социальной значимости
         • "unknown" - направление не определено
      
      3. SOCIALLY SIGNIFICANT (socially_significant) - boolean:
         • true - если проект имеет социальную значимость (любой домен кроме "commercial" и "unknown")
         • false - если проект чисто коммерческий или неизвестен
      
      Analysis rules:
      1. work_mode: choose ONE most appropriate value based on job description
      2. domains: can be multiple, separated by "|" (e.g., "healthcare|insurance")
      3. If no domains identified, use "unknown"
      4. If project is clearly commercial without social impact, use "commercial"
      5. Order domains by relevance if multiple apply
      6. Set socially_significant based on whether domains include social value
      7. Case-insensitive matching for Russian and English terms
      8. Analyze actual project description, not just company claims
      
      Response format (strict flat JSON):
      {
        "work_mode": string,
        "domains": string,
        "socially_significant": boolean
      }
      """;

  // Промпт-шаблон для анализа
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

  public VacancyAnalysisService(ChatClient chatClient, DataManager dataManager, ObjectMapper objectMapper) {
    this.chatClient = chatClient;
    this.dataManager = dataManager;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public int markProcessingForAllVacancy() {
    String sql = """
          insert into jb2_vacancy_analysis_queue (vacancy_id, type_queue, processing, created_date, last_modified_date)
          select v.id, 'FIRST', true, now(), now()
          from JB2_VACANCY v
        """;

    return em.createNativeQuery(sql).executeUpdate();
  }

  @Transactional
  public VacancyAnalysis analyzeSocialVacancy(String vacancyId) {
    log.info("Starting analyzeSocialVacancy for vacancy: {}", vacancyId);

    try {
      // Загружаем вакансию
      Optional<Vacancy> maybeVacancy = dataManager.load(Vacancy.class)
          .id(vacancyId)
          .optional();

      if (maybeVacancy.isEmpty()) {
        throw new IllegalArgumentException("Vacancy not found: " + vacancyId);
      }

      var vacancy = maybeVacancy.get();

      // Подготавливаем контекст для LLM
      String prompt = SOCIAL_ANALYSIS_PROMPT
          .replace("{name}", vacancy.getName())
          .replace("{description}", truncateText(vacancy.getDescription(), 2000))
          .replace("{skills}", vacancy.getKeySkillsStr());

      String response = chatClient
          .prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder()
              .model(LLM_MODEL)
              .build())
          .call()
          .content();


      // Парсим ответ
      JsonNode analysisJson = objectMapper.readTree(response);

      // Создаем или обновляем анализ
      VacancyAnalysis analysis = dataManager
          .load(VacancyAnalysis.class)
          .id(vacancyId)
          .optional()
          .orElseGet(() -> {
            var a = dataManager.create(VacancyAnalysis.class);
            a.setId(vacancyId);
            a.setVacancy(vacancy);
            return a;
          });

      analysis.setDomains(analysisJson.get("domains").asText());
      analysis.setWorkMode(analysisJson.get("work_mode").asText());
      analysis.setSociallySignificant(analysisJson.get("socially_significant").asText());
//      analysis.setProcessing(false);

      VacancyAnalysis result = dataManager.save(analysis);

      log.info("Successfully analyzed vacancy: {}", vacancyId);

      return result;

    } catch (Exception e) {
      log.error("Error analyzing vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw new RuntimeException("Failed to analyze vacancy: " + vacancyId, e);
    }
  }

  @Transactional
  public VacancyAnalysis analyzeVacancy(Long vacancyQueueId) {
    log.info("Starting analysis for vacancy: {}", vacancyQueueId);

    try {
      VacancyAnalysisQueue vacancyAnalysisQueue = dataManager.load(VacancyAnalysisQueue.class)
          .id(vacancyQueueId)
          .one();

      Vacancy vacancy = vacancyAnalysisQueue.getVacancy();

      if (vacancy == null) {
        throw new IllegalArgumentException("Vacancy not found, vacancyAnalysisQueue " + vacancyAnalysisQueue.getId());
      }

      // Подготавливаем контекст для LLM
      String prompt = ANALYSIS_PROMPT
          .replace("{name}", vacancy.getName())
          .replace("{description}", truncateText(vacancy.getDescription(), 2000))
          .replace("{skills}", vacancy.getKeySkillsStr());

      // Вызываем LLM
//      String response = chatClient.prompt()
//          .user(prompt)
//          .call()
//          .content();

      String response = chatClient
          .prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder()
              .model(LLM_MODEL)
//              .responseFormat(
//                  ResponseFormat.builder()
//                      .type(ResponseFormat.Type.JSON_OBJECT)
//                      .build()
//              )
              .build())
          .call()
          .content();


      // Парсим ответ
      JsonNode analysisJson = objectMapper.readTree(response);

      // Создаем или обновляем анализ
      VacancyAnalysis analysis = dataManager
          .load(VacancyAnalysis.class)
          .id(vacancy.getId())
          .optional()
          .orElseGet(() -> {
            var a = dataManager.create(VacancyAnalysis.class);
            a.setId(vacancy.getId());
            a.setVacancy(vacancy);
            return a;
          });

      analysis.setJmix(analysisJson.get("jmix").asText());
      analysis.setJava(analysisJson.get("java").asText());
      analysis.setAi(analysisJson.get("ai").asText());
//      analysis.setProcessing(false);

      VacancyAnalysis result = dataManager.save(analysis);

      log.info("Successfully analyzed vacancy: {}", vacancy.getId());

      vacancyAnalysisQueue.setProcessing(false);
      dataManager.save(vacancyAnalysisQueue);

      return result;

    } catch (Exception e) {
      log.error("Error analyzing vacancyQueueId {}: {}", vacancyQueueId, e.getMessage(), e);
      throw new RuntimeException("Failed to analyze vacancyQueueId: " + vacancyQueueId, e);
    }
  }


  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
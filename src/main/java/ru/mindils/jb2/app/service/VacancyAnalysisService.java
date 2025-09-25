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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.service.analysis.AnalysisStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VacancyAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyAnalysisService.class);

  @PersistenceContext
  private EntityManager em;

  private final ChatClient chatClient;
  private final DataManager dataManager;
  private final ObjectMapper objectMapper;
  private final Map<AnalysisType, AnalysisStrategy> strategies;

  private static final String LLM_MODEL = "qwen3-30b-a3b-instruct-2507-mlx";

  public VacancyAnalysisService(
      ChatClient chatClient,
      DataManager dataManager,
      ObjectMapper objectMapper,
      List<AnalysisStrategy> strategyList
  ) {
    this.chatClient = chatClient;
    this.dataManager = dataManager;
    this.objectMapper = objectMapper;
    // Создаем Map стратегий для быстрого доступа по типу анализа
    this.strategies = strategyList.stream()
        .collect(Collectors.toUnmodifiableMap(AnalysisStrategy::getAnalysisType, Function.identity()));
  }

  /**
   * Универсальный метод для анализа вакансии по определенному типу.
   */
  @Transactional
  public VacancyAnalysis analyze(String vacancyId, AnalysisType type) {
    log.info("Starting analysis for vacancyId: {}, type: {}", vacancyId, type);

    AnalysisStrategy strategy = Optional.ofNullable(strategies.get(type))
        .orElseThrow(() -> new IllegalArgumentException("No strategy found for analysis type: " + type));

    try {
      Vacancy vacancy = dataManager.load(Vacancy.class).id(vacancyId).optional()
          .orElseThrow(() -> new IllegalStateException("Vacancy not found in queue item: " + vacancyId));

      String prompt = strategy.getPrompt(vacancy);

      String response = chatClient.prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder().model(LLM_MODEL).build())
          .call()
          .content();

      JsonNode analysisJson = objectMapper.readTree(response);

      VacancyAnalysis analysis = dataManager.load(VacancyAnalysis.class)
          .id(vacancy.getId())
          .optional()
          .orElseGet(() -> createNewAnalysis(vacancy));

      strategy.updateAnalysis(analysis, analysisJson);
      VacancyAnalysis savedAnalysis = dataManager.save(analysis);
      log.info("Successfully analyzed vacancy: {}", vacancy.getId());

      queueItem.setProcessing(false);
      dataManager.save(queueItem);

      return savedAnalysis;
    } catch (Exception e) {
      log.error("Error analyzing vacancyQueueId {}: {}", vacancyQueueId, e.getMessage(), e);
      throw new RuntimeException("Failed to analyze vacancyQueueId: " + vacancyQueueId, e);
    }
  }

  private VacancyAnalysis createNewAnalysis(Vacancy vacancy) {
    VacancyAnalysis newAnalysis = dataManager.create(VacancyAnalysis.class);
    newAnalysis.setId(vacancy.getId());
    newAnalysis.setVacancy(vacancy);
    return newAnalysis;
  }

  /**
   * Добавляет все вакансии в очередь на обработку для указанного типа анализа.
   */
  @Transactional
  public int markProcessingForJavaVacancy(AnalysisType type) {
    String sql = """
          insert into jb2_vacancy_analysis_queue (vacancy_id, type_queue, processing, created_date, last_modified_date)
          select v.id, ?1, true, now(), now()
          from jb2_vacancy_analysis v
         where v.java = 'true' and
              v.id not in (select q.vacancy_id from jb2_vacancy_analysis_queue q where q.processing = true and q.type_queue = 'SOCIAL')
        """;
    return em.createNativeQuery(sql)
        .setParameter(1, type.getId())
        .executeUpdate();
  }

  /**
   * Добавляет все вакансии в очередь на обработку для указанного типа анализа.
   */
  @Transactional
  public int markProcessingForAllVacancy(AnalysisType type) {
    String sql = """
          insert into jb2_vacancy_analysis_queue (vacancy_id, type_queue, processing, created_date, last_modified_date)
          select v.id, ?1, true, now(), now()
          from JB2_VACANCY v
          where v.id not in (select q.vacancy_id from jb2_vacancy_analysis_queue q where q.type_queue = ?1)
        """;
    return em.createNativeQuery(sql)
        .setParameter(1, type.getId())
        .executeUpdate();
  }

  /**
   * Добавляет в очередь ВСЕ вакансии, для которых ЕЩЁ НЕТ записи в jb2_vacancy_analysis.
   * Дубликаты в очереди указанного типа не создаются.
   * Пример запуска: enqueueNotAnalyzed(AnalysisType.PRIMARY)
   */
  @Transactional
  public int enqueueNotAnalyzed() {
    String sql = """
         insert into jb2_vacancy_analysis_queue (vacancy_id, type_queue, processing, created_date, last_modified_date)
         select v.id, ?1, true, now(), now()
         from jb2_vacancy v
         where not exists (
          select 1 from jb2_vacancy_analysis a
           where a.id = v.id
        )
        and v.id not in (
           select q.vacancy_id from jb2_vacancy_analysis_queue q
           where q.type_queue = ?1
        )
        """;
    return em.createNativeQuery(sql)
        .setParameter(1, AnalysisType.PRIMARY.getId())
        .executeUpdate();
  }
}
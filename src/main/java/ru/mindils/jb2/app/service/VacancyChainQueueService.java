package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.flowui.model.CollectionLoader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyChainAnalysisQueue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для управления очередью цепочки анализа
 */
@Service
public class VacancyChainQueueService {

  private final DataManager dataManager;
  private final FetchPlans fetchPlans;
  private final Metadata metadata;

  @PersistenceContext
  private EntityManager em;

  public VacancyChainQueueService(DataManager dataManager, FetchPlans fetchPlans, Metadata metadata) {
    this.dataManager = dataManager;
    this.fetchPlans = fetchPlans;
    this.metadata = metadata;
  }

  /**
   * Добавить все вакансии в очередь для определенного типа цепочки анализа
   */
  @Transactional
  public int enqueueAllForChainAnalysis(ChainAnalysisType chainType, int batchSize) {
    return enqueueAllForChainAnalysis(chainType, batchSize, null);
  }

  /**
   * Добавить вакансии из лоадера в очередь для цепочки анализа
   */
  @Transactional
  public int enqueueFromLoader(CollectionLoader<Vacancy> loader, ChainAnalysisType chainType, int batchSize) {
    LoadContext<Vacancy> baseCtx = loader.createLoadContext();
    baseCtx.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());
    return enqueueForChainAnalysis(baseCtx, chainType, batchSize);
  }

  /**
   * Добавить вакансии в очередь с дополнительным условием
   */
  @Transactional
  public int enqueueAllForChainAnalysis(ChainAnalysisType chainType, int batchSize, String additionalCondition) {
    String jpql = "select e from jb2_Vacancy e";
    if (additionalCondition != null && !additionalCondition.trim().isEmpty()) {
      jpql += " where " + additionalCondition;
    }

    MetaClass vacancyMetaClass = metadata.getClass(Vacancy.class);
    LoadContext<Vacancy> baseCtx = new LoadContext<>(vacancyMetaClass);

    LoadContext.Query query = new LoadContext.Query(jpql);
    baseCtx.setQuery(query);
    baseCtx.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());

    return enqueueForChainAnalysis(baseCtx, chainType, batchSize);
  }

  /**
   * Добавить только Java-вакансии в очередь
   */
  @Transactional
  public int enqueueJavaVacanciesForChainAnalysis(ChainAnalysisType chainType, int batchSize) {
    return enqueueAllForChainAnalysis(
        chainType,
        batchSize,
        "exists (select 1 from jb2_VacancyAnalysis a where a.id = e.id and a.java = 'true')"
    );
  }

  @Transactional
  public int enqueueNotAnalyzedVacanciesNativeSql(ChainAnalysisType chainType) {
    var checkJsonField = "primary";
    if (chainType == ChainAnalysisType.FULL_ANALYSIS) {
      checkJsonField = "technical";
    }

    return enqueueNotAnalyzedVacanciesNativeSql(chainType, checkJsonField);
  }

  @Transactional
  public int enqueueNotFullAnalyzedVacanciesNativeSql() {
    var sql = """
        INSERT INTO jb2_vacancy_chain_analysis_queue
            (vacancy_id, chain_type, processing, success, error_message, priority,
             created_date, last_modified_date)
        SELECT
            v.id,
            ?1::varchar            AS chain_type, 
            true                   AS processing,
            NULL                   AS success,
            NULL                   AS error_message,
            1                      AS priority,
            NOW()                  AS created_date,
            NOW()                  AS last_modified_date
        FROM jb2_vacancy v
        LEFT JOIN jb2_vacancy_analysis a ON a.id = v.id
        WHERE NOT EXISTS (
                  SELECT 1
                  FROM jb2_vacancy_chain_analysis_queue q
                  WHERE q.vacancy_id = v.id
                    AND q.chain_type = ?1
                    AND q.processing = true
              )
          AND (
                a.step_results IS NULL
                OR NOT jsonb_exists(a.step_results, ?2)
              )
         AND (
            (a.step_results -> 'primary' ->> 'java')::boolean = true or NOT jsonb_exists(a.step_results, 'primary') -- Проверим что сделан первичный анализ или его нет
          );
        """;

    return em.createNativeQuery(sql)
        .setParameter(1, ChainAnalysisType.FULL_ANALYSIS)
        .setParameter(2, "stopFactors")
        .executeUpdate();
  }

  /**
   * Добавить вакансии для первичного анализа
   */
  @Transactional
  public int enqueueNotAnalyzedVacanciesNativeSql(ChainAnalysisType chainType, String checkJsonField) {
    var sql = """
        INSERT INTO jb2_vacancy_chain_analysis_queue
            (vacancy_id, chain_type, processing, success, error_message, priority,
             created_date, last_modified_date)
        SELECT
            v.id,
            ?1::varchar            AS chain_type,            -- явный cast для SELECT-части
            true                   AS processing,
            NULL                   AS success,
            NULL                   AS error_message,
            1                      AS priority,
            NOW()                  AS created_date,
            NOW()                  AS last_modified_date
        FROM jb2_vacancy v
        LEFT JOIN jb2_vacancy_analysis a ON a.id = v.id
        WHERE NOT EXISTS (
                  SELECT 1
                  FROM jb2_vacancy_chain_analysis_queue q
                  WHERE q.vacancy_id = v.id
                    AND q.chain_type = ?1
                    AND q.processing = true
              )
          AND (
                a.step_results IS NULL
                OR NOT jsonb_exists(a.step_results, ?2)
              )
         AND (
            (a.step_results -> 'primary' ->> 'java')::boolean = true or NOT jsonb_exists(a.step_results, 'primary')
          );
        """;

    return em.createNativeQuery(sql)
        .setParameter(1, chainType.getId())
        .setParameter(2, checkJsonField)
        .executeUpdate();
  }

  /**
   * Добавить неанализированные вакансии в очередь
   */
  @Transactional
  public int enqueueNotAnalyzedVacancies(ChainAnalysisType chainType, int batchSize) {
    return enqueueAllForChainAnalysis(
        chainType,
        batchSize,
        "not exists (select 1 from jb2_VacancyAnalysis a where a.id = e.id)"
    );
  }

  /**
   * Добавить вакансии с уже выполненным первичным анализом для социально-технического анализа
   */
  @Transactional
  public int enqueueAnalyzedVacanciesForSocialTechnical(ChainAnalysisType chainType, int batchSize) {
    return enqueueAllForChainAnalysis(
        chainType,
        batchSize,
        "exists (select 1 from jb2_VacancyAnalysis a where a.id = e.id and a.stepResults is not null)"
    );
  }

  private int enqueueForChainAnalysis(LoadContext<Vacancy> baseCtx, ChainAnalysisType chainType, int batchSize) {
    int totalEnqueued = 0;
    int first = 0;

    while (true) {
      @SuppressWarnings("unchecked")
      LoadContext<Vacancy> pageCtx = (LoadContext<Vacancy>) baseCtx.copy();
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<Vacancy> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) break;

      List<Object> vacancyIds = page.stream()
          .map(EntityValues::getId)
          .collect(Collectors.toList());

      // Существующие записи в очереди для этого типа цепочки
      Set<Object> existed = findExistingChainQueuedIds(vacancyIds, chainType.getId());

      SaveContext saveCtx = new SaveContext();
      for (Vacancy v : page) {
        Object id = EntityValues.getId(v);
        if (existed.contains(id)) continue;

        VacancyChainAnalysisQueue q = dataManager.create(VacancyChainAnalysisQueue.class);
        q.setVacancy(dataManager.getReference(Vacancy.class, id));
        q.setChainType(chainType);
        q.setProcessing(Boolean.TRUE);
        saveCtx.saving(q);
      }

      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        dataManager.save(saveCtx);
        totalEnqueued += saveCtx.getEntitiesToSave().size();
      }

      first += page.size();
    }

    return totalEnqueued;
  }

  private Set<Object> findExistingChainQueuedIds(List<Object> vacancyIds, String chainType) {
    if (vacancyIds.isEmpty()) return Set.of();

    ValueLoadContext vlc = ValueLoadContext.create()
        .setQuery(new ValueLoadContext.Query(
            "select q.vacancy.id as vacancyId " +
                "from jb2_VacancyChainAnalysisQueue q " +
                "where q.chainType = :chainType and q.vacancy.id in :ids"))
        .addProperty("vacancyId");
    vlc.getQuery().setParameter("chainType", chainType);
    vlc.getQuery().setParameter("ids", vacancyIds);

    List<KeyValueEntity> rows = dataManager.loadValues(vlc);
    return rows.stream()
        .map(kv -> kv.getValue("vacancyId"))
        .collect(Collectors.toSet());
  }

  /**
   * Получить количество вакансий в очереди для определенного типа цепочки
   */
  public int getQueueCount(ChainAnalysisType chainType) {
    return dataManager.loadValue(
            "select count(e) from jb2_VacancyChainAnalysisQueue e " +
                "where e.chainType = :chainType and e.processing = true",
            Integer.class)
        .parameter("chainType", chainType)
        .one();
  }

  /**
   * Очистить очередь для определенного типа цепочки
   */
  @Transactional
  public int clearQueue(ChainAnalysisType chainType) {
    // ИСПРАВЛЕНО: Сначала получаем список, считаем размер, потом удаляем
    List<VacancyChainAnalysisQueue> queueItems = dataManager.load(VacancyChainAnalysisQueue.class)
        .query("select e from jb2_VacancyChainAnalysisQueue e where e.chainType = :chainType")
        .parameter("chainType", chainType)
        .list();

    int size = queueItems.size();

    if (!queueItems.isEmpty()) {
      dataManager.remove(queueItems);
    }

    return size;
  }

  /**
   * Получить общее количество элементов во всех очередях
   */
  public int getTotalQueueCount() {
    return dataManager.loadValue(
            "select count(e) from jb2_VacancyChainAnalysisQueue e where e.processing = true",
            Integer.class)
        .one();
  }

  /**
   * Получить статистику по очередям
   */
  public QueueStats getQueueStats() {
    int fullAnalysis = getQueueCount(ChainAnalysisType.FULL_ANALYSIS);
    int primaryOnly = getQueueCount(ChainAnalysisType.PRIMARY_ONLY);
    int socialTechnical = getQueueCount(ChainAnalysisType.SOCIAL_TECHNICAL);
    int total = getTotalQueueCount();

    return new QueueStats(fullAnalysis, primaryOnly, socialTechnical, total);
  }

  /**
   * Статистика очередей
   */
  public static record QueueStats(
      int fullAnalysisCount,
      int primaryOnlyCount,
      int socialTechnicalCount,
      int totalCount
  ) {
    public String getSummary() {
      return String.format("Всего: %d (Полный: %d, Первичный: %d, Соц+Тех: %d)",
          totalCount, fullAnalysisCount, primaryOnlyCount, socialTechnicalCount);
    }
  }

  /**
   * Пометить элемент очереди как обработанный
   */
  @Transactional
  public void markProcessed(String vacancyId, ChainAnalysisType chainType, boolean success, String errorMessage) {
    dataManager.load(VacancyChainAnalysisQueue.class)
        .query("select e from jb2_VacancyChainAnalysisQueue e " +
            "where e.vacancy.id = :vacancyId and e.chainType = :chainType and e.processing = true")
        .parameter("vacancyId", vacancyId)
        .parameter("chainType", chainType)
        .optional()
        .ifPresent(queue -> {
          queue.setProcessing(false);
          queue.setSuccess(success);
          queue.setErrorMessage(errorMessage);
          dataManager.save(queue);
        });
  }

  /**
   * Получить следующий элемент из очереди для обработки
   */
  public String getNextVacancyFromQueue(ChainAnalysisType chainType) {
    return dataManager.load(VacancyChainAnalysisQueue.class)
        .query("select e from jb2_VacancyChainAnalysisQueue e " +
            "where e.processing = true and e.chainType = :chainType " +
            "order by e.priority desc, e.id")
        .parameter("chainType", chainType)
        .maxResults(1)
        .optional()
        .map(queue -> queue.getVacancy().getId())
        .orElse(null);
  }
}
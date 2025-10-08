package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.model.CollectionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.TaskQueueStats;
import ru.mindils.jb2.app.entity.GenericTaskQueue;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.entity.VVacancySearch;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.repository.GenericTaskQueueRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для работы с очередью задач
 * 1. Закинуть все вакансии для первичного анализа
 * 2. Закинуть все вакансии для full анализа
 * 3. Получить статистику по задачам
 */
@Service
public class GenericTaskQueueService {
  private static final Logger log = LoggerFactory.getLogger(GenericTaskQueueService.class);

  private final GenericTaskQueueRepository genericTaskQueueRepository;
  private final FetchPlans fetchPlans;
  private final DataManager dataManager;

  public GenericTaskQueueService(GenericTaskQueueRepository genericTaskQueueRepository, FetchPlans fetchPlans, DataManager dataManager) {
    this.genericTaskQueueRepository = genericTaskQueueRepository;
    this.fetchPlans = fetchPlans;
    this.dataManager = dataManager;
  }

  /**
   * Поставить в очередь все вакансии для первичного анализа
   * Добавляет только те вакансии, которые еще не анализировались
   *
   * @return количество добавленных записей
   */
  @Transactional
  public int enqueueFirstLlmAnalysis() {
    log.info("Starting to enqueue vacancies for LLM first analysis");
    int count = genericTaskQueueRepository.enqueueForLlmAnalyzed(GenericTaskQueueType.LLM_FIRST, "JAVA_PRIMARY");
    log.info("Enqueued {} vacancies for first LLM analysis", count);
    return count;
  }

  /**
   * Добавить вакансии из loader в очередь задач
   *
   * @param loader    CollectionLoader с вакансиями
   * @param taskType  тип задачи
   * @param batchSize размер батча для обработки
   * @return количество добавленных задач
   */
  @Transactional
  public int enqueueFromLoader(CollectionLoader<Vacancy> loader, GenericTaskQueueType taskType, int batchSize) {
    log.info("Starting to enqueue vacancies from loader for task type: {}", taskType);
    LoadContext<Vacancy> baseCtx = loader.createLoadContext();
    baseCtx.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());
    return enqueueVacancies(baseCtx, taskType, batchSize);
  }

  /**
   * Добавить вакансии из VVacancySearch loader в очередь задач
   * ВАЖНО: добавляет только вакансии, соответствующие примененным фильтрам
   *
   * @param loader    CollectionLoader с VVacancySearch
   * @param taskType  тип задачи
   * @param batchSize размер батча для обработки
   * @return количество добавленных задач
   */
  @Transactional
  public int enqueueFromVViewLoader(CollectionLoader<VVacancySearch> loader, GenericTaskQueueType taskType, int batchSize) {
    log.info("Starting to enqueue vacancies from VVacancySearch loader for task type: {}", taskType);

    // Создаем базовый контекст с учетом всех фильтров из loader
    LoadContext<VVacancySearch> baseCtx = loader.createLoadContext();

    // Модифицируем только FetchPlan, сохраняя все условия запроса
    baseCtx.setFetchPlan(fetchPlans.builder(VVacancySearch.class).add("id").build());

    // Логируем количество записей для обработки
    LoadContext<VVacancySearch> countCtx = (LoadContext<VVacancySearch>) baseCtx.copy();
    long totalCount = dataManager.getCount(countCtx);
    log.info("Total VVacancySearch records matching filters: {}", totalCount);

    return enqueueVacanciesFromView(baseCtx, taskType, batchSize);
  }

  /**
   * Внутренний метод для добавления вакансий в очередь из Vacancy
   */
  private int enqueueVacancies(LoadContext<Vacancy> baseCtx, GenericTaskQueueType taskType, int batchSize) {
    int totalEnqueued = 0;
    int first = 0;
    final String entityName = "jb2_Vacancy";

    while (true) {
      // Копируем контекст для текущей страницы
      @SuppressWarnings("unchecked")
      LoadContext<Vacancy> pageCtx = (LoadContext<Vacancy>) baseCtx.copy();
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<Vacancy> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) {
        break;
      }

      log.debug("Processing batch starting from {}, size: {}", first, page.size());

      // Собираем ID вакансий с явным приведением типа
      List<String> vacancyIds = page.stream()
          .map(v -> (String) EntityValues.getId(v))
          .collect(Collectors.toList());

      // Находим уже существующие записи в очереди
      Set<String> existingIds = findExistingTaskQueueIds(vacancyIds, taskType);

      // Создаем новые записи
      SaveContext saveCtx = new SaveContext();
      for (Vacancy vacancy : page) {
        String vacancyId = (String) EntityValues.getId(vacancy);

        // Пропускаем, если уже в очереди
        if (existingIds.contains(vacancyId)) {
          log.debug("Vacancy {} already in queue for task type {}, skipping", vacancyId, taskType);
          continue;
        }

        GenericTaskQueue task = dataManager.create(GenericTaskQueue.class);
        task.setEntityName(entityName);
        task.setEntityId(vacancyId);
        task.setTaskType(taskType.getId());
        task.setStatus(GenericTaskQueueStatus.NEW);
        task.setPriority(1);
        saveCtx.saving(task);
      }

      // Сохраняем батч
      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        dataManager.save(saveCtx);
        int batchEnqueued = saveCtx.getEntitiesToSave().size();
        totalEnqueued += batchEnqueued;
        log.debug("Enqueued {} vacancies in current batch", batchEnqueued);
      }

      first += page.size();
    }

    log.info("Finished enqueueing vacancies. Total enqueued: {}", totalEnqueued);
    return totalEnqueued;
  }

  /**
   * Внутренний метод для добавления вакансий в очередь из VVacancySearch
   * Сохраняет все условия фильтрации из loader
   */
  private int enqueueVacanciesFromView(LoadContext<VVacancySearch> baseCtx, GenericTaskQueueType taskType, int batchSize) {
    int totalEnqueued = 0;
    int first = 0;
    final String entityName = "jb2_Vacancy";

    // Посчитаем общее количество для обработки
    LoadContext<VVacancySearch> countCtx = (LoadContext<VVacancySearch>) baseCtx.copy();
    long totalCount = dataManager.getCount(countCtx);

    while (true) {
      // Копируем контекст для текущей страницы с СОХРАНЕНИЕМ всех фильтров
      @SuppressWarnings("unchecked")
      LoadContext<VVacancySearch> pageCtx = (LoadContext<VVacancySearch>) baseCtx.copy();

      // Применяем пагинацию ПОВЕРХ фильтров
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<VVacancySearch> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) {
        break;
      }

      log.info("Processing VVacancySearch batch {}-{} of {} for task type: {}",
          first + 1,
          Math.min(first + page.size(), totalCount),
          totalCount,
          taskType);

      // Собираем ID вакансий
      List<String> vacancyIds = page.stream()
          .map(VVacancySearch::getId)
          .collect(Collectors.toList());

      log.debug("Batch vacancy IDs: {}", vacancyIds);

      // Находим уже существующие записи в очереди
      Set<String> existingIds = findExistingTaskQueueIds(vacancyIds, taskType);

      // Создаем новые записи
      SaveContext saveCtx = new SaveContext();
      for (VVacancySearch vVacancySearch : page) {
        String vacancyId = vVacancySearch.getId();

        // Пропускаем, если уже в очереди
        if (existingIds.contains(vacancyId)) {
          log.debug("Vacancy {} already in queue for task type {}, skipping", vacancyId, taskType);
          continue;
        }

        GenericTaskQueue task = dataManager.create(GenericTaskQueue.class);
        task.setEntityName(entityName);
        task.setEntityId(vacancyId);
        task.setTaskType(taskType.getId());
        task.setStatus(GenericTaskQueueStatus.NEW);
        task.setPriority(1);
        saveCtx.saving(task);
      }

      // Сохраняем батч
      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        dataManager.save(saveCtx);
        int batchEnqueued = saveCtx.getEntitiesToSave().size();
        totalEnqueued += batchEnqueued;
        log.info("Enqueued {} vacancies from VVacancySearch in current batch (progress: {}/{})",
            batchEnqueued, first + page.size(), totalCount);
      }

      first += page.size();
    }

    log.info("Finished enqueueing vacancies from VVacancySearch. Total enqueued: {} out of {} filtered",
        totalEnqueued, totalCount);
    return totalEnqueued;
  }

  /**
   * Найти существующие записи в очереди для указанных вакансий и типа задачи
   */
  private Set<String> findExistingTaskQueueIds(List<String> vacancyIds, GenericTaskQueueType taskType) {
    if (vacancyIds.isEmpty()) {
      return Set.of();
    }

    ValueLoadContext vlc = ValueLoadContext.create()
        .setQuery(new ValueLoadContext.Query(
            "select q.entityId as vacancyId " +
                "from jb2_GenericTaskQueue q " +
                "where q.taskType = :taskType " +
                "and q.entityName = :entityName " +
                "and q.entityId in :ids " +
                "and q.status in :activeStatuses"))
        .addProperty("vacancyId");

    vlc.getQuery().setParameter("taskType", taskType.getId());
    vlc.getQuery().setParameter("entityName", "jb2_Vacancy");
    vlc.getQuery().setParameter("ids", vacancyIds);
    vlc.getQuery().setParameter("activeStatuses", List.of(
        GenericTaskQueueStatus.NEW.getId(),
        GenericTaskQueueStatus.PROCESSING.getId()
    ));

    List<KeyValueEntity> rows = dataManager.loadValues(vlc);
    return rows.stream()
        .map(kv -> kv.<String>getValue("vacancyId"))
        .collect(Collectors.toSet());
  }

  // ============ ПОЛНЫЙ АНАЛИЗ ============

  /**
   * Поставить в очередь все вакансии для полного анализа
   * Добавляет только те вакансии, у которых отсутствует хотя бы один тип анализа
   * или которые не находятся в очереди на обработку
   *
   * @return количество добавленных записей
   */
  @Transactional
  public int enqueueFullLlmAnalysis() {
    log.info("Starting to enqueue vacancies for LLM full analysis");
    int count = genericTaskQueueRepository.enqueueForLlmFullAnalysis(GenericTaskQueueType.LLM_FULL);
    log.info("Enqueued {} vacancies for full LLM analysis", count);
    return count;
  }

  // ============ ОБЩИЕ МЕТОДЫ ============

  /**
   * Получить количество задач по типу и статусу PROCESSING (для обратной совместимости)
   */
  @Transactional(readOnly = true)
  public Integer getCountByType(GenericTaskQueueType queueType) {
    return genericTaskQueueRepository.getCountByType(queueType);
  }

  /**
   * Получить количество задач по типу и статусу
   */
  @Transactional(readOnly = true)
  public Integer getCountByTypeAndStatus(GenericTaskQueueType queueType, GenericTaskQueueStatus status) {
    return genericTaskQueueRepository.getCountByTypeAndStatus(queueType, status);
  }

  /**
   * Получить полную статистику по типу задач
   */
  @Transactional(readOnly = true)
  public TaskQueueStats getStatsForTaskType(GenericTaskQueueType queueType) {
    return genericTaskQueueRepository.getStatsForTaskType(queueType);
  }

  // ============ УДОБНЫЕ МЕТОДЫ ДЛЯ ПЕРВИЧНОГО АНАЛИЗА ============

  @Transactional(readOnly = true)
  public Integer getNewLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.NEW);
  }

  @Transactional(readOnly = true)
  public Integer getProcessingLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.PROCESSING);
  }

  @Transactional(readOnly = true)
  public Integer getCompletedLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.COMPLETED);
  }

  @Transactional(readOnly = true)
  public Integer getFailedLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.FAILED);
  }

  @Transactional(readOnly = true)
  public TaskQueueStats getLlmFirstAnalysisStats() {
    return getStatsForTaskType(GenericTaskQueueType.LLM_FIRST);
  }

  // ============ УДОБНЫЕ МЕТОДЫ ДЛЯ ПОЛНОГО АНАЛИЗА ============

  @Transactional(readOnly = true)
  public Integer getNewLlmFullTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FULL, GenericTaskQueueStatus.NEW);
  }

  @Transactional(readOnly = true)
  public Integer getProcessingLlmFullTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FULL, GenericTaskQueueStatus.PROCESSING);
  }

  @Transactional(readOnly = true)
  public Integer getCompletedLlmFullTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FULL, GenericTaskQueueStatus.COMPLETED);
  }

  @Transactional(readOnly = true)
  public Integer getFailedLlmFullTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FULL, GenericTaskQueueStatus.FAILED);
  }

  @Transactional(readOnly = true)
  public TaskQueueStats getLlmFullAnalysisStats() {
    return getStatsForTaskType(GenericTaskQueueType.LLM_FULL);
  }

  // ============ УТИЛИТАРНЫЕ МЕТОДЫ ============

  /**
   * Сбросить "зависшие" задачи в статусе PROCESSING на FAILED
   */
  @Transactional
  public int resetStuckProcessingTasks(GenericTaskQueueType queueType, int olderThanMinutes) {
    log.info("Resetting stuck processing tasks for type {} older than {} minutes", queueType, olderThanMinutes);
    int count = genericTaskQueueRepository.resetStuckProcessingTasks(queueType, olderThanMinutes);
    log.info("Reset {} stuck processing tasks", count);
    return count;
  }

  /**
   * Перезапустить все неудачные задачи - меняет статус с FAILED на NEW
   */
  @Transactional
  public int retryFailedTasks(GenericTaskQueueType queueType) {
    log.info("Retrying failed tasks for type {}", queueType);
    int count = genericTaskQueueRepository.retryFailedTasks(queueType);
    log.info("Retried {} failed tasks", count);
    return count;
  }

  // ============ СПЕЦИФИЧНЫЕ УТИЛИТАРНЫЕ МЕТОДЫ ============

  @Transactional
  public int retryFailedLlmFirstTasks() {
    return retryFailedTasks(GenericTaskQueueType.LLM_FIRST);
  }

  @Transactional
  public int retryFailedLlmFullTasks() {
    return retryFailedTasks(GenericTaskQueueType.LLM_FULL);
  }

  @Transactional
  public int resetStuckLlmFirstTasks() {
    return resetStuckProcessingTasks(GenericTaskQueueType.LLM_FIRST, 30);
  }

  @Transactional
  public int resetStuckLlmFullTasks() {
    return resetStuckProcessingTasks(GenericTaskQueueType.LLM_FULL, 60); // Больше времени для full анализа
  }

  // ============ КОМБИНИРОВАННЫЕ МЕТОДЫ ============

  /**
   * Добавить в очередь все типы анализа
   */
  @Transactional
  public void enqueueAllAnalysis() {
    int firstCount = enqueueFirstLlmAnalysis();
    int fullCount = enqueueFullLlmAnalysis();
    log.info("Total enqueued: {} first analysis, {} full analysis", firstCount, fullCount);
  }

  /**
   * Получить сводную статистику по всем типам анализа
   */
  @Transactional(readOnly = true)
  public String getAllAnalysisStatsSummary() {
    TaskQueueStats firstStats = getLlmFirstAnalysisStats();
    TaskQueueStats fullStats = getLlmFullAnalysisStats();

    return String.format(
        "First Analysis - New: %d, Processing: %d, Completed: %d, Failed: %d | " +
            "Full Analysis - New: %d, Processing: %d, Completed: %d, Failed: %d",
        firstStats.getNewTasks(), firstStats.getProcessingTasks(),
        firstStats.getCompletedTasks(), firstStats.getFailedTasks(),
        fullStats.getNewTasks(), fullStats.getProcessingTasks(),
        fullStats.getCompletedTasks(), fullStats.getFailedTasks()
    );
  }
}
package ru.mindils.jb2.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.TaskQueueStats;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.repository.GenericTaskQueueRepository;

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

  public GenericTaskQueueService(GenericTaskQueueRepository genericTaskQueueRepository) {
    this.genericTaskQueueRepository = genericTaskQueueRepository;
  }

  // ============ ПЕРВИЧНЫЙ АНАЛИЗ ============

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
  public Integer getCountLlmAnalysis(GenericTaskQueueType queueType) {
    return genericTaskQueueRepository.getCountLlmAnalysis(queueType);
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
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

  /**
   * Поставить в очередь все вакансии для первичного анализа
   * Добавляет только те вакансии, которые еще не анализировались
   *
   * @return количество добавленных записей
   */
  @Transactional
  public int enqueueFirstLlmAnalysis() {
    log.info("Starting to enqueue vacancies for LLM first analysis");
    int count = genericTaskQueueRepository.enqueueForLlmAnalyzed(GenericTaskQueueType.LLM_FIRST, "primary");
    log.info("Enqueued {} vacancies for first LLM analysis", count);
    return count;
  }

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

  // Удобные методы для первичного анализа

  /**
   * Получить количество новых задач для первичного анализа
   */
  @Transactional(readOnly = true)
  public Integer getNewLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.NEW);
  }

  /**
   * Получить количество обрабатываемых задач для первичного анализа
   */
  @Transactional(readOnly = true)
  public Integer getProcessingLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.PROCESSING);
  }

  /**
   * Получить количество завершенных задач для первичного анализа
   */
  @Transactional(readOnly = true)
  public Integer getCompletedLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.COMPLETED);
  }

  /**
   * Получить количество неудачных задач для первичного анализа
   */
  @Transactional(readOnly = true)
  public Integer getFailedLlmFirstTasksCount() {
    return getCountByTypeAndStatus(GenericTaskQueueType.LLM_FIRST, GenericTaskQueueStatus.FAILED);
  }

  /**
   * Получить полную статистику для первичного анализа
   */
  @Transactional(readOnly = true)
  public TaskQueueStats getLlmFirstAnalysisStats() {
    return getStatsForTaskType(GenericTaskQueueType.LLM_FIRST);
  }

  // Утилитарные методы

  /**
   * Сбросить "зависшие" задачи в статусе PROCESSING на FAILED
   * Полезно после сбоев системы
   *
   * @param queueType тип задач
   * @param olderThanMinutes задачи старше этого количества минут будут сброшены
   * @return количество сброшенных задач
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
   *
   * @param queueType тип задач
   * @return количество перезапущенных задач
   */
  @Transactional
  public int retryFailedTasks(GenericTaskQueueType queueType) {
    log.info("Retrying failed tasks for type {}", queueType);
    int count = genericTaskQueueRepository.retryFailedTasks(queueType);
    log.info("Retried {} failed tasks", count);
    return count;
  }

  /**
   * Перезапустить все неудачные задачи для первичного анализа
   */
  @Transactional
  public int retryFailedLlmFirstTasks() {
    return retryFailedTasks(GenericTaskQueueType.LLM_FIRST);
  }

  /**
   * Сбросить зависшие задачи для первичного анализа (старше 30 минут)
   */
  @Transactional
  public int resetStuckLlmFirstTasks() {
    return resetStuckProcessingTasks(GenericTaskQueueType.LLM_FIRST, 30);
  }
}
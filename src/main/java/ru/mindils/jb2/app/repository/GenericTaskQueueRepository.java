package ru.mindils.jb2.app.repository;

import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import ru.mindils.jb2.app.dto.TaskQueueStats;
import ru.mindils.jb2.app.entity.GenericTaskQueueStatus;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class GenericTaskQueueRepository {

  private final DataManager dataManager;
  @PersistenceContext
  private EntityManager em;

  public GenericTaskQueueRepository(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  /**
   * Добавляет в очередь все вакансии, которые еще не анализировались и не находятся в очереди
   */
  public int enqueueForLlmAnalyzed(GenericTaskQueueType queueType, String analyzeType) {
    var sql = """
        INSERT INTO jb2_generic_task_queue
            (entity_name, entity_id, task_type, status,
             error_message, priority, created_date, last_modified_date)
        SELECT
            'jb2_vacancy'          AS entity_name,
            v.id                   AS entity_id,
            ?1::varchar            AS task_type,
            ?3::varchar            AS status,
            NULL                   AS error_message,
            1                      AS priority,
            NOW()                  AS created_date,
            NOW()                  AS last_modified_date
        FROM jb2_vacancy v
        WHERE NOT EXISTS (
                  SELECT 1
                  FROM jb2_vacancy_llm_analysis a
                  WHERE a.vacancy_id = v.id
                    AND a.analyze_type = ?2 
              )
          AND NOT EXISTS (
                  SELECT 1
                  FROM jb2_generic_task_queue q
                  WHERE q.entity_id   = v.id
                    AND q.entity_name = 'jb2_vacancy'
                    AND q.task_type   = ?1::varchar
                    AND q.status IN (?4::varchar, ?5::varchar, ?6::varchar)
              )
        """;

    return em.createNativeQuery(sql)
        .setParameter(1, queueType.getId())
        .setParameter(2, analyzeType)
        .setParameter(3, GenericTaskQueueStatus.NEW.getId())
        .setParameter(4, GenericTaskQueueStatus.NEW.getId())
        .setParameter(5, GenericTaskQueueStatus.PROCESSING.getId())
        .setParameter(6, GenericTaskQueueStatus.COMPLETED.getId())
        .executeUpdate();
  }

  /**
   * Получает количество задач по типу (для обратной совместимости)
   * Теперь считает задачи в статусе PROCESSING
   */
  public Integer getCountLlmAnalysis(GenericTaskQueueType queueType) {
    return dataManager.loadValue("""
                select count(e) from jb2_GenericTaskQueue e
                where e.taskType = :taskType and e.status = :status
            """, Integer.class)
        .parameter("taskType", queueType.getId())
        .parameter("status", GenericTaskQueueStatus.NEW.getId())
        .one();
  }

  /**
   * Получает количество задач по типу и статусу
   */
  public Integer getCountByTypeAndStatus(GenericTaskQueueType queueType, GenericTaskQueueStatus status) {
    return dataManager.loadValue("""
                select count(e) from jb2_GenericTaskQueue e
                where e.taskType = :taskType and e.status = :status
            """, Integer.class)
        .parameter("taskType", queueType.getId())
        .parameter("status", status.getId())
        .one();
  }

  /**
   * Получает статистику по всем статусам для типа задач
   */
  public TaskQueueStats getStatsForTaskType(GenericTaskQueueType queueType) {
    List<KeyValueEntity> results = dataManager.loadValues("""
                select e.status, count(e) from jb2_GenericTaskQueue e
                where e.taskType = :taskType
                group by e.status
            """)
        .properties("status", "count")
        .parameter("taskType", queueType.getId())
        .list();

    TaskQueueStats stats = new TaskQueueStats();

    for (KeyValueEntity row : results) {
      String status = row.getValue("status");
      Long count = row.getValue("count");

      if (status != null && count != null) {
        switch (status) {
          case "NEW" -> stats.setNewTasks(count.intValue());
          case "PROCESSING" -> stats.setProcessingTasks(count.intValue());
          case "COMPLETED" -> stats.setCompletedTasks(count.intValue());
          case "FAILED" -> stats.setFailedTasks(count.intValue());
        }
      }
    }

    return stats;
  }

  /**
   * Сбрасывает статус "зависших" задач с PROCESSING на FAILED
   * Полезно для очистки задач, которые могли остаться в статусе PROCESSING после сбоя
   */
  public int resetStuckProcessingTasks(GenericTaskQueueType queueType, int olderThanMinutes) {
    String jpql = """
            UPDATE jb2_GenericTaskQueue e 
            SET e.status = :failedStatus, 
                e.errorMessage = :errorMessage
            WHERE e.taskType = :taskType 
              AND e.status = :processingStatus 
              AND e.lastModifiedDate < :cutoffTime
        """;

    return em.createQuery(jpql)
        .setParameter("failedStatus", GenericTaskQueueStatus.FAILED.getId())
        .setParameter("errorMessage", "Task reset due to timeout")
        .setParameter("taskType", queueType.getId())
        .setParameter("processingStatus", GenericTaskQueueStatus.PROCESSING.getId())
        .setParameter("cutoffTime", OffsetDateTime.now().minusMinutes(olderThanMinutes))
        .executeUpdate();
  }

  /**
   * Перезапускает неудачные задачи - меняет статус с FAILED на NEW
   */
  public int retryFailedTasks(GenericTaskQueueType queueType) {
    String jpql = """
            UPDATE jb2_GenericTaskQueue e 
            SET e.status = :newStatus, 
                e.errorMessage = null
            WHERE e.taskType = :taskType 
              AND e.status = :failedStatus
        """;

    return em.createQuery(jpql)
        .setParameter("newStatus", GenericTaskQueueStatus.NEW.getId())
        .setParameter("failedStatus", GenericTaskQueueStatus.FAILED.getId())
        .setParameter("taskType", queueType.getId())
        .executeUpdate();
  }

  /**
   * Получает все типы анализа из enum как строки
   */
  private List<String> getAllAnalysisTypes() {
    return Arrays.stream(VacancyLlmAnalysisType.values())
        .map(VacancyLlmAnalysisType::getId)
        .collect(Collectors.toList());
  }

  /**
   * Добавляет в очередь все вакансии для полного анализа
   * Вакансия добавляется если у неё отсутствует хотя бы один из требуемых типов анализа
   * или если анализ не завершен (статус не DONE)
   */
  public int enqueueForLlmFullAnalysis(GenericTaskQueueType queueType) {
    List<String> allAnalysisTypes = getAllAnalysisTypes();
    int totalTypesCount = allAnalysisTypes.size();

    // Строим IN клаузу динамически
    String inClause = allAnalysisTypes.stream()
        .map(type -> "'" + type + "'")
        .collect(Collectors.joining(", "));

    String sql = String.format("""
        INSERT INTO jb2_generic_task_queue
            (entity_name, entity_id, task_type, status,
             error_message, priority, created_date, last_modified_date)
        SELECT DISTINCT
            'jb2_vacancy'          AS entity_name,
            v.id                   AS entity_id,
            ?1                     AS task_type,
            ?2                     AS status,
            NULL                   AS error_message,
            1                      AS priority,
            NOW()                  AS created_date,
            NOW()                  AS last_modified_date
        FROM jb2_vacancy v
        WHERE 
            -- Проверяем, что у вакансии отсутствует хотя бы один из типов анализа
            -- или анализ не завершен (статус не DONE)
            (
                SELECT COUNT(DISTINCT a.analyze_type)
                FROM jb2_vacancy_llm_analysis a
                WHERE a.vacancy_id = v.id
                  AND a.status = 'DONE'
                  AND a.analyze_type IN (%s)
            ) < ?3
        
            -- И вакансия не находится в очереди на обработку
            AND NOT EXISTS (
                SELECT 1
                FROM jb2_generic_task_queue q
                WHERE q.entity_id   = v.id
                  AND q.entity_name = 'jb2_vacancy'
                  AND q.task_type   = ?1
                  AND q.status IN (?4, ?5, ?6)
            )
        """, inClause);

    return em.createNativeQuery(sql)
        .setParameter(1, queueType.getId())
        .setParameter(2, GenericTaskQueueStatus.NEW.getId())
        .setParameter(3, totalTypesCount)
        .setParameter(4, GenericTaskQueueStatus.NEW.getId())
        .setParameter(5, GenericTaskQueueStatus.PROCESSING.getId())
        .setParameter(6, GenericTaskQueueStatus.COMPLETED.getId())
        .executeUpdate();
  }
}
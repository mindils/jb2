package ru.mindils.jb2.app.repository;

import io.jmix.core.DataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;

@Repository
public class GenericTaskQueueRepository {

  private final DataManager dataManager;
  @PersistenceContext
  private EntityManager em;

  public GenericTaskQueueRepository(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  public int enqueueForLlmAnalyzed(GenericTaskQueueType queueType, String analyzeType) {
    var sql = """
        INSERT INTO jb2_generic_task_queue
            (entity_name, entity_id, task_type, processing, success,
             error_message, priority, created_date, last_modified_date)
        SELECT
            'jb2_vacancy'          AS entity_name,
            v.id                   AS entity_id,
            ?1::varchar            AS task_type,
            true                   AS processing,
            NULL                   AS success,
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
                    AND q.processing IS DISTINCT FROM false
              )
        """;

    return em.createNativeQuery(sql)
        .setParameter(1, queueType)
        .setParameter(2, analyzeType)
        .executeUpdate();
  }

  public Integer getCountLlmAnalysis(GenericTaskQueueType queueType) {
    return dataManager.loadValue("""
                select count(e) from jb2_GenericTaskQueue e
                where e.taskType = :taskType and e.processing = true
            """, Integer.class)
        .parameter("taskType", queueType)
        .one();
  }
}
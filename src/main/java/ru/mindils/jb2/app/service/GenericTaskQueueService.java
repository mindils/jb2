package ru.mindils.jb2.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.repository.GenericTaskQueueRepository;

/**
 * 1. Закинуть все вакансии для первичного анализа
 * 2. Закинуть все вакансии для full анализа тоесть все что по списку
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
   *
   * @return int
   */
  @Transactional
  public int enqueueFirstLlmAnalysis() {
    log.info("Starting analysis for vacancyQueueId: , type: ");
    return genericTaskQueueRepository.enqueueForLlmAnalyzed(GenericTaskQueueType.LLM_FIRST, "primary");
  }

  @Transactional(readOnly = true)
  public Integer getCountLlmAnalysis(GenericTaskQueueType queueType) {
    return genericTaskQueueRepository.getCountLlmAnalysis(queueType);
  }
}
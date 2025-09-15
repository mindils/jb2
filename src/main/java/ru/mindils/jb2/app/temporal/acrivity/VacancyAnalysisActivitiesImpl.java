package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueueType;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.service.VacancySyncService;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;

import java.util.Optional;

@ActivityImpl(taskQueues = VacancyAnalysisConstants.QUEUE)
@Component
public class VacancyAnalysisActivitiesImpl implements VacancyAnalysisActivities {

  private static final Logger log = Workflow.getLogger(VacancyAnalysisActivitiesImpl.class);

  private final VacancyAnalysisService vacancyAnalysisService;

  private final SystemAuthenticator authenticator;
  private final DataManager dataManager;

  public VacancyAnalysisActivitiesImpl(VacancyAnalysisService vacancyAnalysisService, SystemAuthenticator authenticator, DataManager dataManager) {
    this.vacancyAnalysisService = vacancyAnalysisService;
    this.authenticator = authenticator;
    this.dataManager = dataManager;
  }

  @Override
  public void analyze(Long vacancyQueueId) {
    authenticator.runWithSystem(() -> vacancyAnalysisService.analyzeVacancy(vacancyQueueId));
  }

  @Override
  public Long getNextVacancyId(VacancyAnalysisQueueType type) {
    return authenticator.withSystem(() -> {
      Optional<VacancyAnalysisQueue> maybe = dataManager.load(VacancyAnalysisQueue.class)
          .query("select e from jb2_VacancyAnalysisQueue e where e.processing = true and e.typeQueue = :type")
          .parameter("type", type)
          .maxResults(1)
          .optional();

      return maybe.map(VacancyAnalysisQueue::getId).orElse(null);
    });
  }

}
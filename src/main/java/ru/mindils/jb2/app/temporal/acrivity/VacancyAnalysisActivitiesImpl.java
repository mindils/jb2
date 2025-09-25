package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.temporal.VacancyAnalysisConstants;

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
  public void analyze(Long vacancyQueueId, AnalysisType type) {
    authenticator.runWithSystem(() -> {
          vacancyAnalysisService.analyze(vacancyQueueId, type);
        }
    );
  }

  @Override
  public Long getNextVacancyId(AnalysisType type) {
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
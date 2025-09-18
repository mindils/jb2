package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.service.VacancyService;
import ru.mindils.jb2.app.temporal.VacancyUpdateConstants;

@ActivityImpl(taskQueues = VacancyUpdateConstants.QUEUE)
@Component
public class VacancyUpdateActivitiesImpl implements VacancyUpdateActivities {

  private static final Logger log = Workflow.getLogger(VacancyUpdateActivitiesImpl.class);

  private final SystemAuthenticator authenticator;
  private final DataManager dataManager;
  private final VacancyService vacancyService;

  public VacancyUpdateActivitiesImpl(SystemAuthenticator authenticator,
                                     DataManager dataManager,
                                     VacancyService vacancyService) {
    this.authenticator = authenticator;
    this.dataManager = dataManager;
    this.vacancyService = vacancyService;
  }

  @Override
  public void update(Long vacancyQueueId) {
    authenticator.runWithSystem(() -> {
      VacancyAnalysisQueue q = dataManager.load(VacancyAnalysisQueue.class)
          .id(vacancyQueueId)
          .one();

      String vacancyId = q.getVacancy().getId();
      log.info("Updating vacancy {} (queueId={})", vacancyId, vacancyQueueId);

      // Обновляем вакансию и работодателя через ваш сервис
      vacancyService.update(vacancyId);

      // Помечаем как обработанную
      q.setProcessing(false);
      dataManager.save(q);
    });
  }

  @Override
  public Long getNextVacancyId() {
    return authenticator.withSystem(() ->
        dataManager.load(VacancyAnalysisQueue.class)
            .query("select e from jb2_VacancyAnalysisQueue e " +
                "where e.processing = true and e.typeQueue = :type " +
                "order by e.id")
            // важно: тут можно передать сам enum — как в анализе — Jmix корректно сконвертирует
            .parameter("type", AnalysisType.VACANCY_UPDATE)
            .maxResults(1)
            .optional()
            .map(VacancyAnalysisQueue::getId)
            .orElse(null)
    );
  }
}

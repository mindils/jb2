package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancyUpdateWorkflow;

@Service
public class VacancyUpdateWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(VacancyUpdateWorkflowService.class);

  private final WorkflowClient workflowClient;

  public VacancyUpdateWorkflowService(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  /**
   * Запускает workflow обновления вакансии
   *
   * @param vacancyId ID вакансии
   */
  public void startUpdateWorkflow(String vacancyId) {
    log.info("Starting vacancy update workflow for vacancy: {}", vacancyId);

    String workflowId = "vacancy-update-" + vacancyId + "-" + System.currentTimeMillis();

    VacancyUpdateWorkflow workflow = workflowClient.newWorkflowStub(
        VacancyUpdateWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancySyncConstants.VACANCY_QUEUE)
            .setWorkflowId(workflowId)
            .build()
    );

    try {
      workflow.updateVacancy(vacancyId);
      log.info("Vacancy update workflow completed successfully for vacancy: {}", vacancyId);
    } catch (Exception e) {
      log.error("Vacancy update workflow failed for vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw new RuntimeException("Ошибка при обновлении вакансии: " + e.getMessage(), e);
    }
  }
}
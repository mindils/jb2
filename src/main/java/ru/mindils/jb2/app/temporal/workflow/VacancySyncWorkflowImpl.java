package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.spring.boot.WorkflowImpl;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;

@WorkflowImpl(taskQueues = VacancySyncConstants.VACANCY_QUEUE)
public class VacancySyncWorkflowImpl implements VacancySyncWorkflow {

  @Override
  public void sync() {
    System.out.println("Sync Vacancy Workflow");
  }
}

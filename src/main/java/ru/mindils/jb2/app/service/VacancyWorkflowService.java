package ru.mindils.jb2.app.service;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;
import ru.mindils.jb2.app.temporal.workflow.VacancySyncWorkflow;

import java.util.List;
import java.util.Map;

@Service
public class VacancyWorkflowService {
  private final WorkflowClient workflowClient;
  private final WorkflowServiceStubs service;

  public VacancyWorkflowService(WorkflowClient workflowClient, WorkflowServiceStubs service) {
    this.workflowClient = workflowClient;
    this.service = service;
  }

  // Оригинальный метод без параметров (для обратной совместимости)
  public void sync() {
    sync(null);
  }

  // Новый метод с параметрами
  public void sync(List<Map<String, String>> requestParams) {
    VacancySyncWorkflow workflow = workflowClient.newWorkflowStub(
        VacancySyncWorkflow.class,
        WorkflowOptions.newBuilder()
            .setTaskQueue(VacancySyncConstants.VACANCY_QUEUE)
            .setWorkflowId(VacancySyncConstants.WORKFLOW_ID)
            .build()
    );

    WorkflowClient.start(() -> workflow.run(requestParams));
  }

  /**
   * Останавливает выполнение workflow через сигнал
   * @return true если сигнал был отправлен, false если workflow не запущен
   */
  public boolean stopSync() {
    try {
      // Проверяем, запущен ли workflow
      if (!isWorkflowRunning()) {
        return false;
      }

      // Получаем существующий workflow stub
      VacancySyncWorkflow workflow = workflowClient.newWorkflowStub(
          VacancySyncWorkflow.class,
          VacancySyncConstants.WORKFLOW_ID
      );

      // Отправляем сигнал остановки
      workflow.stop();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Проверяет, запущен ли workflow
   * @return true если workflow выполняется
   */
  public boolean isWorkflowRunning() {
    try {
      DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
          .setNamespace(workflowClient.getOptions().getNamespace())
          .setExecution(WorkflowExecution.newBuilder()
              .setWorkflowId(VacancySyncConstants.WORKFLOW_ID)
              .build())
          .build();

      DescribeWorkflowExecutionResponse response = service.blockingStub()
          .describeWorkflowExecution(request);

      WorkflowExecutionStatus status = response.getWorkflowExecutionInfo().getStatus();

      return status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
    } catch (Exception e) {
      return false;
    }
  }
}
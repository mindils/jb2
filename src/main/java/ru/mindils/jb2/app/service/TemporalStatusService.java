package ru.mindils.jb2.app.service;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.dto.WorkflowInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TemporalStatusService {

  private static final Logger log = LoggerFactory.getLogger(TemporalStatusService.class);

  private final WorkflowClient workflowClient;
  private final WorkflowServiceStubs serviceStubs;

  public TemporalStatusService(WorkflowClient workflowClient, WorkflowServiceStubs serviceStubs) {
    this.workflowClient = workflowClient;
    this.serviceStubs = serviceStubs;
  }

  /**
   * Получает список активных workflow'ов
   */
  public List<WorkflowInfo> getActiveWorkflows() {
    List<WorkflowInfo> activeWorkflows = new ArrayList<>();

    try {
      WorkflowServiceGrpc.WorkflowServiceBlockingStub stub = serviceStubs.blockingStub();

      // Запрос активных workflow'ов
      ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
          .setNamespace("default")
          .setQuery("ExecutionStatus='Running' OR ExecutionStatus='ContinuedAsNew'")
          .build();

      ListWorkflowExecutionsResponse response = stub.listWorkflowExecutions(request);

      response.getExecutionsList().forEach(execution -> {
        String workflowId = execution.getExecution().getWorkflowId();
        String runId = execution.getExecution().getRunId();
        String workflowType = execution.getType().getName();
        String status = execution.getStatus().name();

        LocalDateTime startTime = null;
        if (execution.hasStartTime()) {
          Instant instant = Instant.ofEpochSecond(
              execution.getStartTime().getSeconds(),
              execution.getStartTime().getNanos()
          );
          startTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        WorkflowInfo info = new WorkflowInfo();
        info.setWorkflowId(workflowId);
        info.setRunId(runId);
        info.setWorkflowType(workflowType);
        info.setStatus(status);
        info.setStartTime(startTime);
        info.setDescription(getWorkflowDescription(workflowType, workflowId));

        activeWorkflows.add(info);
      });

    } catch (Exception e) {
      log.error("Error getting active workflows: {}", e.getMessage(), e);
    }

    return activeWorkflows;
  }

  /**
   * Проверяет, запущен ли конкретный workflow
   */
  public boolean isWorkflowRunning(String workflowId) {
    try {
      WorkflowServiceGrpc.WorkflowServiceBlockingStub stub = serviceStubs.blockingStub();

      // Создаем WorkflowExecution для запроса
      WorkflowExecution execution = WorkflowExecution.newBuilder()
          .setWorkflowId(workflowId)
          .build();

      DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
          .setNamespace("default")
          .setExecution(execution)
          .build();

      DescribeWorkflowExecutionResponse response = stub.describeWorkflowExecution(request);

      WorkflowExecutionStatus status = response.getWorkflowExecutionInfo().getStatus();
      return status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING ||
          status == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW;

    } catch (Exception e) {
      log.debug("Workflow {} is not running or not found: {}", workflowId, e.getMessage());
      return false;
    }
  }

  /**
   * Получает статус конкретного workflow'а
   */
  public String getWorkflowStatus(String workflowId) {
    try {
      WorkflowServiceGrpc.WorkflowServiceBlockingStub stub = serviceStubs.blockingStub();

      WorkflowExecution execution = WorkflowExecution.newBuilder()
          .setWorkflowId(workflowId)
          .build();

      DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
          .setNamespace("default")
          .setExecution(execution)
          .build();

      DescribeWorkflowExecutionResponse response = stub.describeWorkflowExecution(request);

      return response.getWorkflowExecutionInfo().getStatus().name();

    } catch (Exception e) {
      log.debug("Error getting status for workflow {}: {}", workflowId, e.getMessage());
      return "NOT_FOUND";
    }
  }

  /**
   * Альтернативный метод проверки через WorkflowClient (более простой, но менее информативный)
   */
  public boolean isWorkflowRunningViaClient(String workflowId) {
    try {
      WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);

      // Попытка получить результат с минимальным timeout
      // Если workflow запущен - будет исключение timeout
      // Если завершен - получим результат
      stub.getResult(1, TimeUnit.MILLISECONDS, String.class);

      // Если дошли сюда - workflow завершен
      return false;

    } catch (Exception e) {
      // Если timeout или другая ошибка - скорее всего workflow работает
      String message = e.getMessage();
      if (message != null && (message.contains("timeout") || message.contains("DEADLINE_EXCEEDED"))) {
        return true;
      }
      // Другие ошибки могут означать что workflow не найден
      log.debug("Workflow {} check failed: {}", workflowId, message);
      return false;
    }
  }

  /**
   * Получает описание workflow'а по типу и ID
   */
  private String getWorkflowDescription(String workflowType, String workflowId) {
    return switch (workflowType) {
      case "VacancySyncWorkflowImpl" -> "Синхронизация вакансий с hh.ru";
      case "VacancyAnalysisWorkflowImpl" -> {
        if (workflowId.contains("PRIMARY")) {
          yield "Первичный анализ вакансий (Java, Jmix, AI)";
        } else if (workflowId.contains("SOCIAL")) {
          yield "Социальный анализ вакансий (режим работы, домены)";
        } else {
          yield "Анализ вакансий";
        }
      }
      default -> "Неизвестный workflow";
    };
  }
}
package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import ru.mindils.jb2.app.service.analysis.chain.AnalysisChainConfig;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisResult;
import ru.mindils.jb2.app.temporal.VacancyChainAnalysisConstants;
import ru.mindils.jb2.app.temporal.acrivity.VacancyChainAnalysisActivities;

import java.time.Duration;

@WorkflowImpl(taskQueues = VacancyChainAnalysisConstants.QUEUE)
public class VacancyChainAnalysisWorkflowImpl implements VacancyChainAnalysisWorkflow {

  private static final Logger log = Workflow.getLogger(VacancyChainAnalysisWorkflowImpl.class);

  private final VacancyChainAnalysisActivities activities = Workflow.newActivityStub(
      VacancyChainAnalysisActivities.class,
      ActivityOptions.newBuilder()
          .setRetryOptions(
              RetryOptions.newBuilder()
                  .setMaximumAttempts(3)
                  .build()
          )
          .setStartToCloseTimeout(Duration.ofMinutes(10)) // увеличиваем таймаут для цепочки
          .build()
  );

  @Override
  public void runChainAnalysis(AnalysisChainConfig config) {
    log.info("Starting chain analysis workflow with config: {}", config.chainId());

    int processedCount = 0;
    int successCount = 0;
    int failedCount = 0;

    try {
      while (true) {
        String nextVacancyId = activities.getNextVacancyFromQueue(config.chainId());

        if (nextVacancyId == null) {
          log.info("No more vacancies to process for chain: {}. Processed: {}, Success: {}, Failed: {}",
              config.chainId(), processedCount, successCount, failedCount);
          break;
        }

        log.info("Processing vacancy: {} with chain: {}", nextVacancyId, config.chainId());

        try {
          ChainAnalysisResult result = activities.executeChainAnalysis(nextVacancyId, config);

          activities.logAnalysisResult(result);

          if (result.success()) {
            successCount++;
            activities.markVacancyProcessed(nextVacancyId, config.chainId(), true, null);
          } else {
            failedCount++;
            activities.markVacancyProcessed(nextVacancyId, config.chainId(), false, result.errorMessage());
          }

        } catch (Exception e) {
          log.error("Error processing vacancy {}: {}", nextVacancyId, e.getMessage());
          failedCount++;
          activities.markVacancyProcessed(nextVacancyId, config.chainId(), false, e.getMessage());
        }

        processedCount++;

        // Небольшая пауза между обработкой вакансий
        Workflow.sleep(Duration.ofSeconds(1));
      }

      log.info("Chain analysis workflow completed. Chain: {}, Total: {}, Success: {}, Failed: {}",
          config.chainId(), processedCount, successCount, failedCount);

    } catch (Exception e) {
      log.error("Chain analysis workflow failed for config {}: {}", config.chainId(), e.getMessage(), e);
      throw e;
    }
  }
}
package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.entity.VacancyChainAnalysisQueue;
import ru.mindils.jb2.app.service.analysis.chain.AnalysisChainConfig;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisResult;
import ru.mindils.jb2.app.service.analysis.chain.VacancyChainAnalysisService;
import ru.mindils.jb2.app.temporal.VacancyChainAnalysisConstants;

@ActivityImpl(taskQueues = VacancyChainAnalysisConstants.QUEUE)
@Component
public class VacancyChainAnalysisActivitiesImpl implements VacancyChainAnalysisActivities {

  private static final Logger log = Workflow.getLogger(VacancyChainAnalysisActivitiesImpl.class);

  private final VacancyChainAnalysisService chainAnalysisService;
  private final SystemAuthenticator authenticator;
  private final DataManager dataManager;

  public VacancyChainAnalysisActivitiesImpl(
      VacancyChainAnalysisService chainAnalysisService,
      SystemAuthenticator authenticator,
      DataManager dataManager
  ) {
    this.chainAnalysisService = chainAnalysisService;
    this.authenticator = authenticator;
    this.dataManager = dataManager;
  }

  @Override
  public ChainAnalysisResult executeChainAnalysis(String vacancyId, AnalysisChainConfig config) {
    log.info("Executing chain analysis for vacancy: {} with config: {}", vacancyId, config.chainId());

    return authenticator.withSystem(() ->
        chainAnalysisService.executeChain(vacancyId, config));
  }

  @Override
  public String getNextVacancyFromQueue(ChainAnalysisType chainType) {
    return authenticator.withSystem(() ->
        dataManager.load(VacancyChainAnalysisQueue.class)
            .query("select e from jb2_VacancyChainAnalysisQueue e " +
                "where e.processing = true and e.chainType = :chainType " +
                "order by e.id")
            .parameter("chainType", chainType.getId())
            .maxResults(1)
            .optional()
            .map(queue -> queue.getVacancy().getId())
            .orElse(null)
    );
  }

  @Override
  public void markVacancyProcessed(String vacancyId, ChainAnalysisType chainType, boolean success, String errorMessage) {
    authenticator.runWithSystem(() -> {
      dataManager.load(VacancyChainAnalysisQueue.class)
          .query("select e from jb2_VacancyChainAnalysisQueue e " +
              "where e.vacancy.id = :vacancyId and e.chainType = :chainType")
          .parameter("vacancyId", vacancyId)
          .parameter("chainType", chainType.getId())
          .optional()
          .ifPresent(queue -> {
            queue.setProcessing(false);
            queue.setSuccess(success);
            queue.setErrorMessage(errorMessage);
            dataManager.save(queue);
          });
    });
  }

  @Override
  public void logAnalysisResult(ChainAnalysisResult result) {
    if (result.success()) {
      if (result.stoppedAt() != null) {
        log.info("Chain analysis stopped at step '{}' for vacancy {}: {}",
            result.stoppedAt(), result.vacancyId(), result.stopReason());
      } else {
        log.info("Chain analysis completed successfully for vacancy {}", result.vacancyId());
        if (result.finalScore() != null) {
          log.info("Final score for vacancy {}: {} ({})",
              result.vacancyId(),
              result.finalScore().totalScore(),
              result.finalScore().rating());
        }
      }
    } else {
      log.error("Chain analysis failed for vacancy {}: {}",
          result.vacancyId(), result.errorMessage());
    }
  }
}
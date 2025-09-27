package ru.mindils.jb2.app.temporal.acrivity;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.LlmAnalysisResponse;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisStatus;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisService;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;

import java.util.List;
import java.util.Optional;

@ActivityImpl(taskQueues = VacancyLlmAnalysisConstants.QUEUE)
@Component
public class VacancyLlmAnalysisActivitiesImpl implements VacancyLllAnalysisActivities {

  private static final Logger log = LoggerFactory.getLogger(VacancyLlmAnalysisActivitiesImpl.class);

  private final VacancyLlmAnalysisService vacancyLlmAnalysisService;
  private final SystemAuthenticator authenticator;
  private final DataManager dataManager;

  public VacancyLlmAnalysisActivitiesImpl(VacancyLlmAnalysisService vacancyLlmAnalysisService,
                                          SystemAuthenticator authenticator,
                                          DataManager dataManager) {
    this.vacancyLlmAnalysisService = vacancyLlmAnalysisService;
    this.authenticator = authenticator;
    this.dataManager = dataManager;
  }

  @Override
  public LlmAnalysisResponse analyze(String vacancyId, VacancyLlmAnalysisType type) {
    return authenticator.withSystem(() -> {
      log.info("Starting analysis activity for vacancy {} with type {}", vacancyId, type);

      Optional<Vacancy> maybeVacancy = dataManager.load(Vacancy.class)
          .id(vacancyId)
          .optional();

      if (maybeVacancy.isEmpty()) {
        log.warn("Vacancy not found with id: {}", vacancyId);
        throw new RuntimeException("Vacancy not found: " + vacancyId);
      }

      Vacancy vacancy = maybeVacancy.get();
      LlmAnalysisResponse response = vacancyLlmAnalysisService.analyze(vacancy, type);

      log.info("Analysis completed for vacancy {} with type {}, LLM call ID: {}",
          vacancyId, type, response.llmCallId());

      return response;
    });
  }

  @Override
  public void saveAnalysisResult(String vacancyId, VacancyLlmAnalysisType type, LlmAnalysisResponse llmResponse) {
    authenticator.runWithSystem(() -> {
      log.info("Saving analysis result for vacancy {} with type {} from LLM call {}",
          vacancyId, type, llmResponse.llmCallId());
      vacancyLlmAnalysisService.saveAnalysisResult(vacancyId, type, llmResponse);
    });
  }

  @Override
  public void setStatusSkipIfJavaFalse(String vacancyId, LlmAnalysisResponse llmResponse) {
    List<VacancyLlmAnalysisType> stopFactors = List.of(
        VacancyLlmAnalysisType.STOP_FACTORS
    );

    boolean isJavaTrue = Optional.ofNullable(llmResponse.jsonNode())
        .map(n -> n.get("java"))
        .filter(JsonNode::isBoolean)
        .map(JsonNode::booleanValue)
        .orElse(true);

    if (isJavaTrue) return;

    authenticator.runWithSystem(() -> {
      log.info("Setting SKIPPED status for vacancy {} for types: {}", vacancyId, stopFactors);

      stopFactors.forEach(type ->
          vacancyLlmAnalysisService.saveAnalysisStatus(vacancyId, type, VacancyLlmAnalysisStatus.SKIPPED)
      );
    });
  }

  @Override
  public void saveAnalysisStatus(String vacancyId, VacancyLlmAnalysisType type, VacancyLlmAnalysisStatus status) {
    authenticator.runWithSystem(() ->
        vacancyLlmAnalysisService.saveAnalysisStatus(vacancyId, type, status));
  }
}
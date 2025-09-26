package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisService;
import ru.mindils.jb2.app.temporal.VacancyLlmAnalysisConstants;

import java.util.Optional;

@ActivityImpl(taskQueues = VacancyLlmAnalysisConstants.QUEUE)
@Component
public class VacancyLlmAnalysisActivitiesImpl implements VacancyLllAnalysisActivities {

  private static final Logger log = LoggerFactory.getLogger(VacancyLlmAnalysisActivitiesImpl.class);

  private final VacancyLlmAnalysisService vacancyLlmAnalysisService;

  private final SystemAuthenticator authenticator;
  private final DataManager dataManager;

  public VacancyLlmAnalysisActivitiesImpl(VacancyLlmAnalysisService vacancyLlmAnalysisService, SystemAuthenticator authenticator, DataManager dataManager) {
    this.vacancyLlmAnalysisService = vacancyLlmAnalysisService;
    this.authenticator = authenticator;
    this.dataManager = dataManager;
  }

  @Override
  public String analyze(String vacancyId, VacancyLlmAnalysisType type) {
    return authenticator.withSystem(() -> {
      // todo: подумать, что делать есть вакансию не найдем случайно
      Optional<Vacancy> maybe = dataManager.load(Vacancy.class)
          .id(vacancyId)
          .optional();
      return maybe.map(vacancy -> vacancyLlmAnalysisService.analyze(vacancy, type)).orElse(null);
    });
  }

  @Override
  public Optional<String> saveAnalysisResult(String vacancyId, VacancyLlmAnalysisType type, String llm) {
    return authenticator.withSystem(() -> vacancyLlmAnalysisService.saveAnalysisResult(vacancyId, type, llm)
    );
  }
}
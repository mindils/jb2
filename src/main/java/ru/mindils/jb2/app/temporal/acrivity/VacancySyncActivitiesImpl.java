package ru.mindils.jb2.app.temporal.acrivity;

import io.jmix.core.security.SystemAuthenticator;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.service.VacancySyncService;
import ru.mindils.jb2.app.temporal.VacancySyncConstants;

@ActivityImpl(taskQueues = VacancySyncConstants.VACANCY_QUEUE)
@Component
public class VacancySyncActivitiesImpl implements VacancySyncActivities {

  private static final Logger log = Workflow.getLogger(VacancySyncActivitiesImpl.class);

  private final VacancySyncService vacancySyncService;

  private final SystemAuthenticator authenticator;

  public VacancySyncActivitiesImpl(VacancySyncService vacancySyncService, SystemAuthenticator authenticator) {
    this.vacancySyncService = vacancySyncService;
    this.authenticator = authenticator;
  }

  @Override
  public VacancySearchResponseDto searchVacancies(int page) {
    log.info("=== ACTIVITY CALLED: searchVacancies for page: {} ===", page);
    try {
      VacancySearchResponseDto response = authenticator.withSystem(() -> vacancySyncService.searchVacancies(page));
      log.info("Activity searchVacancies completed successfully. Found {} vacancies on page {}, total pages: {}",
          response.getItems().size(), page, response.getPages());
      return response;
    } catch (Exception e) {
      log.error("=== ACTIVITY FAILED: searchVacancies for page {}: {} ===", page, e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public void saveVacancy(String vacancyId) {
    log.info("Saving vacancy with ID: {}", vacancyId);
    try {
      authenticator.runWithSystem(() -> vacancySyncService.saveVacancyWithDetails(vacancyId));
      log.info("Successfully saved vacancy: {}", vacancyId);
    } catch (Exception e) {
      log.error("Error saving vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw e;
    }
  }
}
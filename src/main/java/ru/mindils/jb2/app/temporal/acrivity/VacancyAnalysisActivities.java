package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueueType;

@ActivityInterface
public interface VacancyAnalysisActivities {

  void analyze(Long vacancyAnalysisId);

  Long getNextVacancyId(VacancyAnalysisQueueType type);

}
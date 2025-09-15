package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.entity.AnalysisType;

@ActivityInterface
public interface VacancyAnalysisActivities {

  void analyze(Long vacancyAnalysisId, AnalysisType type);

  Long getNextVacancyId(AnalysisType type);

}
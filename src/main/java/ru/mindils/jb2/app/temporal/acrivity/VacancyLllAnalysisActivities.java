package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

import java.util.Optional;

@ActivityInterface
public interface VacancyLllAnalysisActivities {

  String analyze(String vacancyId, VacancyLlmAnalysisType type);

  Optional<String> saveAnalysisResult(String vacancyId, VacancyLlmAnalysisType type, String llm);
}
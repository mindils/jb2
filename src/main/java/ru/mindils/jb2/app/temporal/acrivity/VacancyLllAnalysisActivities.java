package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.dto.LlmAnalysisResponse;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisStatus;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

import java.util.Optional;

@ActivityInterface
public interface VacancyLllAnalysisActivities {

  /**
   * Анализирует вакансию и возвращает DTO с результатом и метаинформацией
   */
  LlmAnalysisResponse analyze(String vacancyId, VacancyLlmAnalysisType type);

  /**
   * Сохраняет результат анализа из DTO
   */
  void saveAnalysisResult(String vacancyId, VacancyLlmAnalysisType type, LlmAnalysisResponse llmResponse);

  /**
   * Устанавливает статус SKIPPED для указанных типов анализа
   */
  void setStatusSkipIfJavaFalse(String vacancyId, LlmAnalysisResponse llmResponse);

  void saveAnalysisStatus(String vacancyId, VacancyLlmAnalysisType type, VacancyLlmAnalysisStatus status);
}
package ru.mindils.jb2.app.service.analysis.prompt;

import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

public interface PromptGenerator {
  VacancyLlmAnalysisType getSupportedType();

  String generatePrompt(Vacancy vacancy);
}
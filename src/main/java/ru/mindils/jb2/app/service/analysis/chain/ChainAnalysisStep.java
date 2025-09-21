package ru.mindils.jb2.app.service.analysis.chain;

import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

/**
 * Интерфейс для шага анализа в цепочке
 */
public interface ChainAnalysisStep {

  /**
   * Уникальный идентификатор шага
   */
  String getStepId();

  /**
   * Человекочитаемое описание шага
   */
  String getDescription();

  /**
   * Выполнить анализ
   * @param vacancy вакансия для анализа
   * @param currentAnalysis текущее состояние анализа
   * @return результат выполнения шага
   */
  ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis);
}

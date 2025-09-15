package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

/**
 * Интерфейс стратегии для анализа вакансий с помощью LLM.
 */
public interface AnalysisStrategy {

  /**
   * Возвращает тип анализа, за который отвечает стратегия.
   */
  AnalysisType getAnalysisType();

  /**
   * Генерирует промпт для LLM на основе данных вакансии.
   *
   * @param vacancy Вакансия для анализа.
   * @return Готовый текст промпта.
   */
  String getPrompt(Vacancy vacancy);

  /**
   * Обновляет сущность VacancyAnalysis на основе ответа от LLM.
   *
   * @param analysis Сущность для обновления.
   * @param llmResponse Ответ от LLM в формате JsonNode.
   */
  void updateAnalysis(VacancyAnalysis analysis, JsonNode llmResponse);
}
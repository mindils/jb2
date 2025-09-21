package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.service.analysis.chain.AnalysisChainConfig;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisResult;

@ActivityInterface
public interface VacancyChainAnalysisActivities {

  /**
   * Выполнить цепочку анализа для одной вакансии
   */
  ChainAnalysisResult executeChainAnalysis(String vacancyId, AnalysisChainConfig config);

  /**
   * Получить следующую вакансию для анализа из очереди
   */
  String getNextVacancyFromQueue(ChainAnalysisType chainType);

  /**
   * Отметить вакансию как обработанную в очереди
   */
  void markVacancyProcessed(String vacancyId, ChainAnalysisType chainType, boolean success, String errorMessage);

  /**
   * Логирование результата анализа
   */
  void logAnalysisResult(ChainAnalysisResult result);
}
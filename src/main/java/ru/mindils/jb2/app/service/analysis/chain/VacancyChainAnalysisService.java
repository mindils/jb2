package ru.mindils.jb2.app.service.analysis.chain;

import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.VacancyScorer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Упрощенный сервис для выполнения цепочки анализа вакансий
 */
@Service
public class VacancyChainAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyChainAnalysisService.class);

  private final DataManager dataManager;
  private final AnalysisResultManager analysisResultManager;
  private final Map<String, ChainAnalysisStep> stepMap;

  public VacancyChainAnalysisService(
      DataManager dataManager,
      AnalysisResultManager analysisResultManager,
      List<ChainAnalysisStep> steps
  ) {
    this.dataManager = dataManager;
    this.analysisResultManager = analysisResultManager;
    this.stepMap = steps.stream()
        .collect(Collectors.toMap(ChainAnalysisStep::getStepId, Function.identity()));
  }

  /**
   * Выполняет цепочку анализа для вакансии
   */
  @Transactional
  public ChainAnalysisResult executeChain(String vacancyId, AnalysisChainConfig config) {
    log.info("Starting chain analysis for vacancy: {} with config: {}", vacancyId, config.chainId());

    try {
      // Загружаем вакансию и анализ
      Vacancy vacancy = dataManager.load(Vacancy.class).id(vacancyId).one();
      VacancyAnalysis analysis = dataManager.load(VacancyAnalysis.class)
          .id(vacancyId)
          .optional()
          .orElseGet(() -> createNewAnalysis(vacancy));

      ChainAnalysisResult.Builder resultBuilder = ChainAnalysisResult.builder()
          .vacancyId(vacancyId)
          .chainConfig(config)
          .success(true);

      // Выполняем шаги по очереди
      for (String stepId : config.stepIds()) {
        ChainAnalysisStep step = stepMap.get(stepId);
        if (step == null) {
          log.error("Step not found: {}", stepId);
          return resultBuilder
              .success(false)
              .errorMessage("Step not found: " + stepId)
              .build();
        }

        log.info("Executing step: {} for vacancy: {}", stepId, vacancyId);

        ChainStepResult stepResult = step.execute(vacancy, analysis);
        resultBuilder.addStepResult(stepId, stepResult);

        // Сохраняем промежуточный результат
        dataManager.save(analysis);

        // Проверяем условие остановки
        if (!stepResult.shouldContinue()) {
          log.info("Chain stopped at step: {} for vacancy: {}, reason: {}",
              stepId, vacancyId, stepResult.stopReason());

          // Если нужно, все равно считаем скор даже при остановке
          VacancyScorer.VacancyScore finalScore = null;
          if (config.calculateScore()) {
            analysisResultManager.recalculateAndSaveScore(analysis);
            finalScore = new VacancyScorer.VacancyScore(
                analysis.getFinalScore() != null ? analysis.getFinalScore() : 0,
                analysis.getRatingEnum() != null ? analysis.getRatingEnum() : ru.mindils.jb2.app.entity.VacancyRating.VERY_POOR
            );
            dataManager.save(analysis); // Сохраняем с обновленным скором
          }

          return resultBuilder
              .stoppedAt(stepId)
              .stopReason(stepResult.stopReason())
              .finalScore(finalScore)
              .build();
        }
      }

      // Если нужно, вычисляем итоговый скор после всех шагов
      VacancyScorer.VacancyScore finalScore = null;
      if (config.calculateScore()) {
        analysisResultManager.recalculateAndSaveScore(analysis);
        finalScore = new VacancyScorer.VacancyScore(
            analysis.getFinalScore() != null ? analysis.getFinalScore() : 0,
            analysis.getRatingEnum() != null ? analysis.getRatingEnum() : ru.mindils.jb2.app.entity.VacancyRating.VERY_POOR
        );

        dataManager.save(analysis); // Сохраняем с итоговым скором

        log.info("Calculated final score for vacancy {}: {} ({})",
            vacancyId, finalScore.totalScore(), finalScore.rating());
      }

      log.info("Successfully completed chain analysis for vacancy: {}", vacancyId);

      return resultBuilder
          .finalScore(finalScore)
          .build();

    } catch (Exception e) {
      log.error("Error in chain analysis for vacancy {}: {}", vacancyId, e.getMessage(), e);
      return ChainAnalysisResult.builder()
          .vacancyId(vacancyId)
          .chainConfig(config)
          .success(false)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private VacancyAnalysis createNewAnalysis(Vacancy vacancy) {
    VacancyAnalysis analysis = dataManager.create(VacancyAnalysis.class);
    analysis.setId(vacancy.getId());
    analysis.setVacancy(vacancy);
    return analysis;
  }
}
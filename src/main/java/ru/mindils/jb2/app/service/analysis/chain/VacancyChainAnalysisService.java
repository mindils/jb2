package ru.mindils.jb2.app.service.analysis.chain;

import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.VacancyScorer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения цепочки анализа вакансий
 */
@Service
public class VacancyChainAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyChainAnalysisService.class);

  private final DataManager dataManager;
  private final VacancyScorer vacancyScorer;
  private final Map<String, ChainAnalysisStep> stepMap;

  public VacancyChainAnalysisService(
      DataManager dataManager,
      VacancyScorer vacancyScorer,
      List<ChainAnalysisStep> steps
  ) {
    this.dataManager = dataManager;
    this.vacancyScorer = vacancyScorer;
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

      // Выполняем шаги по очереди
      ChainAnalysisResult.Builder resultBuilder = ChainAnalysisResult.builder()
          .vacancyId(vacancyId)
          .chainConfig(config)
          .success(true);

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

        if (!stepResult.shouldContinue()) {
          log.info("Chain stopped at step: {} for vacancy: {}, reason: {}",
              stepId, vacancyId, stepResult.stopReason());
          return resultBuilder
              .stoppedAt(stepId)
              .stopReason(stepResult.stopReason())
              .build();
        }
      }

      // Если нужно, вычисляем итоговый скор
      VacancyScorer.VacancyScore score = null;
      if (config.calculateScore()) {
        score = vacancyScorer.calculateScore(analysis);
        log.info("Calculated score for vacancy {}: {} ({})",
            vacancyId, score.totalScore(), score.rating());
      }

      log.info("Successfully completed chain analysis for vacancy: {}", vacancyId);

      return resultBuilder
          .finalScore(score)
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

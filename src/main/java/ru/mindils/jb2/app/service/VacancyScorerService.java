package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.flowui.model.CollectionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.VacancyScoringResult;
import ru.mindils.jb2.app.entity.*;
import ru.mindils.jb2.app.service.analysis.VacancyScorer;
import ru.mindils.jb2.app.util.UuidGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для расчета и сохранения оценки вакансии
 */
@Service
public class VacancyScorerService {
  private static final Logger log = LoggerFactory.getLogger(VacancyScorerService.class);
  private static final String FACTOR_DELIMITER = " | ";

  private final VacancyScorer vacancyScorer;
  private final DataManager dataManager;
  private final UuidGenerator uuidGenerator;
  private final FetchPlans fetchPlans;

  public VacancyScorerService(VacancyScorer vacancyScorer,
                              DataManager dataManager,
                              UuidGenerator uuidGenerator,
                              FetchPlans fetchPlans) {
    this.vacancyScorer = vacancyScorer;
    this.dataManager = dataManager;
    this.uuidGenerator = uuidGenerator;
    this.fetchPlans = fetchPlans;
  }

  /**
   * Результат массовой обработки вакансий
   */
  public static class BatchProcessingResult {
    private final int total;
    private final int successful;
    private final int failed;
    private final int skipped;
    private final List<String> errors;

    public BatchProcessingResult(int total, int successful, int failed, int skipped, List<String> errors) {
      this.total = total;
      this.successful = successful;
      this.failed = failed;
      this.skipped = skipped;
      this.errors = errors;
    }

    public int getTotal() {
      return total;
    }

    public int getSuccessful() {
      return successful;
    }

    public int getFailed() {
      return failed;
    }

    public int getSkipped() {
      return skipped;
    }

    public List<String> getErrors() {
      return errors;
    }

    @Override
    public String toString() {
      return String.format("Всего: %d, Успешно: %d, Ошибки: %d, Пропущено: %d",
          total, successful, failed, skipped);
    }
  }

  /**
   * Рассчитывает оценки для всех вакансий из лоадера (Vacancy)
   *
   * @param loader    CollectionLoader с вакансиями
   * @param batchSize размер батча для обработки
   * @return результат массовой обработки
   */
  @Transactional
  public BatchProcessingResult calculateAndSaveFromLoader(CollectionLoader<Vacancy> loader, int batchSize) {
    log.info("Starting batch vacancy scoring from loader with batch size: {}", batchSize);
    LoadContext<Vacancy> baseCtx = loader.createLoadContext();
    baseCtx.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());
    return processVacanciesBatch(baseCtx, batchSize);
  }

  /**
   * Рассчитывает оценки для всех вакансий из VVacancySearch лоадера
   * ВАЖНО: обрабатывает только вакансии, соответствующие примененным фильтрам
   *
   * @param loader    CollectionLoader с VVacancySearch
   * @param batchSize размер батча для обработки
   * @return результат массовой обработки
   */
  @Transactional
  public BatchProcessingResult calculateAndSaveFromVViewLoader(CollectionLoader<VVacancySearch> loader, int batchSize) {
    log.info("Starting batch vacancy scoring from VVacancySearch loader with batch size: {}", batchSize);

    // Создаем базовый контекст с учетом всех фильтров из loader
    LoadContext<VVacancySearch> baseCtx = loader.createLoadContext();

    // Модифицируем только FetchPlan, сохраняя все условия запроса
    baseCtx.setFetchPlan(fetchPlans.builder(VVacancySearch.class).add("id").build());

    // Логируем параметры запроса для отладки
    if (baseCtx.getQuery() != null) {
      log.info("Query condition: {}", baseCtx.getQuery().getCondition());
      log.info("Query parameters: {}", baseCtx.getQuery().getParameters());
    }

    return processVVacancySearchBatch(baseCtx, batchSize);
  }

  /**
   * Внутренний метод для обработки вакансий батчами (Vacancy)
   */
  private BatchProcessingResult processVacanciesBatch(LoadContext<Vacancy> baseCtx, int batchSize) {
    int totalProcessed = 0;
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;
    int first = 0;
    List<String> errors = new ArrayList<>();

    while (true) {
      // Копируем контекст для текущей страницы
      @SuppressWarnings("unchecked")
      LoadContext<Vacancy> pageCtx = (LoadContext<Vacancy>) baseCtx.copy();
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<Vacancy> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) {
        break;
      }

      log.debug("Processing batch starting from {}, size: {}", first, page.size());

      // Собираем ID вакансий
      List<String> vacancyIds = page.stream()
          .map(v -> (String) EntityValues.getId(v))
          .collect(Collectors.toList());

      // Загружаем все анализы для батча вакансий одним запросом
      Map<String, List<VacancyLlmAnalysis>> analysesMap = loadAnalysesForVacancies(vacancyIds);

      // Обрабатываем каждую вакансию
      SaveContext saveCtx = new SaveContext();
      for (Vacancy vacancy : page) {
        String vacancyId = (String) EntityValues.getId(vacancy);
        totalProcessed++;

        try {
          List<VacancyLlmAnalysis> analyses = analysesMap.get(vacancyId);
          if (analyses == null || analyses.isEmpty()) {
            log.debug("No completed analysis found for vacancy {}, skipping", vacancyId);
            skippedCount++;
            continue;
          }

          // Рассчитываем оценку
          VacancyScore score = calculateScoreForVacancy(vacancy, analyses);
          saveCtx.saving(score);

          log.debug("Prepared score for vacancy {}: {} points ({})",
              vacancyId, score.getTotalScore(), score.getRating());
        } catch (Exception e) {
          failedCount++;
          String errorMsg = String.format("Error processing vacancy %s: %s", vacancyId, e.getMessage());
          log.error(errorMsg, e);
          errors.add(errorMsg);
        }
      }

      // Сохраняем батч оценок
      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        try {
          dataManager.save(saveCtx);
          int batchSaved = saveCtx.getEntitiesToSave().size();
          successCount += batchSaved;
          log.debug("Successfully saved {} scores in current batch", batchSaved);
        } catch (Exception e) {
          failedCount += saveCtx.getEntitiesToSave().size();
          String errorMsg = "Error saving batch: " + e.getMessage();
          log.error(errorMsg, e);
          errors.add(errorMsg);
        }
      }

      first += page.size();
    }

    BatchProcessingResult result = new BatchProcessingResult(
        totalProcessed, successCount, failedCount, skippedCount, errors);
    log.info("Batch scoring completed. {}", result);
    return result;
  }

  /**
   * Внутренний метод для обработки вакансий батчами из VVacancySearch
   * Сохраняет все условия фильтрации из loader
   */
  private BatchProcessingResult processVVacancySearchBatch(LoadContext<VVacancySearch> baseCtx, int batchSize) {
    int totalProcessed = 0;
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;
    int first = 0;
    List<String> errors = new ArrayList<>();

    // Сначала посчитаем общее количество записей для обработки
    LoadContext<VVacancySearch> countCtx = (LoadContext<VVacancySearch>) baseCtx.copy();
    long totalCount = dataManager.getCount(countCtx);
    log.info("Total VVacancySearch records matching filters: {}", totalCount);

    while (true) {
      // Копируем контекст для текущей страницы с СОХРАНЕНИЕМ всех фильтров
      @SuppressWarnings("unchecked")
      LoadContext<VVacancySearch> pageCtx = (LoadContext<VVacancySearch>) baseCtx.copy();

      // Применяем пагинацию ПОВЕРХ фильтров
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<VVacancySearch> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) {
        break;
      }

      log.info("Processing VVacancySearch batch {}-{} of {}",
          first + 1,
          Math.min(first + page.size(), totalCount),
          totalCount);

      // Собираем ID вакансий из VVacancySearch
      List<String> vacancyIds = page.stream()
          .map(VVacancySearch::getId)
          .collect(Collectors.toList());

      log.debug("Batch vacancy IDs: {}", vacancyIds);

      // Загружаем все анализы для батча вакансий одним запросом
      Map<String, List<VacancyLlmAnalysis>> analysesMap = loadAnalysesForVacancies(vacancyIds);

      // Обрабатываем каждую вакансию
      SaveContext saveCtx = new SaveContext();
      for (VVacancySearch vVacancySearch : page) {
        String vacancyId = vVacancySearch.getId();
        totalProcessed++;

        try {
          List<VacancyLlmAnalysis> analyses = analysesMap.get(vacancyId);
          if (analyses == null || analyses.isEmpty()) {
            log.debug("No completed analysis found for vacancy {}, skipping", vacancyId);
            skippedCount++;
            continue;
          }

          // Рассчитываем оценку БЕЗ загрузки полной вакансии
          VacancyScore score = calculateScoreForVacancyId(vacancyId, analyses);
          saveCtx.saving(score);

          log.debug("Prepared score for vacancy {}: {} points ({})",
              vacancyId, score.getTotalScore(), score.getRating());
        } catch (Exception e) {
          failedCount++;
          String errorMsg = String.format("Error processing vacancy %s: %s", vacancyId, e.getMessage());
          log.error(errorMsg, e);
          errors.add(errorMsg);
        }
      }

      // Сохраняем батч оценок
      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        try {
          dataManager.save(saveCtx);
          int batchSaved = saveCtx.getEntitiesToSave().size();
          successCount += batchSaved;
          log.info("Successfully saved {} scores in current batch (progress: {}/{})",
              batchSaved, first + page.size(), totalCount);
        } catch (Exception e) {
          failedCount += saveCtx.getEntitiesToSave().size();
          String errorMsg = "Error saving batch: " + e.getMessage();
          log.error(errorMsg, e);
          errors.add(errorMsg);
        }
      }

      first += page.size();
    }

    BatchProcessingResult result = new BatchProcessingResult(
        totalProcessed, successCount, failedCount, skippedCount, errors);
    log.info("Batch scoring from VVacancySearch completed. {}", result);
    return result;
  }

  /**
   * Загружает все анализы для списка вакансий одним запросом
   * ВАЖНО: загружает vacancy.id с помощью fetchPlan, чтобы избежать lazy loading
   */
  private Map<String, List<VacancyLlmAnalysis>> loadAnalysesForVacancies(List<String> vacancyIds) {
    if (vacancyIds.isEmpty()) {
      return Map.of();
    }

    // Загружаем анализы с минимальным fetchPlan для vacancy (только ID)
    List<VacancyLlmAnalysis> allAnalyses = dataManager.load(VacancyLlmAnalysis.class)
        .query("select e from jb2_VacancyLlmAnalysis e " +
            "where e.vacancy.id in :vacancyIds " +
            "and e.status = :status " +
            "and e.analyzeData is not null")
        .parameter("vacancyIds", vacancyIds)
        .parameter("status", VacancyLlmAnalysisStatus.DONE.getId())
        .fetchPlan(fetchPlans.builder(VacancyLlmAnalysis.class)
            .addAll("analyzeType", "analyzeData", "status")
            .add("vacancy", builder -> builder.addAll("id"))
            .build())
        .list();

    // Группируем анализы по vacancy ID (теперь БЕЗ дополнительных запросов)
    return allAnalyses.stream()
        .collect(Collectors.groupingBy(
            analysis -> (String) EntityValues.getId(analysis.getVacancy())
        ));
  }

  /**
   * Рассчитывает оценку для одной вакансии на основе ее анализов
   * Использует только ID вакансии, не загружая полный объект
   */
  private VacancyScore calculateScoreForVacancyId(String vacancyId, List<VacancyLlmAnalysis> analyses) {
    // Рассчитываем оценку с описаниями
    VacancyScoringResult scoringResult = vacancyScorer.calculateScore(analyses);
    log.debug("Calculated score: {} for vacancy {}", scoringResult.getTotalScore(), vacancyId);

    // Определяем рейтинг
    VacancyScoreRating rating = determineRating(scoringResult.getTotalScore());

    // Генерируем детерминированный UUID
    UUID scoreId = uuidGenerator.generateUuid(vacancyId, "vacancy_score");

    // Пытаемся найти существующую запись
    Optional<VacancyScore> existingScore = dataManager.load(VacancyScore.class)
        .id(scoreId)
        .optional();

    VacancyScore vacancyScore;
    if (existingScore.isPresent()) {
      vacancyScore = existingScore.get();
      log.debug("Updating existing score record with UUID {}", scoreId);
    } else {
      vacancyScore = dataManager.create(VacancyScore.class);
      vacancyScore.setId(scoreId);

      // Создаем reference на вакансию БЕЗ загрузки полного объекта
      Vacancy vacancyRef = dataManager.getReference(Vacancy.class, vacancyId);
      vacancyScore.setVacancy(vacancyRef);

      log.debug("Creating new score record with UUID {}", scoreId);
    }

    // Устанавливаем значения
    vacancyScore.setTotalScore(scoringResult.getTotalScore());
    vacancyScore.setRating(rating);
    vacancyScore.setVersion(1); // Версия алгоритма

    // Конвертируем списки факторов в строки
    vacancyScore.setPositiveDescription(factorsToString(scoringResult.getPositiveFactors()));
    vacancyScore.setNegativeDescription(factorsToString(scoringResult.getNegativeFactors()));

    return vacancyScore;
  }

  /**
   * Рассчитывает оценку для одной вакансии на основе ее анализов
   * @deprecated Используйте calculateScoreForVacancyId для лучшей производительности
   */
  private VacancyScore calculateScoreForVacancy(Vacancy vacancy, List<VacancyLlmAnalysis> analyses) {
    String vacancyId = (String) EntityValues.getId(vacancy);
    return calculateScoreForVacancyId(vacancyId, analyses);
  }

  /**
   * Рассчитывает оценку вакансии и сохраняет результат в базу данных
   * Использует детерминированный UUID, поэтому при повторном вызове обновит существующую запись
   *
   * @param vacancyId ID вакансии для расчета оценки
   * @return сохраненная оценка вакансии
   * @throws RuntimeException если не найдена вакансия или отсутствуют результаты анализа
   */
  @Transactional
  public VacancyScore calculateAndSave(String vacancyId) {
    log.info("Starting score calculation for vacancy: {}", vacancyId);

    try {
      // Проверяем существование вакансии
      boolean vacancyExists = dataManager.load(Vacancy.class)
          .id(vacancyId)
          .optional()
          .isPresent();

      if (!vacancyExists) {
        String message = "Vacancy not found: " + vacancyId;
        log.error(message);
        throw new RuntimeException(message);
      }

      // Загружаем все результаты LLM анализа для вакансии с оптимальным fetchPlan
      List<VacancyLlmAnalysis> analyses = dataManager.load(VacancyLlmAnalysis.class)
          .query("select e from jb2_VacancyLlmAnalysis e where e.vacancy.id = :vacancyId " +
              "and e.status = :status and e.analyzeData is not null")
          .parameter("vacancyId", vacancyId)
          .parameter("status", VacancyLlmAnalysisStatus.DONE.getId())
          .fetchPlan(fetchPlans.builder(VacancyLlmAnalysis.class)
              .addAll("analyzeType", "analyzeData", "status")
              .build())
          .list();

      if (analyses.isEmpty()) {
        String message = "No completed analysis found for vacancy " + vacancyId;
        log.error(message);
        throw new RuntimeException(message);
      }

      log.info("Found {} analysis results for vacancy {}", analyses.size(), vacancyId);

      // Используем оптимизированный метод для расчета
      VacancyScore vacancyScore = calculateScoreForVacancyId(vacancyId, analyses);

      // Сохраняем в базу данных
      VacancyScore saved = dataManager.save(vacancyScore);

      log.info("Successfully saved score for vacancy {}: {} points ({}), positive: {}, negative: {}",
          vacancyId,
          saved.getTotalScore(),
          saved.getRating(),
          saved.getPositiveDescription() != null ? saved.getPositiveDescription().length() : 0,
          saved.getNegativeDescription() != null ? saved.getNegativeDescription().length() : 0);

      return saved;
    } catch (Exception e) {
      log.error("Error calculating score for vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw new RuntimeException("Failed to calculate and save vacancy score", e);
    }
  }

  /**
   * Получает существующую оценку вакансии
   *
   * @param vacancyId ID вакансии
   * @return Optional с оценкой или empty если оценка не найдена
   */
  @Transactional(readOnly = true)
  public Optional<VacancyScore> getScore(String vacancyId) {
    log.debug("Getting score for vacancy: {}", vacancyId);
    UUID scoreId = uuidGenerator.generateUuid(vacancyId, "vacancy_score");
    return dataManager.load(VacancyScore.class)
        .id(scoreId)
        .optional();
  }

  /**
   * Проверяет, существует ли оценка для вакансии
   *
   * @param vacancyId ID вакансии
   * @return true если оценка существует
   */
  @Transactional(readOnly = true)
  public boolean hasScore(String vacancyId) {
    return getScore(vacancyId).isPresent();
  }

  /**
   * Определяет рейтинг на основе итоговой оценки
   *
   * @param totalScore итоговая оценка
   * @return рейтинг вакансии
   */
  private VacancyScoreRating determineRating(int totalScore) {
    if (totalScore >= 300) return VacancyScoreRating.EXCELLENT;
    if (totalScore >= 200) return VacancyScoreRating.GOOD;
    if (totalScore >= 100) return VacancyScoreRating.MODERATE;
    if (totalScore >= 0) return VacancyScoreRating.POOR;
    return VacancyScoreRating.VERY_POOR;
  }

  /**
   * Конвертирует список факторов в строку для сохранения в БД
   * Использует разделитель " | " между факторами
   *
   * @param factors список факторов
   * @return строка с факторами или null если список пустой
   */
  private String factorsToString(List<String> factors) {
    if (factors == null || factors.isEmpty()) {
      return null;
    }
    return String.join(FACTOR_DELIMITER, factors);
  }
}
package ru.mindils.jb2.app.service.analysis;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.model.MetaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;

import java.util.List;

@Service
public class VacancyScoreUpdateService {

  private static final Logger log = LoggerFactory.getLogger(VacancyScoreUpdateService.class);

  private final DataManager dataManager;
  private final FetchPlans fetchPlans;
  private final Metadata metadata;
  private final AnalysisResultManager analysisResultManager;

  public VacancyScoreUpdateService(DataManager dataManager,
                                   FetchPlans fetchPlans,
                                   Metadata metadata,
                                   AnalysisResultManager analysisResultManager) {
    this.dataManager = dataManager;
    this.fetchPlans = fetchPlans;
    this.metadata = metadata;
    this.analysisResultManager = analysisResultManager;
  }

  /** Мутабельный результат одного прогонов/батча */
  public static class UpdateResult {
    public int processed;
    public int updated;
  }

  /** Пересчитать скор/рейтинг для одной записи по ID */
  @Transactional
  public boolean recalcOne(String vacancyId) {
    FetchPlan fp = fetchPlans.builder(VacancyAnalysis.class)
        .add("id")
        .add("stepResults")
        .add("finalScore")
        .add("rating")
        .add("analysisMetadata")
        .build();

    var opt = dataManager.load(VacancyAnalysis.class)
        .id(vacancyId)
        .fetchPlan(fp)
        .optional();

    if (opt.isEmpty()) {
      log.debug("VacancyAnalysis {} not found", vacancyId);
      return false;
    }

    VacancyAnalysis va = opt.get();
    if (va.getStepResults() == null) {
      log.debug("VacancyAnalysis {} has no stepResults — skip", vacancyId);
      return false;
    }

    Integer beforeScore = va.getFinalScore();
    String beforeRating = va.getRating();

    analysisResultManager.recalculateAndSaveScore(va);

    if (!equalsInt(beforeScore, va.getFinalScore()) || !equalsStr(beforeRating, va.getRating())) {
      dataManager.save(va);
      log.info("Recalculated score for {}: {} -> {}, rating {} -> {}",
          vacancyId, beforeScore, va.getFinalScore(), beforeRating, va.getRating());
      return true;
    }
    return false;
  }

  /** Пересчитать скор/рейтинг для батча */
  @Transactional
  public UpdateResult recalcScoresBatch(int offset, int batchSize) {
    FetchPlan fp = fetchPlans.builder(VacancyAnalysis.class)
        .add("id")
        .add("stepResults")
        .add("finalScore")
        .add("rating")
        .add("analysisMetadata")
        .build();

    MetaClass meta = metadata.getClass(VacancyAnalysis.class);
    LoadContext<VacancyAnalysis> ctx = new LoadContext<>(meta);

    LoadContext.Query q = new LoadContext.Query(
        "select e from jb2_VacancyAnalysis e " +
            "where e.stepResults is not null " +
            "order by e.lastModifiedDate desc"
    );
    q.setFirstResult(offset);
    q.setMaxResults(batchSize);

    ctx.setQuery(q);
    ctx.setFetchPlan(fp);

    List<VacancyAnalysis> batch = dataManager.loadList(ctx);

    UpdateResult result = new UpdateResult();
    result.processed = batch.size();

    int changed = 0;
    SaveContext saveCtx = new SaveContext();

    for (VacancyAnalysis va : batch) {
      Integer beforeScore = va.getFinalScore();
      String beforeRating = va.getRating();

      analysisResultManager.recalculateAndSaveScore(va);

      if (!equalsInt(beforeScore, va.getFinalScore()) || !equalsStr(beforeRating, va.getRating())) {
        saveCtx.saving(va);
        changed++;
      }
    }

    if (!saveCtx.getEntitiesToSave().isEmpty()) {
      dataManager.save(saveCtx);
    }

    result.updated = changed;

    log.info("Recalc batch offset={}, size={} -> processed={}, updated={}",
        offset, batchSize, result.processed, result.updated);

    return result;
  }

  /** Пересчитать все (страницами) */
  @Transactional
  public UpdateResult recalcScoresAll(int batchSize) {
    UpdateResult total = new UpdateResult();
    int offset = 0;

    while (true) {
      UpdateResult part = recalcScoresBatch(offset, batchSize);
      total.processed += part.processed;
      total.updated += part.updated;

      if (part.processed < batchSize) break; // последняя страница
      offset += batchSize;
    }

    log.info("Recalc ALL done: processed={}, updated={}", total.processed, total.updated);
    return total;
  }

  // helpers
  private boolean equalsInt(Integer a, Integer b) {
    return a == null ? b == null : a.equals(b);
  }

  private boolean equalsStr(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }
}

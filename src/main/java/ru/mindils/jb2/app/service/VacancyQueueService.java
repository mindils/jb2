package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.model.CollectionLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VacancyQueueService {

  private final DataManager dataManager;
  private final FetchPlans fetchPlans;

  public VacancyQueueService(DataManager dataManager, FetchPlans fetchPlans) {
    this.dataManager = dataManager;
    this.fetchPlans = fetchPlans;
  }

  /**
   * Удобный оверлоад: берёт условия у лоадера (фильтры/сортировки), игнорирует пагинацию,
   * и добавляет ВСЕ подходящие вакансии в очередь указанного типа.
   */
  @Transactional
  public int enqueueAllForUpdate(CollectionLoader<Vacancy> loader,
                                 AnalysisType type,
                                 int batchSize) {
    LoadContext<Vacancy> base = loader.createLoadContext();
    base.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());
    return enqueueAllForUpdate(base, type, batchSize);
  }

  /**
   * Основной метод: батчами грузит вакансии по baseCtx и ставит в JB2_VACANCY_ANALYSIS_QUEUE без дублей
   * (по паре (vacancy_id, type_queue)).
   *
   * @param baseCtx   LoadContext с уже заданными JPQL/параметрами/сортировкой
   * @param type      тип очереди (PRIMARY/SOCIAL/…)
   * @param batchSize размер батча загрузки
   * @return количество добавленных записей
   */
  @Transactional
  public int enqueueAllForUpdate(LoadContext<Vacancy> baseCtx,
                                 AnalysisType type,
                                 int batchSize) {
    int totalEnqueued = 0;
    int first = 0;

    // гарантируем лёгкую загрузку (только id)
    baseCtx.setFetchPlan(fetchPlans.builder(Vacancy.class).add("id").build());

    while (true) {
      @SuppressWarnings("unchecked")
      LoadContext<Vacancy> pageCtx = (LoadContext<Vacancy>) baseCtx.copy();
      pageCtx.getQuery().setFirstResult(first).setMaxResults(batchSize);

      List<Vacancy> page = dataManager.loadList(pageCtx);
      if (page.isEmpty()) break;

      List<Object> vacancyIds = page.stream()
          .map(EntityValues::getId)
          .collect(Collectors.toList());

      // Существующие записи в очереди для этого типа (чтобы не плодить дубли)
      Set<Object> existed = findExistingQueuedIds(vacancyIds, type.getId());

      SaveContext saveCtx = new SaveContext();
      for (Vacancy v : page) {
        Object id = EntityValues.getId(v);
        if (existed.contains(id)) continue;

        VacancyAnalysisQueue q = dataManager.create(VacancyAnalysisQueue.class);
        q.setVacancy(dataManager.getReference(Vacancy.class, id)); // без доп. загрузки сущности
        q.setTypeQueue(type);
        q.setProcessing(Boolean.FALSE);
        saveCtx.saving(q);
      }

      if (!saveCtx.getEntitiesToSave().isEmpty()) {
        dataManager.save(saveCtx);
        totalEnqueued += saveCtx.getEntitiesToSave().size();
      }

      first += page.size();
    }

    return totalEnqueued;
  }

  private Set<Object> findExistingQueuedIds(List<Object> vacancyIds, String typeId) {
    if (vacancyIds.isEmpty()) return Set.of();

    ValueLoadContext vlc = ValueLoadContext.create()
        .setQuery(new ValueLoadContext.Query(
            "select q.vacancy.id as vacancyId " +
                "from jb2_VacancyAnalysisQueue q " +
                "where q.typeQueue = :type and q.vacancy.id in :ids"))
        .addProperty("vacancyId");
    vlc.getQuery().setParameter("type", typeId);
    vlc.getQuery().setParameter("ids", vacancyIds);

    List<KeyValueEntity> rows = dataManager.loadValues(vlc);
    return rows.stream()
        .map(kv -> kv.getValue("vacancyId"))
        .collect(Collectors.toSet());
  }
}

package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.VacancySyncState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class VacancyOpsService {

  private final DataManager dataManager;

  public VacancyOpsService(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  /** Кол-во записей в очереди указанного типа */
  public int getQueueCount(AnalysisType type) {
    return dataManager.loadValue("""
        select count(e)
        from jb2_VacancyAnalysisQueue e
        where e.typeQueue = :type and e.processing = true
        """, Integer.class)
        .parameter("type", type)
        .one();
  }

  public int getPrimaryQueueCount() {
    return getQueueCount(AnalysisType.PRIMARY);
  }

  public int getSocialQueueCount() {
    return getQueueCount(AnalysisType.SOCIAL);
  }

  public int getUpdateQueueCount() {
    return getQueueCount(AnalysisType.VACANCY_UPDATE);
  }

  /** Последний момент успешного sync-сейва */
  public LocalDateTime getLastSyncTime() {
    List<VacancySyncState> list = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e order by e.lastModifiedDate desc")
        .maxResults(1)
        .list();
    return list.isEmpty() ? null : list.getFirst().getUpdateDate();
  }

  /** Дней с последнего sync (1..30, с «страховкой» по краям) */
  public int calcDaysSinceLastSyncClamped() {
    LocalDateTime last = getLastSyncTime();
    if (last == null) return 30;
    long days = ChronoUnit.DAYS.between(last, LocalDate.now());
    if (days < 1) return 1;
    if (days > 30) return 30;
    return (int) days;
  }
}

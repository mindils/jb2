package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.VacancySyncState;
import ru.mindils.jb2.app.entity.VacancySyncStateType;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VacancySyncStateService {

  private final DataManager dataManager;

  public VacancySyncStateService(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  @Transactional
  public void updateSyncState() {
    List<VacancySyncState> vacancySyncStates = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e where e.stateType = :stateType")
        .parameter("stateType", VacancySyncStateType.ALL_SYNC)
        .maxResults(1)
        .list();

    if (vacancySyncStates.isEmpty()) {
      VacancySyncState vacancySyncState = dataManager.create(VacancySyncState.class);
      vacancySyncState.setStateType(VacancySyncStateType.ALL_SYNC);
      vacancySyncState.setUpdateDate(LocalDateTime.now());
      dataManager.save(vacancySyncState);
      return;
    }

    VacancySyncState first = vacancySyncStates.getFirst();
    first.setUpdateDate(LocalDateTime.now());
    first.setStateType(VacancySyncStateType.ALL_SYNC);

    dataManager.save(first);
  }

}

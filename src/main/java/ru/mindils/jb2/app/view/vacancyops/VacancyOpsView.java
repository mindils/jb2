package ru.mindils.jb2.app.view.vacancyops;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.dto.WorkflowInfo;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.VacancyAnalysisQueue;
import ru.mindils.jb2.app.entity.VacancySyncState;
import ru.mindils.jb2.app.service.TemporalStatusService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Route(value = "vacancy-ops-view", layout = MainView.class)
@ViewController(id = "jb2_VacancyOpsView")
@ViewDescriptor(path = "vacancy-ops-view.xml")
public class VacancyOpsView extends StandardView {

  private static final Logger log = LoggerFactory.getLogger(VacancyOpsView.class);

  @Autowired
  private VacancyWorkflowService vacancyWorkflowService;

  @Autowired
  private TemporalStatusService temporalStatusService;

  @Autowired
  private Notifications notifications;

  @ViewComponent
  private H3 workflowStatusHeader;

  @ViewComponent
  private CollectionLoader<WorkflowInfo> workflowInfoesDl;

  @ViewComponent
  private CollectionContainer<WorkflowInfo> workflowInfoesDc;

  @Autowired
  private DataManager dataManager;

  @ViewComponent
  private Paragraph lastTimeUpdateText;

  @ViewComponent
  private TextField daysPeriodField;
  @ViewComponent
  private Paragraph queueText;

  @Subscribe
  public void onInit(final InitEvent event) {
    refreshWorkflowStatus();
  }

  @Subscribe(id = "updateAllVacanciesBtn", subject = "clickListener")
  public void onUpdateAllVacanciesBtnClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.sync();

    notifications.create("Полное обновление запущено")
        .withType(Notifications.Type.SUCCESS)
        .show();


    refreshWorkflowStatus();
  }

  @Subscribe(id = "updateFromLastLoadBtn", subject = "clickListener")
  public void onUpdateFromLastLoadBtnClick(final ClickEvent<JmixButton> event) {
    // Вычисляем период с последней загрузки (1..30)
    int daysPeriod = calcDaysSinceLastSyncClamped();

    List<Map<String, String>> params = List.of(Map.of("period", String.valueOf(daysPeriod)));
    vacancyWorkflowService.sync(params);

    notifications.create(String.format("Запущено обновление за последние %d дней", daysPeriod))
        .withType(Notifications.Type.SUCCESS)
        .show();

    refreshWorkflowStatus();
  }

  @Subscribe(id = "updateByDaysBtn", subject = "clickListener")
  public void onUpdateByDaysBtnClick(final ClickEvent<JmixButton> event) {
    int days = clampDays(parseIntOrDefault(daysPeriodField.getValue(), 3));

    // Заглушка — чтобы подключить логику, раскомментируйте 2 строки ниже:
    List<Map<String, String>> params = List.of(Map.of("period", String.valueOf(days)));
    vacancyWorkflowService.sync(params);

    notifications.create(String.format("Заглушка: будет обновление за %d дней", days))
        .withType(Notifications.Type.SUCCESS)
        .show();

    // Если понадобится — можно обновлять статус
    refreshWorkflowStatus();
  }

  @Subscribe(id = "refreshProcessesBtn", subject = "clickListener")
  public void onRefreshProcessesBtnClick(final ClickEvent<JmixButton> event) {
    workflowInfoesDl.load();
    updateHeaderCount();
  }

  @Install(to = "workflowInfoesDl", target = Target.DATA_LOADER)
  private List<WorkflowInfo> workflowInfoesDlLoadDelegate(final LoadContext<WorkflowInfo> loadContext) {
    return temporalStatusService.getActiveWorkflows();
  }

  /* ===================== helpers & small refactors ===================== */

  private void refreshWorkflowStatus() {
    try {
      workflowInfoesDl.load();
      updateLastTimeUpdateVacancy();
      updateHeaderCount();
      updateQueueCount();
    } catch (Exception e) {
      notifications.create("Ошибка при получении статуса workflow'ов: " + e.getMessage())
          .withType(Notifications.Type.ERROR)
          .show();
    }
  }

  private void updateQueueCount() {
    Integer count = dataManager.loadValue("select count(e) from jb2_VacancyAnalysisQueue e where e.typeQueue = :typeQueue and e.processing = :processing", Integer.class)
        .parameter("typeQueue", AnalysisType.VACANCY_UPDATE)
        .parameter("processing", Boolean.TRUE)
        .one();
    queueText.setText("В очереди: " + count);
  }

  private void updateHeaderCount() {
    if (workflowStatusHeader == null) return;
    int active = workflowInfoesDc.getItems() == null ? 0 : workflowInfoesDc.getItems().size();
    String headerText = active == 0
        ? "Запущенные процессы (нет активных)"
        : String.format("Запущенные процессы (%d активных)", active);
    workflowStatusHeader.setText(headerText);
  }

  private void updateLastTimeUpdateVacancy() {
    List<VacancySyncState> list = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e order by e.lastModifiedDate desc")
        .maxResults(1)
        .list();

    if (list.isEmpty()) {
      lastTimeUpdateText.setText("Обновлений не было");
    } else {
      lastTimeUpdateText.setText("Последнее обновление: " + list.get(0).getLastModifiedDate());
    }
  }

  private int calcDaysSinceLastSyncClamped() {
    List<VacancySyncState> list = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e order by e.lastModifiedDate desc")
        .maxResults(1)
        .list();

    if (list.isEmpty()) {
      log.info("No sync state found, using maximum period: 30 days");
      return 30;
    }

    OffsetDateTime lastModified = list.get(0).getLastModifiedDate();
    long daysBetween = ChronoUnit.DAYS.between(lastModified.toLocalDate(), LocalDate.now());
    int period = (int) Math.max(1, Math.min(30, daysBetween));
    log.info("Last sync was {} days ago, using period: {} days", daysBetween, period);
    return period;
  }

  private static int parseIntOrDefault(String value, int def) {
    try {
      return Integer.parseInt(value == null ? "" : value.trim());
    } catch (Exception e) {
      return def;
    }
  }

  private static int clampDays(int days) {
    if (days < 1) return 1;
    if (days > 30) return 30;
    return days;
  }

  @Subscribe(id = "updateFromQueueBtnClick", subject = "clickListener")
  public void onUpdateFromQueueBtnClickClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.updateFromQueue();
    notifications.create("Запущен workflow обновления вакансий из очереди").show();
  }
}

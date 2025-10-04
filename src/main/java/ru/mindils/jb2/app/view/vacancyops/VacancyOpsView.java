package ru.mindils.jb2.app.view.vacancyops;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.dto.WorkflowInfo;
import ru.mindils.jb2.app.service.TemporalStatusService;
import ru.mindils.jb2.app.service.VacancyOpsService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;


import java.time.LocalDateTime;
import java.util.List;

@Route(value = "vacancy-ops-view", layout = MainView.class)
@ViewController(id = "jb2_VacancyOpsView")
@ViewDescriptor(path = "vacancy-ops-view.xml")
public class VacancyOpsView extends StandardView {

  /* ===== Services ===== */
  @Autowired
  private VacancyWorkflowService vacancyWorkflowService;

  @Autowired
  private TemporalStatusService temporalStatusService;

  @Autowired
  private VacancyOpsService vacancyOpsService;

  @Autowired
  private Notifications notifications;


  @ViewComponent private Paragraph updateQueueCountText;
  @ViewComponent private Paragraph lastSyncText;

  @ViewComponent private TextField daysField;

  @ViewComponent private H3 workflowStatusHeader;

  @ViewComponent
  private CollectionLoader<WorkflowInfo> workflowInfosDl;

  @ViewComponent private CollectionContainer<WorkflowInfo> workflowInfosDc;

  @Subscribe
  public void onInit(final InitEvent event) {
    refreshAll();
  }

  /* ===== Data loader ===== */
  @Install(to = "workflowInfosDl", target = Target.DATA_LOADER)
  private List<WorkflowInfo> loadWorkflows(final io.jmix.core.LoadContext<WorkflowInfo> ctx) {
    return temporalStatusService.getActiveWorkflows();
  }

  @Subscribe(id = "syncAllBtn", subject = "clickListener")
  public void onSyncAllBtnClick(final ClickEvent<JmixButton> e) {
    vacancyWorkflowService.sync();
    notifications.create("Полная синхронизация запущена").withType(Notifications.Type.SUCCESS).show();
    refreshAll();
  }

  @Subscribe(id = "syncRecentBtn", subject = "clickListener")
  public void onSyncRecentBtnClick(final ClickEvent<JmixButton> e) {
    int days = parseDaysOrCompute();
    vacancyWorkflowService.sync(List.of(java.util.Map.of("period", String.valueOf(days))));
    notifications.create("Синхронизация за последние " + days + " дней запущена")
        .withType(Notifications.Type.SUCCESS).show();
    refreshAll();
  }

  @Subscribe(id = "refreshBtn", subject = "clickListener")
  public void onRefreshBtnClick(final ClickEvent<JmixButton> e) {
    refreshAll();
  }

  private void refreshAll() {
    refreshStats();
    workflowInfosDl.load();
    updateWorkflowsHeader();
  }

  private void refreshStats() {
    // Очереди
    updateQueueCountText.setText(String.valueOf(vacancyOpsService.getUpdateQueueCount()));

    // Последний sync
    LocalDateTime last = vacancyOpsService.getLastSyncTime();
    lastSyncText.setText(last == null
        ? "Нет данных"
        : last.toString());
  }

  private void updateWorkflowsHeader() {
    int active = workflowInfosDc.getItems() == null ? 0 : workflowInfosDc.getItems().size();
    workflowStatusHeader.setText(active == 0
        ? "Запущенные процессы (нет активных)"
        : "Запущенные процессы (" + active + " активных)");
  }

  private int parseDaysOrCompute() {
    try {
      String v = daysField.getValue();
      if (v != null && !v.isBlank()) {
        int d = Integer.parseInt(v.trim());
        if (d < 1) return 1;
        if (d > 30) return 30;
        return d;
      }
    } catch (Exception ignored) { }
    return vacancyOpsService.calcDaysSinceLastSyncClamped();
  }

}

package ru.mindils.jb2.app.view.vacancyops;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.dto.WorkflowInfo;
import ru.mindils.jb2.app.entity.VacancySyncState;
import ru.mindils.jb2.app.entity.VacancySyncStateType;
import ru.mindils.jb2.app.service.TemporalStatusService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Route(value = "vacancy-ops-view", layout = MainView.class)
@ViewController(id = "jb2_VacancyOpsView")
@ViewDescriptor(path = "vacancy-ops-view.xml")
public class VacancyOpsView extends StandardView {

  Logger log = LoggerFactory.getLogger(VacancyOpsView.class);

  @Autowired
  private VacancyWorkflowService vacancyWorkflowService;

  @Autowired
  private TemporalStatusService temporalStatusService;

  @Autowired
  private Notifications notifications;

  @ViewComponent
  private Div workflowStatusContainer;

  @ViewComponent
  private H3 workflowStatusHeader;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
  @ViewComponent
  private CollectionLoader<WorkflowInfo> workflowInfoesDl;
  @ViewComponent
  private CollectionContainer<WorkflowInfo> workflowInfoesDc;
  @Autowired
  private DataManager dataManager;
  @ViewComponent
  private Paragraph lastTimeUpdateText;

  @Subscribe
  public void onInit(final InitEvent event) {
//    initWorkflowGrid();
    refreshWorkflowStatus();
  }

  @Subscribe(id = "updateAllVacanciesBtn", subject = "clickListener")
  public void onUpdateAllVacanciesBtnClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.sync();

    notifications
        .create("Полное обновление запущено")
        .withType(Notifications.Type.SUCCESS)
        .show();

    refreshWorkflowStatus();
  }

//  @Subscribe(id = "refreshStatusBtn", subject = "clickListener")
//  public void onRefreshStatusBtnClick(final ClickEvent<JmixButton> event) {

  /// /    refreshWorkflowStatus();
//    notifications
//        .create("Статус обновлен")
//        .withType(Notifications.Type.SUCCESS)
//        .show();
//  }

//  private void initWorkflowGrid() {
//    if (workflowGrid != null) {
//      workflowGrid.addColumn(TemporalStatusService.WorkflowInfo::getDescription)
//          .setHeader("Процесс")
//          .setAutoWidth(true)
//          .setFlexGrow(1);
//
//      workflowGrid.addColumn(TemporalStatusService.WorkflowInfo::getStatus)
//          .setHeader("Статус")
//          .setAutoWidth(true);
//
//      workflowGrid.addColumn(info -> info.getStartTime() != null ?
//              info.getStartTime().format(DATE_FORMATTER) : "—")
//          .setHeader("Время запуска")
//          .setAutoWidth(true);
//
//      workflowGrid.addColumn(TemporalStatusService.WorkflowInfo::getWorkflowId)
//          .setHeader("ID")
//          .setAutoWidth(true)
//          .setFlexGrow(0);
//    }
//  }
  private void refreshWorkflowStatus() {
    try {
      workflowInfoesDl.load();

      updateLastTimeUpdateVacancy();
      // Обновляем текст заголовка с количеством активных процессов
      if (workflowStatusHeader != null) {
        String headerText = workflowInfoesDc.getItems().isEmpty()
            ? "Запущенные процессы (нет активных)"
            : String.format("Запущенные процессы (%d активных)", workflowInfoesDc.getItems().size());
        workflowStatusHeader.setText(headerText);
      }

      // Добавляем дополнительную информацию о конкретных workflow'ах
      updateSpecificWorkflowStatus();

    } catch (Exception e) {
      notifications
          .create("Ошибка при получении статуса workflow'ов: " + e.getMessage())
          .withType(Notifications.Type.ERROR)
          .show();
    }
  }

  private void updateLastTimeUpdateVacancy() {
    List<VacancySyncState> list = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e order by e.lastModifiedDate desc")
        .maxResults(1)
        .list();

    if (list.isEmpty()) {
      lastTimeUpdateText.setText("Обновлений не было");
      return;
    }

    lastTimeUpdateText.setText(list.getFirst().getLastModifiedDate().toString());
  }

  private void updateSpecificWorkflowStatus() {
    if (workflowStatusContainer == null) {
      return;
    }

    // Очищаем контейнер
    workflowStatusContainer.removeAll();

    // Проверяем статус основных workflow'ов
    boolean syncRunning = temporalStatusService.isWorkflowRunning("VACANCY_SYNK");
    boolean primaryAnalysisRunning = temporalStatusService.isWorkflowRunning("VACANCY_ANALYSIS_PRIMARY");
    boolean socialAnalysisRunning = temporalStatusService.isWorkflowRunning("VACANCY_ANALYSIS_SOCIAL");

    // Добавляем статусы
    addStatusSpan("Синхронизация вакансий: ", syncRunning ? "ЗАПУЩЕН" : "ОСТАНОВЛЕН", syncRunning);
    addStatusSpan("Первичный анализ: ", primaryAnalysisRunning ? "ЗАПУЩЕН" : "ОСТАНОВЛЕН", primaryAnalysisRunning);
    addStatusSpan("Социальный анализ: ", socialAnalysisRunning ? "ЗАПУЩЕН" : "ОСТАНОВЛЕН", socialAnalysisRunning);
  }

  private void addStatusSpan(String label, String status, boolean isRunning) {
    if (workflowStatusContainer == null) {
      return;
    }

    Div statusDiv = new Div();
    statusDiv.addClassName("workflow-status-item");
    statusDiv.getStyle().set("margin", "5px 0");

    Span labelSpan = new Span(label);
    labelSpan.getStyle().set("font-weight", "bold");

    Span statusSpan = new Span(status);
    if (isRunning) {
      statusSpan.getStyle()
          .set("color", "green")
          .set("font-weight", "bold");
    } else {
      statusSpan.getStyle()
          .set("color", "gray");
    }

    statusDiv.add(labelSpan, statusSpan);
    workflowStatusContainer.add(statusDiv);
  }

  @Subscribe(id = "refreshProcessesBtn", subject = "clickListener")
  public void onRefreshProcessesBtnClick(final ClickEvent<JmixButton> event) {
    workflowInfoesDl.load();
  }

  @Install(to = "workflowInfoesDl", target = Target.DATA_LOADER)
  private List<WorkflowInfo> workflowInfoesDlLoadDelegate(final LoadContext<WorkflowInfo> loadContext) {
    return temporalStatusService.getActiveWorkflows();
  }

  @Subscribe(id = "updateFromLastLoadBtn", subject = "clickListener")
  public void onUpdateFromLastLoadBtnClick(final ClickEvent<JmixButton> event) {
    // Получаем последнюю дату обновления
    List<VacancySyncState> list = dataManager.load(VacancySyncState.class)
        .query("select e from jb2_VacancySyncState e order by e.lastModifiedDate desc")
        .maxResults(1)
        .list();

    int daysPeriod;

    if (list.isEmpty()) {
      // Если записей нет - используем максимальный period
      daysPeriod = 30;
      log.info("No sync state found, using maximum period: {} days", daysPeriod);
    } else {
      OffsetDateTime lastModified = list.getFirst().getLastModifiedDate();

      // Вычисляем разность в днях между последней датой обновления и текущим моментом
      long daysBetween = ChronoUnit.DAYS.between(
          lastModified.toLocalDate(),
          LocalDate.now()
      );

      // Ограничиваем значение между 1 и 30
      daysPeriod = (int) Math.max(1, Math.min(30, daysBetween));

      log.info("Last sync was {} days ago, using period: {} days", daysBetween, daysPeriod);
    }

    List<Map<String, String>> params = List.of(
        Map.of("period", String.valueOf(daysPeriod))
    );

    vacancyWorkflowService.sync(params);

    notifications
        .create(String.format("Запущено обновление за последние %d дней", daysPeriod))
        .withType(Notifications.Type.SUCCESS)
        .show();

    refreshWorkflowStatus();
  }
}
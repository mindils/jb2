package ru.mindils.jb2.app.view.vacancymanager;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.service.GenericTaskQueueService;
import ru.mindils.jb2.app.service.VacancyQueueProcessorWorkflowService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "vacancy-manager-view", layout = MainView.class)
@ViewController(id = "jb2_VacancyManagerView")
@ViewDescriptor(path = "vacancy-manager-view.xml")
public class VacancyManagerView extends StandardView {
  @Autowired
  private GenericTaskQueueService genericTaskQueueService;
  @Autowired
  private Notifications notifications;
  @ViewComponent
  private Paragraph primaryQueueCountText;
  @ViewComponent
  private Paragraph fullQueueCountText;

  @ViewComponent
  private Paragraph lastSyncText;

  @Autowired
  private VacancyWorkflowService vacancyWorkflowService;

  @Autowired
  private VacancyQueueProcessorWorkflowService vacancyQueueProcessorWorkflowService;
  @ViewComponent
  private TypedTextField<Object> daysField;
  @Autowired
  private DataManager dataManager;
  @ViewComponent
  private Paragraph updateQueueCountText;

  @Subscribe(id = "analyzePrimaryBtn", subject = "clickListener")
  public void onAnalyzePrimaryBtnClick(final ClickEvent<JmixButton> event) {
    vacancyQueueProcessorWorkflowService.startFirstAnalysisQueueProcessing();

    notifications.create("Обработка запущена")
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe
  public void onBeforeShow(final BeforeShowEvent event) {
    refreshStats();
  }

  @Subscribe(id = "enqueueFirstChainBtn", subject = "clickListener")
  public void onEnqueueFirstChainBtnClick(final ClickEvent<JmixButton> event) {
    int i = genericTaskQueueService.enqueueFirstLlmAnalysis();
    notifications.create("В очередь на первичный анализ поставлено %d вакансий".formatted(i))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  private void refreshStats() {
    // Количество вакансий Добавленных в очередь на первичную обработку
    primaryQueueCountText.setText(genericTaskQueueService.getCountByType(GenericTaskQueueType.LLM_FIRST).toString());
    fullQueueCountText.setText(genericTaskQueueService.getCountByType(GenericTaskQueueType.LLM_FULL).toString());

    // Количество вакансий в очереди
    updateQueueCountText.setText(genericTaskQueueService.getCountByType(GenericTaskQueueType.VACANCY_UPDATE).toString());

    // Последняя обновленная вакансия
    OffsetDateTime lastVacancyDate = dataManager.loadValue("""
            select e.lastModifiedDate from jb2_Vacancy e order by e.lastModifiedDate desc
            """, OffsetDateTime.class)
        .maxResults(1)
        .optional()
        .orElse(null);

    if (lastVacancyDate != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
      String formattedDate = lastVacancyDate.format(formatter);
      lastSyncText.setText(formattedDate);
    } else {
      lastSyncText.setText("Последняя обновленная вакансия: нет данных");
    }

  }

  @Subscribe(id = "enqueueFullChainBtn", subject = "clickListener")
  public void onEnqueueFullChainBtnClick(final ClickEvent<JmixButton> event) {
    int i = genericTaskQueueService.enqueueFullLlmAnalysis();
    notifications.create("В очередь на полный анализ поставлено %d вакансий".formatted(i))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "analyzeFullBtn", subject = "clickListener")
  public void onAnalyzeFullBtnClick(final ClickEvent<JmixButton> event) {
    vacancyQueueProcessorWorkflowService.startFullAnalysisQueueProcessing();

    notifications.create("Обработка запущена")
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "syncAllBtn", subject = "clickListener")
  public void onSyncAllBtnClick(final ClickEvent<JmixButton> e) {
    vacancyWorkflowService.sync();
    notifications.create("Полная синхронизация запущена").withType(Notifications.Type.SUCCESS).show();
    refreshStats();
  }

  @Subscribe(id = "syncRecentBtn", subject = "clickListener")
  public void onSyncRecentBtnClick(final ClickEvent<JmixButton> e) {
    int days = parseDaysOrCompute();
    vacancyWorkflowService.sync(List.of(Map.of("period", String.valueOf(days))));
    notifications.create("Синхронизация за последние " + days + " дней запущена")
        .withType(Notifications.Type.SUCCESS).show();
    refreshStats();
  }

  @Subscribe(id = "stopSyncBtn", subject = "clickListener")
  public void onStopSyncBtnClick(final ClickEvent<JmixButton> event) {
    boolean stopped = vacancyWorkflowService.stopSync();

    if (stopped) {
      notifications.create("Запрос на остановку синхронизации отправлен")
          .withType(Notifications.Type.SUCCESS)
          .show();
    } else {
      notifications.create("Не удалось остановить: синхронизация не запущена")
          .withType(Notifications.Type.WARNING)
          .show();
    }

    refreshStats();
  }

  private int parseDaysOrCompute() {
    String v = daysField.getValue();
    if (v != null && !v.isBlank()) {
      int d = Integer.parseInt(v.trim());
      if (d < 1) return 1;
      if (d > 30) return 30;
      return d;
    }
    return 1;
  }

  @Subscribe(id = "updateFromQueueBtn", subject = "clickListener")
  public void onUpdateFromQueueBtnClick(final ClickEvent<JmixButton> event) {
    vacancyQueueProcessorWorkflowService.startVacancyUpdateQueueProcessing();

    notifications.create("Обработка очереди обновления вакансий запущена")
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "stopUpdateQueueBtn", subject = "clickListener")
  public void onStopUpdateQueueBtnClick(final ClickEvent<JmixButton> event) {
    boolean stopped = vacancyQueueProcessorWorkflowService.stopVacancyUpdateQueueProcessing();

    if (stopped) {
      notifications.create("Запрос на остановку обработки очереди обновления отправлен")
          .withType(Notifications.Type.SUCCESS)
          .show();
    } else {
      notifications.create("Не удалось остановить: обработка очереди не запущена")
          .withType(Notifications.Type.WARNING)
          .show();
    }

    refreshStats();
  }

  @Subscribe(id = "refreshStatsBtn", subject = "clickListener")
  public void onRefreshStatsBtnClick(final ClickEvent<JmixButton> event) {
    refreshStats();
  }

}
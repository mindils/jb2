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
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.service.TemporalStatusService;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.service.VacancyChainQueueService;
import ru.mindils.jb2.app.service.VacancyOpsService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.service.analysis.VacancyScoreUpdateService;
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
  private VacancyAnalysisService vacancyAnalysisService;

  @Autowired

  private Notifications notifications;
  @Autowired
  private VacancyScoreUpdateService scoreUpdateService;


  @Autowired
  private VacancyChainQueueService chainQueueService;

  @ViewComponent private Paragraph updateQueueCountText;
  @ViewComponent private Paragraph primaryQueueCountText;
  @ViewComponent private Paragraph fullQueueCountText;
  @ViewComponent private Paragraph lastSyncText;

  @ViewComponent private Paragraph fullChainQueueCountText;
  @ViewComponent private Paragraph primaryChainQueueCountText;
  @ViewComponent private Paragraph socialTechnicalChainQueueCountText;

  /* ===== Quick actions ===== */
  @ViewComponent private TextField daysField;

  /* ===== Workflows grid ===== */
  @ViewComponent private H3 workflowStatusHeader;
  @ViewComponent private CollectionLoader<WorkflowInfo> workflowInfosDl;
  @ViewComponent private CollectionContainer<WorkflowInfo> workflowInfosDc;

  /* ===== Lifecycle ===== */
  @Subscribe
  public void onInit(final InitEvent event) {
    refreshAll();
  }

  /* ===== Data loader ===== */
  @Install(to = "workflowInfosDl", target = Target.DATA_LOADER)
  private List<WorkflowInfo> loadWorkflows(final io.jmix.core.LoadContext<WorkflowInfo> ctx) {
    return temporalStatusService.getActiveWorkflows();
  }

  /* ===== Buttons: Quick Actions ===== */
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

  @Subscribe(id = "analyzeSocialBtn", subject = "clickListener")
  public void onAnalyzeSocialBtnClick(final ClickEvent<JmixButton> e) {
    vacancyWorkflowService.analyze(AnalysisType.SOCIAL);
    notifications.create("Запущен воркфлоу социального анализа").show();
  }

  @Subscribe(id = "refreshBtn", subject = "clickListener")
  public void onRefreshBtnClick(final ClickEvent<JmixButton> e) {
    refreshAll();
  }

  @Subscribe(id = "updateFromQueueBtn", subject = "clickListener")
  public void onUpdateFromQueueBtnClick(final ClickEvent<JmixButton> e) {
    vacancyWorkflowService.updateFromQueue();
    notifications.create("Запущен workflow обновления из очереди").show();
    refreshAll();
  }

  @Subscribe(id = "enqueueNotAnalyzedBtn", subject = "clickListener")
  public void onEnqueueNotAnalyzedBtnClick(final ClickEvent<JmixButton> e) {
    int added = vacancyAnalysisService.enqueueNotAnalyzed();
    notifications.create("В очередь первичного анализа добавлено: " + added).show();
    refreshAll();
  }

  @Subscribe(id = "enqueuePrimaryAllBtn", subject = "clickListener")
  public void onEnqueuePrimaryAllBtnClick(final ClickEvent<JmixButton> e) {
    int count = vacancyAnalysisService.markProcessingForAllVacancy(AnalysisType.PRIMARY);
    notifications.create("В очередь первичного анализа добавлено: " + count).show();
    refreshAll();
  }

  @Subscribe(id = "enqueueSocialJavaBtn", subject = "clickListener")
  public void onEnqueueSocialJavaBtnClick(final ClickEvent<JmixButton> e) {
    int count = vacancyAnalysisService.markProcessingForJavaVacancy(AnalysisType.SOCIAL);
    notifications.create("В очередь социального анализа (Java) добавлено: " + count).show();
    refreshAll();
  }

  @Subscribe(id = "enqueueFirstChainBtn", subject = "clickListener")
  public void onEnqueueFirstChainBtnClick(final ClickEvent<JmixButton> e) {
    int added = chainQueueService.enqueueNotAnalyzedVacanciesNativeSql(ChainAnalysisType.PRIMARY_ONLY);
    notifications.create("В очередь полного анализа добавлено: " + added).show();
    refreshAll();
  }

  @Subscribe(id = "enqueueJavaFullChainBtn", subject = "clickListener")
  public void onEnqueueJavaFullChainBtnClick(final ClickEvent<JmixButton> e) {
    int added = chainQueueService.enqueueJavaVacanciesForChainAnalysis(ChainAnalysisType.SOCIAL_TECHNICAL, 1000);
    notifications.create("В очередь социально-технического анализа (Java) добавлено: " + added).show();
    refreshAll();
  }

  /* ===== Helpers ===== */
  private void refreshAll() {
    refreshStats();
    workflowInfosDl.load();
    updateWorkflowsHeader();
  }

  private void refreshStats() {
    // Очереди
    updateQueueCountText.setText(String.valueOf(vacancyOpsService.getUpdateQueueCount()));
    primaryQueueCountText.setText(String.valueOf(chainQueueService.getQueueCount(ChainAnalysisType.PRIMARY_ONLY)));
    fullQueueCountText.setText(String.valueOf(chainQueueService.getQueueCount(ChainAnalysisType.FULL_ANALYSIS)));

    fullChainQueueCountText.setText(String.valueOf(chainQueueService.getQueueCount(ChainAnalysisType.FULL_ANALYSIS)));
    primaryChainQueueCountText.setText(String.valueOf(chainQueueService.getQueueCount(ChainAnalysisType.PRIMARY_ONLY)));
    socialTechnicalChainQueueCountText.setText(String.valueOf(chainQueueService.getQueueCount(ChainAnalysisType.SOCIAL_TECHNICAL)));


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

  @Subscribe(id = "calculateScoreBtn", subject = "clickListener")
  public void onCalculateScoreBtnClick(final ClickEvent<JmixButton> event) {
    scoreUpdateService.recalcScoresAll(100);
  }

  @Subscribe(id = "enqueueFullChainBtn", subject = "clickListener")
  public void onEnqueueFullChainBtnClick(final ClickEvent<JmixButton> event) {
    int added = chainQueueService.enqueueNotAnalyzedVacanciesNativeSql(ChainAnalysisType.FULL_ANALYSIS);
    notifications.create("В очередь полного анализа добавлено: " + added).show();
    refreshAll();
  }

}

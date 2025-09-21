package ru.mindils.jb2.app.view.chainanalysismonitor;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.service.VacancyChainQueueService;
import ru.mindils.jb2.app.service.VacancyChainWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "chain-analysis-monitor", layout = MainView.class)
@ViewController(id = "jb2_ChainAnalysisMonitorView")
@ViewDescriptor(path = "chain-analysis-monitor-view.xml")
public class ChainAnalysisMonitorView extends StandardView {

  @Autowired private VacancyChainWorkflowService chainWorkflowService;
  @Autowired private VacancyChainQueueService chainQueueService;
  @Autowired private Notifications notifications;

  @ViewComponent private Paragraph fullChainCountText;
  @ViewComponent private Paragraph primaryChainCountText;
  @ViewComponent private Paragraph socialTechnicalChainCountText;

  @ViewComponent private H3 chainStatusHeader;

  @Subscribe
  public void onInit(final InitEvent event) {
    refreshStats();
  }

  @Subscribe(id = "refreshBtn", subject = "clickListener")
  public void onRefreshBtnClick(final ClickEvent<JmixButton> e) {
    refreshStats();
  }

  // Chain workflow controls
  @Subscribe(id = "startFullChainBtn", subject = "clickListener")
  public void onStartFullChainBtnClick(final ClickEvent<JmixButton> e) {
    try {
      chainWorkflowService.startFullAnalysis();
      notifications.create("Запущен полный цепочный анализ")
          .withType(Notifications.Type.SUCCESS).show();
    } catch (IllegalStateException ex) {
      notifications.create("Ошибка: " + ex.getMessage())
          .withType(Notifications.Type.ERROR).show();
    }
    refreshStats();
  }

  @Subscribe(id = "startPrimaryChainBtn", subject = "clickListener")
  public void onStartPrimaryChainBtnClick(final ClickEvent<JmixButton> e) {
    try {
      chainWorkflowService.startPrimaryAnalysis();
      notifications.create("Запущен первичный цепочный анализ")
          .withType(Notifications.Type.SUCCESS).show();
    } catch (IllegalStateException ex) {
      notifications.create("Ошибка: " + ex.getMessage())
          .withType(Notifications.Type.ERROR).show();
    }
    refreshStats();
  }

  @Subscribe(id = "startSocialTechnicalChainBtn", subject = "clickListener")
  public void onStartSocialTechnicalChainBtnClick(final ClickEvent<JmixButton> e) {
    try {
      chainWorkflowService.startSocialTechnicalAnalysis();
      notifications.create("Запущен социально-технический цепочный анализ")
          .withType(Notifications.Type.SUCCESS).show();
    } catch (IllegalStateException ex) {
      notifications.create("Ошибка: " + ex.getMessage())
          .withType(Notifications.Type.ERROR).show();
    }
    refreshStats();
  }

  // Queue management
  @Subscribe(id = "enqueueNotAnalyzedBtn", subject = "clickListener")
  public void onEnqueueNotAnalyzedBtnClick(final ClickEvent<JmixButton> e) {
    int added = chainQueueService.enqueueNotAnalyzedVacancies(ChainAnalysisType.FULL_ANALYSIS, 1000);
    notifications.create("В очередь полного анализа добавлено: " + added).show();
    refreshStats();
  }

  @Subscribe(id = "enqueueJavaForSocialTechBtn", subject = "clickListener")
  public void onEnqueueJavaForSocialTechBtnClick(final ClickEvent<JmixButton> e) {
    int added = chainQueueService.enqueueJavaVacanciesForChainAnalysis(ChainAnalysisType.SOCIAL_TECHNICAL, 1000);
    notifications.create("В очередь социально-технического анализа добавлено: " + added).show();
    refreshStats();
  }

  @Subscribe(id = "clearFullChainQueueBtn", subject = "clickListener")
  public void onClearFullChainQueueBtnClick(final ClickEvent<JmixButton> e) {
    int cleared = chainQueueService.clearQueue(ChainAnalysisType.FULL_ANALYSIS);
    notifications.create("Очищено записей из очереди полного анализа: " + cleared).show();
    refreshStats();
  }

  private void refreshStats() {
    fullChainCountText.setText(String.valueOf(
        chainQueueService.getQueueCount(ChainAnalysisType.FULL_ANALYSIS)));
    primaryChainCountText.setText(String.valueOf(
        chainQueueService.getQueueCount(ChainAnalysisType.PRIMARY_ONLY)));
    socialTechnicalChainCountText.setText(String.valueOf(
        chainQueueService.getQueueCount(ChainAnalysisType.SOCIAL_TECHNICAL)));

    // Status of running workflows
    boolean fullRunning = chainWorkflowService.isChainAnalysisRunning("full_analysis");
    boolean primaryRunning = chainWorkflowService.isChainAnalysisRunning("primary_only");
    boolean socialTechRunning = chainWorkflowService.isChainAnalysisRunning("social_technical");

    int runningCount = (fullRunning ? 1 : 0) + (primaryRunning ? 1 : 0) + (socialTechRunning ? 1 : 0);

    chainStatusHeader.setText(runningCount == 0
        ? "Активные цепочки анализа (нет активных)"
        : "Активные цепочки анализа (" + runningCount + " активных)");
  }
}
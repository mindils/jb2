package ru.mindils.jb2.app.view.vacancymanager;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
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
import ru.mindils.jb2.app.view.main.MainView;

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
  @Autowired
  private VacancyQueueProcessorWorkflowService vacancyQueueProcessorWorkflowService;

  @Subscribe(id = "analyzePrimaryBtn", subject = "clickListener")
  public void onAnalyzePrimaryBtnClick(final ClickEvent<JmixButton> event) {
    vacancyQueueProcessorWorkflowService.startQueueProcessing();

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
    primaryQueueCountText.setText(genericTaskQueueService.getCountLlmAnalysis(GenericTaskQueueType.LLM_FIRST).toString());
  }

  @Subscribe(id = "enqueueFullChainBtn", subject = "clickListener")
  public void onEnqueueFullChainBtnClick(final ClickEvent<JmixButton> event) {
    int i = genericTaskQueueService.enqueueFirstLlmAnalysis();
    notifications.create("В очередь на полный анализ поставлено %d вакансий".formatted(i))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }
}
package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.service.LLMDebugService;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.service.VacancyService;
import ru.mindils.jb2.app.service.VacancyWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "vacancies", layout = MainView.class)
@ViewController(id = "jb2_Vacancy.list")
@ViewDescriptor(path = "vacancy-list-view.xml")
@LookupComponent("vacanciesDataGrid")
@DialogMode(width = "64em")
public class VacancyListView extends StandardListView<Vacancy> {
  @Autowired
  private Notifications notifications;
  @Autowired
  private VacancyService vacancyService;
  @Autowired
  private VacancyWorkflowService vacancyWorkflowService;
  @Autowired
  private VacancyAnalysisService vacancyAnalysisService;
  @ViewComponent
  private CollectionContainer<Vacancy> vacanciesDc;

  @Autowired
  private LLMDebugService lLMDebugService;


  @Subscribe(id = "updateVacancy", subject = "clickListener")
  public void onUpdateVacancyClick(final ClickEvent<JmixButton> event) {
//    vacancyAnalysisService.markProcessingForAllVacancy();
//        vacancyService.update("123111578");
//    vacancyService.update("117878777");
//    notifications.show("Vacancy clicked");
  }

  @Subscribe(id = "updateAllVacancy", subject = "clickListener")
  public void onUpdateAllVacancyClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.sync();
//       vacancyService.updateAll();
//        notifications.show("updateAllVacancy clicked");
  }

  @Subscribe(id = "addPrimaryToQueueBtn", subject = "clickListener")
  public void onAddPrimaryToQueueBtnClick(final ClickEvent<JmixButton> event) {
    int count = vacancyAnalysisService.markProcessingForAllVacancy(AnalysisType.PRIMARY);
    notifications.create(String.format("Добавлено в очередь на первичный анализ: %d вакансий", count))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "startPrimaryAnalysisBtn", subject = "clickListener")
  public void onStartPrimaryAnalysisBtnClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.analyze(AnalysisType.PRIMARY);
    notifications.create("Запущен воркфлоу первичного анализа").show();
  }

  @Subscribe(id = "addSocialToQueueBtn", subject = "clickListener")
  public void onAddSocialToQueueBtnClick(final ClickEvent<JmixButton> event) {
    int count = vacancyAnalysisService.markProcessingForJavaVacancy(AnalysisType.SOCIAL);
    notifications.create(String.format("Добавлено в очередь на социальный анализ: %d вакансий", count))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "startSocialAnalysisBtn", subject = "clickListener")
  public void onStartSocialAnalysisBtnClick(final ClickEvent<JmixButton> event) {
    vacancyWorkflowService.analyze(AnalysisType.SOCIAL);
    notifications.create("Запущен воркфлоу социального анализа").show();
  }
}
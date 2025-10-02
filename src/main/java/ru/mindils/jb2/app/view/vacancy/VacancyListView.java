package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.jpqlfilter.JpqlFilter;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.ChainAnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.service.LLMDebugService;
import ru.mindils.jb2.app.service.VacancyAnalysisService;
import ru.mindils.jb2.app.service.VacancyChainQueueService;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisService;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisWorkflowService;
import ru.mindils.jb2.app.service.VacancyQueueService;
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
  private VacancyChainQueueService chainQueueService;

  @Autowired
  private LLMDebugService lLMDebugService;
  @Autowired
  private VacancyQueueService vacancyQueueService;
  @ViewComponent
  private CollectionLoader<Vacancy> vacanciesDl;
  @Autowired
  private DataManager dataManager;
  @Autowired
  private FetchPlans fetchPlans;

  @Autowired
  private VacancyLlmAnalysisService vacancyLlmAnalysisService;
  @Autowired
  private VacancyLlmAnalysisWorkflowService vacancyLlmAnalysisWorkflowService;

  @ViewComponent
  private UrlQueryParametersFacet urlQueryParameters;

  @ViewComponent
  private JpqlFilter<Boolean> javaFilter;


  @Subscribe
  public void onInit(final InitEvent event) {
//    urlQueryParameters.registerBinder(new JavaFilterUrlBinder());
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

  @Subscribe("updateVacancyButton.addVacancyBtn")
  public void onUpdateVacancyButtonAddVacancyBtnClick(final DropdownButtonItem.ClickEvent event) {
    addQueueTable(AnalysisType.VACANCY_UPDATE);
  }

  @Subscribe("updateVacancyButton.addVacancyInFirstLlmQueueBtn")
  public void onUpdateVacancyButtonAddVacancyInFirstLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {
    addQueueTable(AnalysisType.PRIMARY);
  }

  @Subscribe("updateVacancyButton.addVacancyInSocialLlmQueueBtn")
  public void onUpdateVacancyButtonAddVacancyInSocialLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {
    addQueueTable(AnalysisType.SOCIAL);
  }

  @Subscribe("chainAnalysisButton.addToFullChainBtn")
  public void onChainAnalysisButtonAddToFullChainBtnClick(final DropdownButtonItem.ClickEvent event) {
    addToChainQueue(ChainAnalysisType.FULL_ANALYSIS);
  }

  @Subscribe("chainAnalysisButton.addToPrimaryChainBtn")
  public void onChainAnalysisButtonAddToPrimaryChainBtnClick(final DropdownButtonItem.ClickEvent event) {
    addToChainQueue(ChainAnalysisType.PRIMARY_ONLY);
  }

  @Subscribe("chainAnalysisButton.addToSocialTechnicalChainBtn")
  public void onChainAnalysisButtonAddToSocialTechnicalChainBtnClick(final DropdownButtonItem.ClickEvent event) {
    addToChainQueue(ChainAnalysisType.SOCIAL_TECHNICAL);
  }

  private void addToChainQueue(ChainAnalysisType chainType) {
    int added = chainQueueService.enqueueFromLoader(
        vacanciesDl,
        chainType,
        1000
    );

    String chainName = switch (chainType) {
      case FULL_ANALYSIS -> "полного анализа";
      case PRIMARY_ONLY -> "первичного анализа";
      case SOCIAL_TECHNICAL -> "социально-технического анализа";
      default -> "анализа";
    };

    notifications.create(
            String.format("В очередь %s добавлено: %d вакансий", chainName, added))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }


  private void addQueueTable(AnalysisType queueType) {
    int added = vacancyQueueService.enqueueAllForUpdate(
        vacanciesDl,
        queueType,
        1000
    );

    notifications.create(
            String.format("В очередь добавлено: %d вакансий (тип: %s)", added, queueType))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe(id = "updateAnalysisVacancy", subject = "clickListener")
  public void onUpdateAnalysisVacancyClick(final ClickEvent<JmixButton> event) {
    Vacancy vacancy = vacanciesDc.getItemOrNull();
    if (vacancy == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }

    vacancyLlmAnalysisWorkflowService.startFullAnalysisBy(vacancy.getId());

    notifications.create("В %s отправлена на выполнение".formatted(vacancy.getId()))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("firstLlmAnalyseMenu")
  public void onFirstLlmAnalyseMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    Vacancy vacancy = vacanciesDc.getItemOrNull();

    if (vacancy == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }

    vacancyLlmAnalysisWorkflowService.startFirstAnalysisBy(vacancy.getId());
  }
}
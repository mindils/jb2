package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.GenericTaskQueueType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyScore;
import ru.mindils.jb2.app.entity.VacancyScoreRating;
import ru.mindils.jb2.app.service.GenericTaskQueueService;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisWorkflowService;
import ru.mindils.jb2.app.service.VacancyScorerService;
import ru.mindils.jb2.app.service.VacancyUpdateWorkflowService;
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
  private VacancyLlmAnalysisWorkflowService vacancyLlmAnalysisWorkflowService;

  @Autowired
  private VacancyUpdateWorkflowService vacancyUpdateWorkflowService;

  @Autowired
  private GenericTaskQueueService genericTaskQueueService;

  @Autowired
  private VacancyScorerService vacancyScorerService;

  @ViewComponent
  private CollectionContainer<Vacancy> vacanciesDc;

  @ViewComponent
  private CollectionLoader<Vacancy> vacanciesDl;

  @Subscribe
  public void onInit(final InitEvent event) {
    // urlQueryParameters.registerBinder(new JavaFilterUrlBinder());
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

  @Subscribe("vacancyDropdown.updateVacancyBtn")
  public void onVacancyDropdownUpdateVacancyBtnClick(final DropdownButtonItem.ClickEvent event) {
    int added = genericTaskQueueService.enqueueFromLoader(
        vacanciesDl,
        GenericTaskQueueType.VACANCY_UPDATE,
        1000
    );

    notifications.create("В очередь на обновление добавлено %d вакансий".formatted(added))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.addVacancyInFirstLlmQueueBtn")
  public void onVacancyDropdownAddVacancyInFirstLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {
    int added = genericTaskQueueService.enqueueFromLoader(
        vacanciesDl,
        GenericTaskQueueType.LLM_FIRST,
        1000
    );

    notifications.create("В очередь на первичный анализ добавлено %d вакансий".formatted(added))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.addVacancyInSocialLlmQueueBtn")
  public void onVacancyDropdownAddVacancyInSocialLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {

    int added = genericTaskQueueService.enqueueFromLoader(
        vacanciesDl,
        GenericTaskQueueType.LLM_FULL,
        1000
    );

    notifications.create("В очередь на полный анализ добавлено %d вакансий".formatted(added))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.calculateVacancyScoresBtn")
  public void onVacancyDropdownCalculateVacancyScoresBtnClick(final DropdownButtonItem.ClickEvent event) {
    try {
      // Рассчитываем оценки для всех вакансий из лоадера
      VacancyScorerService.BatchProcessingResult result = vacancyScorerService.calculateAndSaveFromLoader(
          vacanciesDl,
          100 // batch size
      );

      // Показываем результат
      notifications.create(
              "Расчет оценок завершен: %s".formatted(result.toString()))
          .withType(result.getFailed() > 0 ? Notifications.Type.WARNING : Notifications.Type.SUCCESS)
          .show();

      // Если были ошибки, логируем их
      if (!result.getErrors().isEmpty()) {
        notifications.create(
                "Обнаружены ошибки при расчете. Проверьте логи.")
            .withType(Notifications.Type.WARNING)
            .show();
      }

      // Обновляем грид
      getViewData().loadAll();

    } catch (Exception e) {
      notifications.create("Ошибка при массовом расчете оценок: " + e.getMessage())
          .withType(Notifications.Type.ERROR)
          .show();
    }
  }

  @Subscribe("fullLlmAnalyseMenu")
  public void onFullLlmAnalyseMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    Vacancy vacancy = vacanciesDc.getItemOrNull();

    if (vacancy == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }

    vacancyLlmAnalysisWorkflowService.startFullAnalysisBy(vacancy.getId());
  }

  @Subscribe("updateVacancyMenu")
  public void onUpdateVacancyMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    Vacancy vacancy = vacanciesDc.getItemOrNull();

    if (vacancy == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }

    try {
      vacancyUpdateWorkflowService.startUpdateWorkflow(vacancy.getId());
      notifications.create("Обновление вакансии запущено")
          .withType(Notifications.Type.SUCCESS)
          .show();
    } catch (Exception e) {
      notifications.create("Ошибка при запуске обновления: " + e.getMessage())
          .withType(Notifications.Type.ERROR)
          .show();
    }
  }

  @Subscribe("vacancyScoreCalculateMenu")
  public void onVacancyScoreCalculateMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    Vacancy vacancy = vacanciesDc.getItemOrNull();

    if (vacancy == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }

    try {
      // Рассчитываем оценку вакансии
      VacancyScore score = vacancyScorerService.calculateAndSave(vacancy.getId());

      // Показываем результат пользователю
      VacancyScoreRating rating = score.getRating();
      notifications.create("Оценка рассчитана: %d баллов (%s)".formatted(score.getTotalScore(), rating))
          .withType(Notifications.Type.SUCCESS)
          .show();

      // Обновляем грид
      getViewData().loadAll();

    } catch (RuntimeException e) {
      notifications.create("Ошибка при расчете оценки: " + e.getMessage())
          .withType(Notifications.Type.ERROR)
          .show();
    }
  }
}
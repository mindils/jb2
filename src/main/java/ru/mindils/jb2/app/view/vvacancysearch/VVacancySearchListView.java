package ru.mindils.jb2.app.view.vvacancysearch;

import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.*;
import ru.mindils.jb2.app.service.GenericTaskQueueService;
import ru.mindils.jb2.app.service.VacancyLlmAnalysisWorkflowService;
import ru.mindils.jb2.app.service.VacancyScorerService;
import ru.mindils.jb2.app.service.VacancyUpdateWorkflowService;
import ru.mindils.jb2.app.view.main.MainView;

import java.util.*;

@Route(value = "v-vacancy-searches", layout = MainView.class)
@ViewController(id = "jb2_VVacancySearch.list")
@ViewDescriptor(path = "v-vacancy-search-list-view.xml")
@LookupComponent("vVacancySearchesDataGrid")
@DialogMode(width = "64em")
public class VVacancySearchListView extends StandardListView<VVacancySearch> {

  @Autowired
  private Notifications notifications;

  @Autowired
  private DataManager dataManager;

  @Autowired
  private VacancyLlmAnalysisWorkflowService vacancyLlmAnalysisWorkflowService;

  @Autowired
  private VacancyUpdateWorkflowService vacancyUpdateWorkflowService;

  @Autowired
  private GenericTaskQueueService genericTaskQueueService;

  @Autowired
  private VacancyScorerService vacancyScorerService;

  @ViewComponent
  private CollectionContainer<VVacancySearch> vVacancySearchesDc;

  @ViewComponent
  private CollectionLoader<VVacancySearch> vVacancySearchesDl;
  @Autowired
  private Dialogs dialogs;
  @Autowired
  private DialogWindows dialogWindows;

  @Subscribe("firstLlmAnalyseMenu")
  public void onFirstLlmAnalyseMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    VVacancySearch vVacancySearch = vVacancySearchesDc.getItemOrNull();
    if (vVacancySearch == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }
    vacancyLlmAnalysisWorkflowService.startFirstAnalysisBy(vVacancySearch.getId());
    notifications.create("Первичный анализ запущен")
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.updateVacancyBtn")
  public void onVacancyDropdownUpdateVacancyBtnClick(final DropdownButtonItem.ClickEvent event) {
    // Подсчитываем количество записей
    LoadContext<VVacancySearch> ctx = vVacancySearchesDl.createLoadContext();
    long totalToProcess = dataManager.getCount(ctx);

    int added = genericTaskQueueService.enqueueFromVViewLoader(
        vVacancySearchesDl,
        GenericTaskQueueType.VACANCY_UPDATE,
        1000
    );
    notifications.create("В очередь на обновление добавлено %d вакансий (из %d отфильтрованных)"
            .formatted(added, totalToProcess))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.addVacancyInFirstLlmQueueBtn")
  public void onVacancyDropdownAddVacancyInFirstLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {
    // Подсчитываем количество записей
    LoadContext<VVacancySearch> ctx = vVacancySearchesDl.createLoadContext();
    long totalToProcess = dataManager.getCount(ctx);

    int added = genericTaskQueueService.enqueueFromVViewLoader(
        vVacancySearchesDl,
        GenericTaskQueueType.LLM_FIRST,
        1000
    );
    notifications.create("В очередь на первичный анализ добавлено %d вакансий (из %d отфильтрованных)"
            .formatted(added, totalToProcess))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.addVacancyInSocialLlmQueueBtn")
  public void onVacancyDropdownAddVacancyInSocialLlmQueueBtnClick(final DropdownButtonItem.ClickEvent event) {
    // Подсчитываем количество записей
    LoadContext<VVacancySearch> ctx = vVacancySearchesDl.createLoadContext();
    long totalToProcess = dataManager.getCount(ctx);

    int added = genericTaskQueueService.enqueueFromVViewLoader(
        vVacancySearchesDl,
        GenericTaskQueueType.LLM_FULL,
        1000
    );
    notifications.create("В очередь на полный анализ добавлено %d вакансий (из %d отфильтрованных)"
            .formatted(added, totalToProcess))
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("vacancyDropdown.calculateVacancyScoresBtn")
  public void onVacancyDropdownCalculateVacancyScoresBtnClick(final DropdownButtonItem.ClickEvent event) {
    try {
      // Подсчитываем количество записей, которые будут обработаны
      LoadContext<VVacancySearch> ctx = vVacancySearchesDl.createLoadContext();
      long totalToProcess = dataManager.getCount(ctx);

      // Показываем подтверждение
      notifications.create("Начинаю расчет оценок для %d вакансий...".formatted(totalToProcess))
          .withType(Notifications.Type.DEFAULT)
          .show();

      // Рассчитываем оценки для всех вакансий из лоадера
      VacancyScorerService.BatchProcessingResult result = vacancyScorerService.calculateAndSaveFromVViewLoader(
          vVacancySearchesDl,
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
    VVacancySearch vVacancySearch = vVacancySearchesDc.getItemOrNull();
    if (vVacancySearch == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }
    vacancyLlmAnalysisWorkflowService.startFullAnalysisBy(vVacancySearch.getId());
    notifications.create("Полный анализ запущен")
        .withType(Notifications.Type.SUCCESS)
        .show();
  }

  @Subscribe("updateVacancyMenu")
  public void onUpdateVacancyMenuGridContextMenuItemClick(final GridContextMenu.GridContextMenuItemClickEvent<?> event) {
    VVacancySearch vVacancySearch = vVacancySearchesDc.getItemOrNull();
    if (vVacancySearch == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }
    try {
      vacancyUpdateWorkflowService.startUpdateWorkflow(vVacancySearch.getId());
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
    VVacancySearch vVacancySearch = vVacancySearchesDc.getItemOrNull();
    if (vVacancySearch == null) {
      notifications.create("Вакансия не выбрана")
          .withType(Notifications.Type.ERROR)
          .show();
      return;
    }
    try {
      // Рассчитываем оценку вакансии
      VacancyScore score = vacancyScorerService.calculateAndSave(vVacancySearch.getId());

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

  @Subscribe("vVacancySearchesDataGrid.readAction")
  public void onVVacancySearchesDataGridReadAction(final ActionPerformedEvent event) {
    VVacancySearch itemOrNull = vVacancySearchesDc.getItemOrNull();
    if (itemOrNull == null) {
      notifications.show("Выберете вакансию");
      return;
    }

    Optional<Vacancy> optional = dataManager.load(Vacancy.class)
        .id(itemOrNull.getId())
        .optional();

    if (optional.isEmpty()) {
      notifications.show("Чет вакансию не нашли  в бд");
      return;
    }
    DialogWindow<View<?>> window = dialogWindows.detail(this, Vacancy.class)
        .editEntity(optional.get())
        .build();

    if (window.getView() instanceof StandardDetailView) {
      ((StandardDetailView<?>) window.getView()).setReadOnly(true);
    }

    window.addAfterCloseListener(afterCloseEvent -> {
      if (afterCloseEvent.closedWith(StandardOutcome.SAVE)) {
        vVacancySearchesDl.load();
      }
    });

    window.open();
  }
}
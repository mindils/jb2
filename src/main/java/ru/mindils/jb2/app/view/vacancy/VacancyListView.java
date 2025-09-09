package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.rest.vacancy.VacancyApiClient;
import ru.mindils.jb2.app.service.VacancyService;
import ru.mindils.jb2.app.service.VacancySyncWorkflowService;
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
  private VacancySyncWorkflowService vacancySyncWorkflowService;

    @Subscribe(id = "updateVacancy", subject = "clickListener")
    public void onUpdateVacancyClick(final ClickEvent<JmixButton> event) {
//        vacancyService.update("123111578");
        vacancyService.update("117878777");
        notifications.show("Vacancy clicked");
    }

    @Subscribe(id = "updateAllVacancy", subject = "clickListener")
    public void onUpdateAllVacancyClick(final ClickEvent<JmixButton> event) {
        vacancySyncWorkflowService.sync();
//       vacancyService.updateAll();
//        notifications.show("updateAllVacancy clicked");
    }
}
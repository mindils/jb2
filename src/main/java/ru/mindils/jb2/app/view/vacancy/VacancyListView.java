package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "vacancies", layout = MainView.class)
@ViewController(id = "jb2_Vacancy.list")
@ViewDescriptor(path = "vacancy-list-view.xml")
@LookupComponent("vacanciesDataGrid")
@DialogMode(width = "64em")
public class VacancyListView extends StandardListView<Vacancy> {
  @Autowired
  private Notifications notifications;

  @Subscribe(id = "updateVacancy", subject = "clickListener")
  public void onUpdateVacancyClick(final ClickEvent<JmixButton> event) {
    notifications.show("Vacancy clicked");
  }
}
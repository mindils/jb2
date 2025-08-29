package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "vacancies/:id", layout = MainView.class)
@ViewController(id = "jb2_Vacancy.detail")
@ViewDescriptor(path = "vacancy-detail-view.xml")
@EditedEntityContainer("vacancyDc")
public class VacancyDetailView extends StandardDetailView<Vacancy> {
}
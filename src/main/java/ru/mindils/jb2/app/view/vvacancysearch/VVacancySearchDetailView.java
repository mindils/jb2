package ru.mindils.jb2.app.view.vvacancysearch;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.VVacancySearch;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "v-vacancy-searches/:id", layout = MainView.class)
@ViewController(id = "jb2_VVacancySearch.detail")
@ViewDescriptor(path = "v-vacancy-search-detail-view.xml")
@EditedEntityContainer("vVacancySearchDc")
public class VVacancySearchDetailView extends StandardDetailView<VVacancySearch> {
}
package ru.mindils.jb2.app.view.vvacancysearch;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import ru.mindils.jb2.app.entity.VVacancySearch;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "v-vacancy-searches", layout = MainView.class)
@ViewController(id = "jb2_VVacancySearch.list")
@ViewDescriptor(path = "v-vacancy-search-list-view.xml")
@LookupComponent("vVacancySearchesDataGrid")
@DialogMode(width = "64em")
public class VVacancySearchListView extends StandardListView<VVacancySearch> {
}
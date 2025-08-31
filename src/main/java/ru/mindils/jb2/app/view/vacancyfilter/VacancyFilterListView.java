package ru.mindils.jb2.app.view.vacancyfilter;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import ru.mindils.jb2.app.entity.VacancyFilter;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "vacancy-filters", layout = MainView.class)
@ViewController(id = "jb2_VacancyFilter.list")
@ViewDescriptor(path = "vacancy-filter-list-view.xml")
@LookupComponent("vacancyFiltersDataGrid")
@DialogMode(width = "64em")
public class VacancyFilterListView extends StandardListView<VacancyFilter> {
}
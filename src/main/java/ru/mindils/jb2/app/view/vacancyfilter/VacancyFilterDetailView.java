package ru.mindils.jb2.app.view.vacancyfilter;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import ru.mindils.jb2.app.entity.VacancyFilter;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "vacancy-filters/:id", layout = MainView.class)
@ViewController(id = "jb2_VacancyFilter.detail")
@ViewDescriptor(path = "vacancy-filter-detail-view.xml")
@EditedEntityContainer("vacancyFilterDc")
public class VacancyFilterDetailView extends StandardDetailView<VacancyFilter> {
    @ViewComponent
    private CollectionLoader<VacancyFilterParams> vacancyFilterParamsDl;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        vacancyFilterParamsDl.setParameter("vacancyFilterId", getEditedEntity().getId());
        vacancyFilterParamsDl.load();
    }
}
package ru.mindils.jb2.app.view.vacancyfilterparams;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "vacancy-filter-paramses/:id", layout = MainView.class)
@ViewController(id = "jb2_VacancyFilterParams.detail")
@ViewDescriptor(path = "vacancy-filter-params-detail-view.xml")
@EditedEntityContainer("vacancyFilterParamsDc")
public class VacancyFilterParamsDetailView extends StandardDetailView<VacancyFilterParams> {
}
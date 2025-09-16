package ru.mindils.jb2.app.view.vacancyops;


import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "vacancy-ops-view", layout = MainView.class)
@ViewController(id = "jb2_VacancyOpsView")
@ViewDescriptor(path = "vacancy-ops-view.xml")
public class VacancyOpsView extends StandardView {
}
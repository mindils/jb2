package ru.mindils.jb2.app.view.employer;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "employers/:id", layout = MainView.class)
@ViewController(id = "jb2_Employer.detail")
@ViewDescriptor(path = "employer-detail-view.xml")
@EditedEntityContainer("employerDc")
public class EmployerDetailView extends StandardDetailView<Employer> {
}
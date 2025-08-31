package ru.mindils.jb2.app.view.employer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.service.EmployerService;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "employers", layout = MainView.class)
@ViewController(id = "jb2_Employer.list")
@ViewDescriptor(path = "employer-list-view.xml")
@LookupComponent("employersDataGrid")
@DialogMode(width = "64em")
public class EmployerListView extends StandardListView<Employer> {
    private final EmployerService employerService;

    public EmployerListView(EmployerService employerService) {
        this.employerService = employerService;
    }

    @Subscribe(id = "employerUpdate", subject = "clickListener")
    public void onEmployerUpdateClick(final ClickEvent<JmixButton> event) {
        employerService.update("1057");

    }
}
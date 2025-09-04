package ru.mindils.jb2.app.view.employer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "employers/:id", layout = MainView.class)
@ViewController(id = "jb2_Employer.detail")
@ViewDescriptor(path = "employer-detail-view.xml")
@EditedEntityContainer("employerDc")
public class EmployerDetailView extends StandardDetailView<Employer> {

  @ViewComponent
  private Html description;
  @ViewComponent
  private Html brandedDescription;

  @Subscribe
  public void onBeforeShow(final BeforeShowEvent event) {
    description.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getDescription()));
    brandedDescription.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getBrandedDescription()));
  }
}
package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "vacancies/:id", layout = MainView.class)
@ViewController(id = "jb2_Vacancy.detail")
@ViewDescriptor(path = "vacancy-detail-view.xml")
@EditedEntityContainer("vacancyDc")
public class VacancyDetailView extends StandardDetailView<Vacancy> {

  private final DialogWindows dialogWindows;
  @ViewComponent
  private Html description;
  @ViewComponent
  private Html brandedDescription;
  @ViewComponent
  private JmixButton employerUrl;

  public VacancyDetailView(DialogWindows dialogWindows) {
    this.dialogWindows = dialogWindows;
  }

  @Subscribe
  public void onBeforeShow(final BeforeShowEvent event) {
    description.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getDescription()));
    brandedDescription.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getBrandedDescription()));

    employerUrl.setText(getEditedEntity().getEmployer().getName());
  }

  @Subscribe(id = "employerUrl", subject = "clickListener")
  public void onEmployerUrlClick(final ClickEvent<JmixButton> event) {
    DialogWindow<View<?>> window = dialogWindows.detail(this, Employer.class)
        .editEntity(getEditedEntity().getEmployer())
        .build();

    if (window.getView() instanceof StandardDetailView) {
      ((StandardDetailView<?>) window.getView()).setReadOnly(true);
    }

    window.open();

  }
}
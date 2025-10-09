package ru.mindils.jb2.app.view.vacancy;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyInfo;
import ru.mindils.jb2.app.entity.VacancyStatus;
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
  @Autowired
  private DataManager dataManager;
  @ViewComponent
  private JmixSelect<Object> vacancyInfoSelect;

  public VacancyDetailView(DialogWindows dialogWindows) {
    this.dialogWindows = dialogWindows;
  }

  @Subscribe
  public void onBeforeShow(final BeforeShowEvent event) {
    description.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getDescription()));
    brandedDescription.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getBrandedDescription()));

    employerUrl.setText(getEditedEntity().getEmployer().getName());

    VacancyInfo vacancyInfo = getEditedEntity().getVacancyInfo();
    if (vacancyInfo != null && vacancyInfo.getStatus() != null) {
      vacancyInfoSelect.setValue(vacancyInfo.getStatus());
    }
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

  @Subscribe("vacancyInfoSelect")
  public void onVacancyInfoSelectComponentValueChange(
      final AbstractField.ComponentValueChangeEvent<JmixSelect<?>, VacancyStatus> event) {

    VacancyInfo vacancyInfo = dataManager.load(VacancyInfo.class)
        .id(getEditedEntity().getId())
        .optional()
        .orElseGet(() -> {
          VacancyInfo info = dataManager.create(VacancyInfo.class);
          info.setId(getEditedEntity().getId());
          return info;
        });

    vacancyInfo.setStatus(event.getValue() == null ? null : event.getValue());
    dataManager.save(vacancyInfo);
  }

  @Subscribe(id = "goToHhBtn", subject = "clickListener")
  public void onGoToHhBtnClick(final ClickEvent<JmixButton> event) {
    String alternateUrl = getEditedEntity().getAlternateUrl();
    if (alternateUrl != null && !alternateUrl.isEmpty()) {
      UI.getCurrent().getPage().open(alternateUrl, "_blank");
    }
  }
}
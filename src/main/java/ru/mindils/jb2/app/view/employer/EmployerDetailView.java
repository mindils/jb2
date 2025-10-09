package ru.mindils.jb2.app.view.employer;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.EmployerInfo;
import ru.mindils.jb2.app.entity.EmployerStatus;
import ru.mindils.jb2.app.entity.VacancyInfo;
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

  @Autowired
  private DataManager dataManager;

  @ViewComponent
  private JmixSelect<Object> employerInfoSelect;

  @Subscribe
  public void onBeforeShow(final BeforeShowEvent event) {
    description.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getDescription()));
    brandedDescription.setHtmlContent("<div>%s</div>".formatted(getEditedEntity().getBrandedDescription()));

    EmployerInfo employerInfo = getEditedEntity().getEmployerInfo();
    if (employerInfo != null && employerInfo.getStatus() != null) {
      employerInfoSelect.setValue(employerInfo.getStatus());
    }
  }

  @Subscribe("employerInfoSelect")
  public void onEmployerInfoSelectComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixSelect<?>, EmployerStatus> event) {
    EmployerInfo employerInfo = dataManager.load(EmployerInfo.class)
        .id(getEditedEntity().getId())
        .optional()
        .orElseGet(() -> {
          EmployerInfo info = dataManager.create(EmployerInfo.class);
          info.setId(getEditedEntity().getId());
          return info;
        });

    employerInfo.setStatus(event.getValue() == null ? null : event.getValue());
    dataManager.save(employerInfo);
  }
}
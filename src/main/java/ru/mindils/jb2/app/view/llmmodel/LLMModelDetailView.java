package ru.mindils.jb2.app.view.llmmodel;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.LLMModel;
import ru.mindils.jb2.app.view.main.MainView;

@Route(value = "l-lm-models/:id", layout = MainView.class)
@ViewController(id = "jb2_LLMModel.detail")
@ViewDescriptor(path = "llm-model-detail-view.xml")
@EditedEntityContainer("lLMModelDc")
public class LLMModelDetailView extends StandardDetailView<LLMModel> {
}
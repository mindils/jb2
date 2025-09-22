package ru.mindils.jb2.app.view.llmmodel;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import ru.mindils.jb2.app.entity.LLMModel;
import ru.mindils.jb2.app.view.main.MainView;


@Route(value = "l-lm-models", layout = MainView.class)
@ViewController(id = "jb2_LLMModel.list")
@ViewDescriptor(path = "llm-model-list-view.xml")
@LookupComponent("lLMModelsDataGrid")
@DialogMode(width = "64em")
public class LLMModelListView extends StandardListView<LLMModel> {
}
package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class ModelManagerViewTest extends ApplicationTest {

    private ModelManagerView view;
    private GridPane parent;
    private StackPane container;

    @Override
    public void start(Stage stage) {
        view = new ModelManagerView();
        parent = new GridPane();
        parent.add(view, 0, 0);
        container = new StackPane(parent);
        Scene scene = new Scene(container, 400, 400);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void shows_parent_at_first() {
        then((Object) view.isVisible()).isEqualTo(true);
        then((Object) parent.isVisible()).isEqualTo(true);
    }

    @Test
    void has_add_delete_remote() {
        then((Object) lookup("#add_model")).isNotNull();
        then((Object) lookup("#delete_model")).isNotNull();
        then((Object) lookup("#remote_models")).isNotNull();
    }

    @Test
    void clicking_add_model_manually_shows_form_and_hides_parent() throws Exception {
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        then((Object) parent.isVisible()).isEqualTo(false);
        then((Object) lookup("Model Name:").queryLabeled()).isNotNull();
    }

    @Test
    void dialog_has_required_fields() {
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        then((Object) lookup("Model Name:").queryLabeled()).isNotNull();
        then((Object) lookup("Description:").queryLabeled()).isNotNull();
        then((Object) lookup("Input Price:").queryLabeled()).isNotNull();
        then((Object) lookup("Output Price:").queryLabeled()).isNotNull();

        then((Object) lookup("Save").queryButton()).isNotNull();
        then((Object) lookup("Cancel").queryButton()).isNotNull();
    }

    @Test
    void returning_from_form_shows_parent() {
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();
        then((Object) parent.isVisible()).isEqualTo(false);

        clickOn("Cancel"); waitForFxEvents();
        // wait for animation to finish
        try { Thread.sleep(600); } catch (InterruptedException e) {}
        waitForFxEvents();

        then((Object) parent.isVisible()).isEqualTo(true);
    }

    @Test
    void enable_Save_only_if_name_is_valid() {
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        then(lookup("Save").queryButton().isDisabled()).isTrue();

        clickOn("#name .text-input"); write("new-model"); waitForFxEvents();
        then(lookup("Save").queryButton().isDisabled()).isFalse();
    }

    @Test
    void update_models_on_save() {
        final ObjectProperty<GenAIProvider> provider = new SimpleObjectProperty(GenAIProvider.CUSTOM_OPEN_AI);
        final ListProperty<GenAIModel> models = new SimpleListProperty(FXCollections.observableArrayList());
        models.getValue().add(new GenAIModel(
            provider.getValue(), "one", "desc1", 1.0, 1.1
        ));

        view.providerProperty.bind(provider);
        view.modelsProperty.bindBidirectional(models);

        //
        // Add a first model
        //
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("two"); waitForFxEvents();
        clickOn("#description .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("desc2"); waitForFxEvents();
        clickOn("#input_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("2.0"); waitForFxEvents();
        clickOn("#output_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("2.1"); waitForFxEvents();

        clickOn("Save");

        then(models).hasSize(2);

        GenAIModel m = models.getValue().get(0);
        then(m.name()).isEqualTo("one");
        then(m.description()).isEqualTo("desc1");
        then(m.inputPrice()).isEqualTo(1.0);
        then(m.outputPrice()).isEqualTo(1.1);

        m = models.getValue().get(1);
        then(m.name()).isEqualTo("two");
        then(m.description()).isEqualTo("desc2");
        then(m.inputPrice()).isEqualTo(2.0);
        then(m.outputPrice()).isEqualTo(2.1);

        //
        // Add a second model
        //
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("three"); waitForFxEvents();
        clickOn("#description .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("desc3"); waitForFxEvents();
        clickOn("#input_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("3.0"); waitForFxEvents();
        clickOn("#output_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("3.1"); waitForFxEvents();

        clickOn("Save");

        then(models).hasSize(3);

        m = models.getValue().get(0);
        then(m.name()).isEqualTo("one");
        then(m.description()).isEqualTo("desc1");
        then(m.inputPrice()).isEqualTo(1.0);
        then(m.outputPrice()).isEqualTo(1.1);

        m = models.getValue().get(1);
        then(m.name()).isEqualTo("two");
        then(m.description()).isEqualTo("desc2");
        then(m.inputPrice()).isEqualTo(2.0);
        then(m.outputPrice()).isEqualTo(2.1);

        m = models.getValue().get(2);
        then(m.name()).isEqualTo("three");
        then(m.description()).isEqualTo("desc3");
        then(m.inputPrice()).isEqualTo(3.0);
        then(m.outputPrice()).isEqualTo(3.1);

    }

    @Test
    void reset_values_on_add_manually() {
        final ObjectProperty<GenAIProvider> provider = new SimpleObjectProperty(GenAIProvider.CUSTOM_OPEN_AI);
        final ListProperty<GenAIModel> models = new SimpleListProperty(FXCollections.observableArrayList());
        models.getValue().add(new GenAIModel(
            provider.getValue(), "one", "desc1", 1.0, 1.1
        ));

        view.providerProperty.bind(provider);
        view.modelsProperty.bindBidirectional(models);

        //
        // Add a first model
        //
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("two"); waitForFxEvents();
        clickOn("Save");

        //
        // Add a second model
        //
        clickOn(view);
        clickOn("#add_model"); waitForFxEvents();

        then(lookup("#name .text-input").queryTextInputControl().getText()).isEqualTo("");
        then(lookup(("#description .text-input")).queryTextInputControl().getText()).isEqualTo("");
        then(lookup(("#input_price .text-input")).queryTextInputControl().getText()).isEqualTo("0.0");
        then(lookup(("#output_price .text-input")).queryTextInputControl().getText()).isEqualTo("0.0");
    }

    @Test
    void remote_shows_ModelUpdterDialog() throws Exception {
        /*
        view.providerProperty.bind(new SimpleObjectProperty(GenAIProvider.CUSTOM_OPEN_AI));

        clickOn("#remote_models"); waitForFxEvents();

        I can0t really do this because the we are now in a swing environment...
        Let's skip it for now, we will come back on this when the panel is turned
        into FX
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isVisible()) {
                    if (ModelUpdaterDialog.TITLE.equals(((JDialog) window).getTitle())) {
                        return true;
                    }
                }
            }
            return false;
        });
        */
    }

    @Test
    void delete_button_delets_the_selected_model() {
        final ObjectProperty<GenAIProvider> provider = new SimpleObjectProperty(GenAIProvider.CUSTOM_OPEN_AI);
        final ObjectProperty<String> selected = new SimpleObjectProperty();
        final ListProperty<GenAIModel> models = new SimpleListProperty(FXCollections.observableArrayList());

        models.getValue().addAll(
            new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, "one", "desc1", 1.0, 1.1),
            new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, "two", "desc2", 2.0, 2.1)
        );

        view.providerProperty.bind(provider);
        view.modelsProperty.bindBidirectional(models);
        view.selectedModelProperty.bind(selected);

        //
        // no selected model... nothing to delete
        //
        clickOn("#delete_model"); waitForFxEvents();
        then(models).hasSize(2);

        selected.set("two");
        clickOn("#delete_model"); waitForFxEvents();
        then(models).containsExactly(models.get(0));

    }
}

package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class ModelManagementViewTest extends ApplicationTest {

    private ModelManagementView view;

    @Override
    public void start(Stage stage) {
        view = new ModelManagementView();
        Scene scene = new Scene(view, 400, 400);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void has_add_model_manually_menu_item() {
        then(view.getItems())
            .extracting(MenuItem::getText)
            .contains("Add model manually");
    }

    @Test
    void has_add_models_from_remote_menu_item() {
        then(view.getItems())
            .extracting(MenuItem::getText)
            .contains("Add models from remote");
    }

    @Test
    void clicking_add_model_manually_opens_dialog() {
        clickOn(view);
        clickOn("Add model manually");

        boolean dialogFound = listWindows().stream()
            .filter(w -> w instanceof javafx.stage.Stage)
            .map(w -> (javafx.stage.Stage) w)
            .anyMatch(s -> "Add Custom Model".equals(s.getTitle()));

        then(dialogFound).as("Dialog 'Add Custom Model' should be visible").isTrue();
    }

    @Test
    void dialog_has_required_fields() {
        clickOn(view);
        clickOn("Add model manually");

        then((Object) lookup("Model Name:").queryLabeled()).isNotNull();
        then((Object) lookup("Description:").queryLabeled()).isNotNull();
        then((Object) lookup("Input Price:").queryLabeled()).isNotNull();
        then((Object) lookup("Output Price:").queryLabeled()).isNotNull();

        then((Object) lookup("OK").queryButton()).isNotNull();
        then((Object) lookup("Cancel").queryButton()).isNotNull();
    }

    @Test
    void enable_OK_only_if_name_is_valid() {
        clickOn(view);
        clickOn("Add model manually"); waitForFxEvents();

        then(lookup("OK").queryButton().isDisabled()).isTrue();

        clickOn(".text-input"); write("new-model"); waitForFxEvents();
        then(lookup("OK").queryButton().isDisabled()).isFalse();
    }

    @Test
    void update_models_on_ok() {
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
        clickOn("Add model manually"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("two"); waitForFxEvents();
        clickOn("#description .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("desc2"); waitForFxEvents();
        clickOn("#input_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("2.0"); waitForFxEvents();
        clickOn("#output_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("2.1"); waitForFxEvents();

        clickOn("OK");

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
        clickOn("Add model manually"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("three"); waitForFxEvents();
        clickOn("#description .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("desc3"); waitForFxEvents();
        clickOn("#input_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("3.0"); waitForFxEvents();
        clickOn("#output_price .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("3.1"); waitForFxEvents();

        clickOn("OK");

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
        clickOn("Add model manually"); waitForFxEvents();

        clickOn("#name .text-input").push(KeyCode.SHORTCUT, KeyCode.A).write("two"); waitForFxEvents();
        clickOn("OK");

        //
        // Add a second model
        //
        clickOn(view);
        clickOn("Add model manually"); waitForFxEvents();

        then(lookup("#name .text-input").queryTextInputControl().getText()).isEqualTo("");
        then(lookup(("#description .text-input")).queryTextInputControl().getText()).isEqualTo("");
        then(lookup(("#input_price .text-input")).queryTextInputControl().getText()).isEqualTo("0.0");
        then(lookup(("#output_price .text-input")).queryTextInputControl().getText()).isEqualTo("0.0");
    }
}

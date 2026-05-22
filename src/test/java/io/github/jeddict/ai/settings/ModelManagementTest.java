package io.github.jeddict.ai.settings;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class ModelManagementTest extends ApplicationTest {

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
}

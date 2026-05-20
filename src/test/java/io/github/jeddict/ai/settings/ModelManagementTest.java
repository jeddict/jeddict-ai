package io.github.jeddict.ai.settings;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.assertj.core.api.BDDAssertions.then;

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
}

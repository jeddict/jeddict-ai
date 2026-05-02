package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.settings.JeddictPreferences;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.testfx.framework.junit5.ApplicationTest;

public class JeddictPreferencesUITest extends ApplicationTest {

    @TempDir
    public static Path HOME;

    private JeddictPreferences ui;

    @BeforeAll
    public static void beforeAll() throws Exception {
        // prepare a test home with bundled settings
        System.setProperty("user.home", HOME.toAbsolutePath().toString());
        Files.copy(Path.of("src/test/resources/settings/jeddict.json"), HOME.resolve("jeddict.json"));
        // initialize singleton
        PreferencesManager.getInstance();
    }

    @Override
    public void start(Stage stage) {
        ui = new JeddictPreferences();
        Scene scene = new Scene(new StackPane(ui.preferences.getView()), 900, 700);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void ui_prompts_saved_via_ui() {
        // select Prompts category
        clickOn("Prompts");

        // Open prompts editor and add a prompt via the UI
        clickOn("#addButton");
        clickOn("#nameField").write("uiPrompt");
        clickOn("#contentArea").write("ui prompt content");
        // click Create
        clickOn("#actionButton");

        // Persist
        interact(() -> ui.save());

        PreferencesManager pm = PreferencesManager.getInstance();
        then(pm.getPrompts()).containsEntry("uiPrompt", "ui prompt content");
    }
}

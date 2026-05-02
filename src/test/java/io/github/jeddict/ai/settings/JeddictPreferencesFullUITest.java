package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Full UI test for entire JeddictPreferences panel using TestFX.
 * It operates on the UI but sets the backing SettingsProperty values to
 * simulate user interaction across all categories, then calls save()
 * and asserts PreferencesManager persisted them.
 */
public class JeddictPreferencesFullUITest extends ApplicationTest {

    @TempDir
    public static Path HOME;

    private JeddictPreferences ui;

    @BeforeAll
    public static void beforeAll() throws Exception {
        System.setProperty("user.home", HOME.toAbsolutePath().toString());
        Files.copy(Path.of("src/test/resources/settings/jeddict.json"), HOME.resolve("jeddict.json"));
        // initialize singleton
        PreferencesManager.getInstance();
    }

    @Override
    public void start(Stage stage) {
        ui = new JeddictPreferences();
        // ensure potentially-null properties have safe defaults before attaching view to scene
        ui.settings.set("model", "");

        javafx.scene.layout.StackPane root = new StackPane();
        Scene scene = new Scene(root, 1000, 800);
        stage.setScene(scene);
        stage.show();

        // attach preferences view after stage is shown to avoid PreferencesFx history NPEs
        javafx.application.Platform.runLater(() -> root.getChildren().add(ui.preferences.getView()));
    }

    @Test
    public void full_ui_save_persists_all_settings() {
        // Set virtually all settings through the SettingsProperty backing model on the FX thread
        interact(() -> {
            // Booleans
            ui.settings.set("enableAssistant", true);
            ui.settings.set("enableInlineCompletion", true);
            ui.settings.set("enableInlinePromptHint", true);
            ui.settings.set("enableInlineHintOnEnter", true);
            ui.settings.set("enableInlineHint", true);

            // Contexts
            ui.settings.set("classContext", AIClassContext.ENTIRE_PROJECT);
            ui.settings.set("varClassContext", AIClassContext.CURRENT_PACKAGE);

            // Provider / model / keys
            ui.settings.set("provider", GenAIProvider.OPEN_AI);
            ui.settings.set("model", "gpt-test-ui");
            ui.settings.set("apiKey", "ui-apikey-1");
            ui.settings.set("provider_location", "https://ui.example.local");

            // Numeric values
            ui.settings.set("temperature", 0.9);
            ui.settings.set("topP", 0.55);
            ui.settings.set("presencePenalty", 0.12);
            ui.settings.set("frequencyPenalty", 0.22);
            ui.settings.set("seed", 7);
            ui.settings.set("maxTokens", 1234);
            ui.settings.set("maxCompletionTokens", 2345);
            ui.settings.set("maxOutputTokens", 3456);
            ui.settings.set("topK", 11);

            // Stream / timeout / retries / org
            ui.settings.set("stream", true);
            ui.settings.set("timeout", 120);
            ui.settings.set("maxRetries", 4);
            ui.settings.set("organizationId", "org-ui-1");

            // Code execution
            ui.settings.set("allowCodeExecution", true);
            ui.settings.set("includeCodeExecutionOutput", true);

            // File lists
            ui.settings.set("fileExtensionToInclude", "java,kt,xml");
            ui.settings.set("excludeDirs", "node_modules,build,tmp");

            // Conversation context (string -> numeric mapping)
            ui.settings.set("conversationContext", "Include last 10 replies");

            // Rules and headers
            ui.settings.set("globalRules", "ui-rule-1");
            ui.settings.set("headers", "X-UI:abc\nY-UI:def");

            // Prompts - via promptsController items (simulate user adding via UI)
            try {
                java.lang.reflect.Field f = ui.getClass().getDeclaredField("promptsController");
                f.setAccessible(true);
                Object pc = f.get(ui);
                if (pc instanceof PromptsPanelController) {
                    PromptsPanelController controller = (PromptsPanelController) pc;
                    controller.items.clear();
                    controller.items.add(Map.entry("ui1", "UI Prompt One"));
                    controller.items.add(Map.entry("ui2", "UI Prompt Two\nWith newline"));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // Save
            ui.save();
        });

        // Verify PreferencesManager
        PreferencesManager pm = PreferencesManager.getInstance();
        then(pm.isAiAssistantActivated()).isTrue();
        then(pm.isSmartCodeEnabled()).isTrue();
        then(pm.isInlinePromptHintEnabled()).isTrue();
        then(pm.isInlineHintEnabled()).isTrue();
        then(pm.isHintsEnabled()).isTrue();

        then(pm.getClassContext()).isEqualTo(AIClassContext.ENTIRE_PROJECT);
        then(pm.getVarContext()).isEqualTo(AIClassContext.CURRENT_PACKAGE);

        then(pm.getProvider()).isEqualTo(GenAIProvider.OPEN_AI);
        then(pm.getModel()).isEqualTo("gpt-test-ui");
        then(pm.getApiKey()).isEqualTo("ui-apikey-1");
        then(pm.getProviderLocation()).isEqualTo("https://ui.example.local");

        then(pm.getTemperature()).isEqualTo(0.9);
        then(pm.getTopP()).isEqualTo(0.55);
        then(pm.getPresencePenalty()).isEqualTo(0.12);
        then(pm.getFrequencyPenalty()).isEqualTo(0.22);
        then(pm.getSeed()).isEqualTo(7);
        then(pm.getMaxTokens()).isEqualTo(1234);
        then(pm.getMaxCompletionTokens()).isEqualTo(2345);
        then(pm.getMaxOutputTokens()).isEqualTo(3456);
        then(pm.getTopK()).isEqualTo(11);

        then(pm.isStreamEnabled()).isTrue();
        then(pm.getTimeout()).isEqualTo(120);
        then(pm.getMaxRetries()).isEqualTo(4);
        then(pm.getOrganizationId()).isEqualTo("org-ui-1");

        then(pm.isAllowCodeExecution()).isTrue();
        then(pm.isIncludeCodeExecutionOutput()).isTrue();

        then(pm.getFileExtensionListToInclude()).containsExactly("java","kt","xml");
        then(pm.getExcludeDirs()).containsExactly("node_modules","build","tmp");

        then(pm.getConversationContext()).isEqualTo(10);

        then(pm.getGlobalRules()).isEqualTo("ui-rule-1");

        then(pm.getCustomHeaders()).containsEntry("X-UI","abc").containsEntry("Y-UI","def");

        then(pm.getPrompts()).containsEntry("ui1","UI Prompt One").containsEntry("ui2","UI Prompt Two\nWith newline");
    }
}

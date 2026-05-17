package io.github.jeddict.ai.settings;

import com.dlsc.preferencesfx.formsfx.view.renderer.PreferencesFxFormRenderer;
import com.dlsc.preferencesfx.model.Category;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import static io.github.jeddict.ai.models.registry.GenAIProvider.DEEPINFRA;import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import javafx.application.Platform;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxService;
import org.testfx.framework.junit5.ApplicationTest;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop._continue_;
import static ste.lloop.Loop.on;
import ste.netbeans.javafx.JFXPanel;

/**
 * Full UI test for entire JeddictPreferences panel using TestFX.
 * This replaces the previous settings-backed approach and performs real UI interactions
 * to set values as a user would, then calls save() and verifies PreferencesManager.
 */
public class JeddictPreferencesFullUITest extends ApplicationTest {

    private final Duration D500 = Duration.ofMillis(500);

    @TempDir
    public static Path HOME;
    private static Path configFile;

    private StackPane root;
    private JeddictPreferences preferences;

    @BeforeAll
    public static void beforeAll() throws Exception {
        System.setProperty("user.home", HOME.toAbsolutePath().toString());

        Path configPath = HOME.resolve(".config/jeddict");
        Files.createDirectories(configPath);

        configFile = configPath.resolve("jeddict-config.json");
        Files.copy(Path.of("src/test/resources/settings/jeddict.json"), configFile);
    }

    @AfterEach
    public void afterEach(final TestInfo info) throws Exception {
        final Image image =
            FxService.serviceContext().getCaptureSupport().captureNode(root);

        final String test = info.getTestMethod()
            .map(java.lang.reflect.Method::getName)
            .orElse("unknown");
        final Path dir = Paths.get("target", "screenshots");
        Files.createDirectories(dir);

        final Path screenshot = dir.resolve(test + '-' + System.currentTimeMillis() + ".png");
        FxService.serviceContext().getCaptureSupport().saveImage(image, screenshot);

        System.out.println("Screenshot saved " + screenshot);
    }

    @Override
    public void start(Stage stage) {
        preferences = new JeddictPreferences(); // it reads jeddict-config.json
        preferences.refresh();

        root = new StackPane();
        Scene scene = new Scene(root, 1000, 800);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> root.getChildren().add(preferences.getView()));
    }

    @Test
    public void full_ui_save_persists_all_settings() {
        // wait for UI to be attached (wait up to 500ms)
        waitForFxEvents();
        // Assistant category
        clickOn(preferences.asset("Assistant"));  waitForFxEvents();
        setToggle("AIAssistancePanel.aiAssistantActivationCheckBox.text", true);
        setToggle("AIAssistancePanel.enableSmartCodeCheckBox.text", true);
        setToggle("AIAssistancePanel.enableInlinePromptHintCheckBox.text", true);
        setToggle("AIAssistancePanel.enableInlineHintCheckBox.text", true);
        setToggle("AIAssistancePanel.enableHintsCheckBox.text", true);
        setToggle("AIAssistancePanel.enableDevelopment.text", true);

        // Inline Completion subcategory - set contexts
        clickOn(preferences.asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.tabTitle"));  waitForFxEvents();
        setComboBox("AIAssistancePanel.classContextLabel.text", AIClassContext.ENTIRE_PROJECT.toString());
        setComboBox("AIAssistancePanel.varContextLabel.text", AIClassContext.CURRENT_PACKAGE.toString());

        // Providers
        clickOn(preferences.asset("AIAssistancePanel.providersPane.TabConstraints.tabTitle"));  waitForFxEvents();

        // Provider combo uses display names
        setComboBox("AIAssistancePanel.providerLabel.text", DEEPINFRA.name());
        setText("AIAssistancePanel.apiKeyLabel.text", "ui-apikey-1");
        setText("AIAssistancePanel.providerLocationLabel.text", "http://localhost:7777");
        setComboBox("AIAssistancePanel.gptModelLabel.text", "aion-1.0-mini");

        // Inference settings (temperature/topP/topK/...)
        clickOn(preferences.asset("AIAssistancePanel.settings.inference.title"));  waitForFxEvents();
        setSlider("AIAssistancePanel.temperatureLabel.text", 0.5);
        setSlider("AIAssistancePanel.topPLabel.text", 0.2);
        setSlider("AIAssistancePanel.presencePenaltyLabel.text", 0.12);
        setSlider("AIAssistancePanel.frequencyPenaltyLabel.text", 0.22);
        setText("AIAssistancePanel.seedLabel.text", "7");
        setText("AIAssistancePanel.maxTokensLabel.text", "1234");
        setText("AIAssistancePanel.maxCompletionTokensLabel.text", "2345");
        setText("AIAssistancePanel.maxOutputTokensLabel.text", "3456");
        setText("AIAssistancePanel.topKLabel.text", "11");

        // Provider settings: stream/timeout/retries/headers
        clickOn(preferences.asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle"));
        waitForFxEvents(); setToggle("AIAssistancePanel.stream.text", true);
        setText("AIAssistancePanel.timeoutLabel.text", "120");
        setText("AIAssistancePanel.maxRetriesLabel.text", "4");
        setText("AIAssistancePanel.customHeadersLabel.text", "X-UI:abc\nY-UI:def", true);

        // Chat / Context
        clickOn(preferences.asset("AIAssistancePanel.askAIPane.TabConstraints.tabTitle")); waitForFxEvents();
        setComboBox("AIAssistancePanel.conversationContextLabel.text", preferences.asset("AIAssistancePanel.conversationContext.option.last_10_chats"));
        setToggle("AIAssistancePanel.excludeJavadocCommentsCheckBox.text", true);
        setText("AIAssistancePanel.fileExtLabel.text", "java,kt,xml");
        setText("AIAssistancePanel.excludeDir.text", "node_modules,build,tmp");

        // Global Rules
        clickOn(preferences.asset("AIAssistancePanel.globalRulesPane.TabConstraints.tabTitle"));waitForFxEvents();
        clickOn((TextArea)lookup(".simple-textarea").query()).write("ui-rule-1"); waitForFxEvents();

        // Prompts - ensure it exists (try clicking the tab; if not present, verify model contains the prompts view)
        boolean promptsClicked = true;
        try {
            clickOn(preferences.asset("AIAssistancePanel.promptSettingsPane.TabConstraints.tabTitle")); waitForFxEvents();
            Node promptsPanel = lookup("#promptsPanel").query();
            then(promptsPanel).isNotNull();
        } catch (Exception ex) {
            promptsClicked = false;
        }

        if (!promptsClicked) {
            // fallback: verify the prompts controller view is present in the Preferences model
            interact(() -> {
                try {
                    java.lang.reflect.Field f = preferences.getClass().getDeclaredField("promptsController");
                    f.setAccessible(true);
                    Object pc = f.get(preferences);
                    then(pc).isNotNull();
                    if (pc instanceof PromptsPanelController) {
                        then(((PromptsPanelController) pc).getView()).isNotNull();
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Save via backing save() method
        interact(() -> preferences.save());

        // Verify PreferencesManager
        PreferencesManager pm = PreferencesManager.getInstance();
        then(pm.isAiAssistantActivated()).isTrue();
        then(pm.isSmartCodeEnabled()).isTrue();
        then(pm.isInlinePromptHintEnabled()).isTrue();
        then(pm.isInlineHintEnabled()).isTrue();
        then(pm.isHintsEnabled()).isTrue();

        then(pm.getClassContext()).isEqualTo(AIClassContext.ENTIRE_PROJECT);
        then(pm.getVarContext()).isEqualTo(AIClassContext.CURRENT_PACKAGE);

        then(pm.getProvider()).isEqualTo(GenAIProvider.DEEPINFRA);
        then(pm.getModel()).isNotNull();
        then(pm.getApiKey()).isEqualTo("ui-apikey-1");
        then(pm.getProviderLocation()).isEqualTo("https://api.deepinfra.com/v1/openai");

        then(pm.getTemperature()).isEqualTo(0.5);
        then(pm.getTopP()).isEqualTo(0.2);
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

        then(pm.getFileExtensionListToInclude()).containsExactly("java","kt","xml");
        then(pm.getExcludeDirs()).containsExactly("node_modules","build","tmp");

        then(pm.getConversationContext()).isEqualTo(0);

        then(pm.getGlobalRules()).isEqualTo("ui-rule-1");

        then(pm.getCustomHeaders()).containsEntry("X-UI","abc").containsEntry("Y-UI","def");

        then(pm.isDevelopment()).isTrue();
        then(pm.isLogRequestsEnabled()).isTrue();
        then(pm.isLogResponsesEnabled()).isTrue();
    }

    @Test
    public void provider_selection_updates_endpoint_urls_model() {
        waitForFxEvents();
        clickOn(preferences.asset("AIAssistancePanel.providersPane.TabConstraints.tabTitle"));
        waitForFxEvents();

        // select OPEN_AI and verify endpoint updated
        setComboBox("AIAssistancePanel.providerLabel.text", GenAIProvider.ANTHROPIC.name());
        waitForFxEvents();
        Node node = findFieldControl(preferences.asset("AIAssistancePanel.providerLocationLabel.text"), ".text-field");
        then(node).isNull();
        then(getUrlText("#modelsUrl")).isEqualTo(GenAIProvider.ANTHROPIC.getModelInfoUrl());
        then(getUrlText("#apiKeyUrl")).isEqualTo(GenAIProvider.ANTHROPIC.getApiKeyUrl());

        node = findFieldControl(preferences.asset("AIAssistantPanel.gptModelLabel.text"), ".combo-box");
        then(((ComboBox)node).getValue()).isEqualTo("claude-3-haiku");

        // select CUSTOM_OPEN_AI and verify endpoint updated
        setComboBox("AIAssistancePanel.providerLabel.text", GenAIProvider.CUSTOM_OPEN_AI.name());
        waitForFxEvents();
        node = findFieldControl(preferences.asset("AIAssistancePanel.providerLocationLabel.text"), ".text-field");
        then(node).isNotNull();
        then(node.isVisible()).isTrue();
        then(((TextInputControl) node).getText()).isEqualTo("http://localhost/v1/openai");

        then(getUrlText("#modelsUrl")).isEmpty();
        then(getUrlText("#apiKeyUrl")).isEmpty();

        node = findFieldControl(preferences.asset("AIAssistantPanel.gptModelLabel.text"), ".combo-box");
        then(((ComboBox)node).getValue()).isEqualTo(null);
    }

    @Test
    public void getPanel_returns_a_JFXPanel() {
        then(preferences.getPanel()).isInstanceOf(JFXPanel.class);
    }

    @Test
    public void string_fields_show_empty_when_not_set() {
        final List<Category> categories = preferences.preferences.preferencesFxModel.getCategories();

        nullAllSettings(categories); waitForFxEvents();
        //
        // section Assistant
        //
        clickOn(preferences.asset("AIAssistancePanel.assistantPane.TabConstraints.tabTitle"));  waitForFxEvents();
        clickOn(preferences.asset("AIAssistancePanel.askAIPane.TabConstraints.tabTitle")); waitForFxEvents();

        // UI may be backed by PreferencesManager values; verify the settings property is empty when no stored value exists

        on("AIAssistancePanel.fileExtLabel.text", "AIAssistancePanel.excludeDir.text" ).loop((k) ->
            then(getFieldText(k)).isEqualTo("")
        );

        //
        // Section Providers
        //
        clickOn(preferences.asset("AIAssistancePanel.providersPane.TabConstraints.tabTitle"));
        on(
            "AIAssistancePanel.providerLocationLabel.text", "AIAssistantPanel.apiKeyLabel.text"
        ).loop(k ->
            then(getFieldText(k)).isEqualTo("")
        );

        //
        // Section Inference
        //
        clickOn(preferences.asset("AIAssistancePanel.settings.inference.title")); waitForFxEvents();
        on(
            "AIAssistancePanel.topKLabel.text", "AIAssistancePanel.seedLabel.text",
            "AIAssistancePanel.maxTokensLabel.text", "AIAssistancePanel.maxOutputTokensLabel.text",
            "AIAssistancePanel.maxCompletionTokensLabel.text"
        ).loop((k) ->
            then(getFieldText(k)).isEqualTo("0")
        );
        then(getFieldText("AIAssistancePanel.organizationIdLabel.text")).isEqualTo("");

        //
        // Section Provider Settings
        //
        clickOn(preferences.asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle")); waitForFxEvents();
        on(
            "AIAssistancePanel.timeoutLabel.text", "AIAssistancePanel.maxRetriesLabel.text"
        ).loop((k) ->then(getFieldText(k)).isEqualTo("0")
        );
        then(getFieldText("AIAssistancePanel.customHeadersLabel.text")).isEqualTo("");

        //
        // Section Global Rules
        //
        clickOn(preferences.asset("AIAssistancePanel.globalRulesPane.TabConstraints.tabTitle")); waitForFxEvents();
        then(((TextArea)lookup(".simple-textarea").query()).getText()).isEqualTo("");
    }

    @Test
    public void enable_disable_assistant_settings_when_assistance_is_enabled_disabled() {
        waitForFxEvents();

        //
        // Initially the toggle is ON
        //
        on(
            "AIAssistancePanel.enableSmartCodeCheckBox.text", "AIAssistancePanel.enableInlinePromptHintCheckBox.text",
            "AIAssistancePanel.enableInlineHintCheckBox.text", "AIAssistancePanel.enableHintsCheckBox.text"
        ).loop((k) -> {
            then(findFieldControl(preferences.asset(k), ".toggle-control").isDisabled()).isFalse();
        });

        //
        // Click on it to switch it OFF
        //
        clickOn(findFieldControl(preferences.asset("AIAssistancePanel.aiAssistantActivationCheckBox.text"), ".toggle-control"));
        waitForFxEvents();
        on(
            "AIAssistancePanel.enableSmartCodeCheckBox.text", "AIAssistancePanel.enableInlinePromptHintCheckBox.text",
            "AIAssistancePanel.enableInlineHintCheckBox.text", "AIAssistancePanel.enableHintsCheckBox.text"
        ).loop((k) -> {
            then(findFieldControl(preferences.asset(k), ".toggle-control").isDisabled()).isTrue();
        });
    }

    @Test
    public void use_default_values_when_settings_are_missing() throws Exception {
        Files.delete(configFile);
        Files.copy(
            Path.of("src/test/resources/settings/jeddict_minimal.json"), configFile
        );
        PreferencesManager.getInstance(true);
        preferences.refresh();

        waitForFxEvents();
        clickOn(preferences.asset("AIAssistancePanel.settings.inference.title")); waitForFxEvents();
        then(getFieldDouble("AIAssistancePanel.temperatureLabel.text")).isEqualTo(0);
        then(getFieldText("AIAssistancePanel.topKLabel.text")).isEqualTo("40");
        then(getFieldDouble("AIAssistancePanel.topPLabel.text")).isEqualTo(0.95);
        then(getFieldDouble("AIAssistancePanel.presencePenaltyLabel.text")).isEqualTo(0);
        then(getFieldDouble("AIAssistancePanel.frequencyPenaltyLabel.text")).isEqualTo(0);
        then(getFieldText("AIAssistancePanel.seedLabel.text")).isEqualTo("123");
        then(getFieldText("AIAssistancePanel.maxTokensLabel.text")).isEqualTo("4096");
        then(getFieldText("AIAssistancePanel.maxOutputTokensLabel.text")).isEqualTo("4096");
        then(getFieldText("AIAssistancePanel.maxCompletionTokensLabel.text")).isEqualTo("4096");

        clickOn(preferences.asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle")); waitForFxEvents();
        then(getFieldText("AIAssistancePanel.maxRetriesLabel.text")).isEqualTo("2");
        then(getFieldText("AIAssistancePanel.timeoutLabel.text")).isEqualTo("60");
    }

    // --------------------------------------------------------- private methods

    private Node findFieldControl(final String labelText, final String selector) {
        return on(lookup(labelText).queryAll()).loop((node) -> {
            try {
                if (!(node instanceof Label) || !node.isVisible()) {
                    _continue_();
                }

                final Label l = (Label)node;
                final Parent p = l.getParent();

                if (p instanceof PreferencesFxFormRenderer form) {
                    final int row = GridPane.getRowIndex(l), col = GridPane.getColumnIndex(l);

                    for (Node n : form.getChildren()) {
                        if ((GridPane.getRowIndex(n) == row) && (GridPane.getColumnIndex(n) == col+1)) {
                            _break_(n.lookup(selector));
                        }
                    }
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
        });
    }


    private void setToggle(final String key, final boolean value) {
        Node n = findFieldControl(preferences.asset(key), ".toggle-control");
        if (n instanceof ToggleSwitch) {
            interact(() -> ((ToggleSwitch) n).setSelected(value));
        } else if (n != null) {
            // try clicking the control
            clickOn(n);
        }
    }

    private void setSlider(final String key, final double value) {
        final HBox parent = (HBox)findFieldControl(preferences.asset(key), ".double-slider-control");
        final Slider s = (Slider)parent.getChildren().get(0);
        interact(() -> s.setValue(value));
    }

    private void setComboBox(final String key, final String item) {
        final Node n = findFieldControl(preferences.asset(key), ".combo-box");
        if (n instanceof ComboBox comboBox) {
            //clickOn(comboBox); waitForFxEvents();
            interact(() -> comboBox.show());
            clickOn(item); waitForFxEvents();
        }
    }

    private void setText(final String key, final String value) {
        setText(key, value, false);
    }

    private void setText(final String key, final String value, final boolean write) {
        Node n = findFieldControl(preferences.asset(key), ".text-field");
        if (!write && (n instanceof TextInputControl)) {
            TextInputControl t = (TextInputControl) n;
            interact(() -> t.setText(value));
        } else {
            // fallback: click on label then type
            clickOn(n);
            write(value);
        }
    }

    private String getFieldText(final String key) {
        final TextField text = (TextField)findFieldControl(
            preferences.asset(key), ".text-field"
        );

        if (text == null) {
            throw new IllegalArgumentException("no TextField found with key " + key);
        }

        return text.getText();
    }

    private Double getFieldDouble(final String key) {
        //
        // The slider is contained in a HBox
        //
        final HBox box = (HBox)findFieldControl(
            preferences.asset(key), ".double-slider-control"
        );

        if (box == null) {
            throw new IllegalArgumentException("no Spinner found with key " + key);
        }

        final Slider slider = (Slider)box.lookup(".slider.double-slider-control");

        return slider.getValue();
    }

    private String getUrlText(final String selector) {
        final Hyperlink link = lookup(selector).query();

        if ((link == null) || !link.isVisible()) {
            throw new IllegalArgumentException("no visible url found with " + selector + " (" + link + ")");
        }

        return link.getText();
    }

    private void nullAllSettings(final List<Category> categories) {
        on(categories).loop(category -> {
            nullAllSettings(category.getChildren());
            on(category.getGroups()).loop(group -> {
                on(group.getSettings()).loop(setting -> {
                    if (setting.valueProperty() != null) {
                        setting.valueProperty().setValue(null);
                    };
                });
            });
        });
    }

}

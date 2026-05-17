/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jeddict.ai.settings;

import atlantafx.base.theme.NordLight;
import atlantafx.base.util.BBCodeParser;
import com.dlsc.formsfx.model.structure.BooleanField;
import com.dlsc.formsfx.model.structure.DoubleField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.IntegerField;
import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.util.FieldTooltip;
import com.dlsc.formsfx.view.util.VisibilityProperty;
import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.view.NavigationView;
import com.dlsc.preferencesfx.view.PreferencesFxView;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import static io.github.jeddict.ai.models.registry.GenAIProvider.ANTHROPIC;
import static io.github.jeddict.ai.models.registry.GenAIProvider.GOOGLE;
import static io.github.jeddict.ai.models.registry.GenAIProvider.GROQ;
import static io.github.jeddict.ai.models.registry.GenAIProvider.MISTRAL;
import static io.github.jeddict.ai.models.registry.GenAIProvider.OPEN_AI;
import static io.github.jeddict.ai.models.registry.GenAIProvider.PERPLEXITY;
import java.awt.Desktop;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.HyperlinkLabel;
import static ste.lloop.Loop.on;
import ste.netbeans.javafx.JFXPanel;


/**
 *
 */
public class JeddictPreferences {

    public static final String CLASS_SETTING_CUSTOM_ELEMENT = "custom-element-control";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io.github.jeddict.ai.settings.Bundle");

    public static final StringConverter CONVERTER_STRING_STRING = new StringConverter<String>() {
        @Override
        public String fromString(final String s) {
            return StringUtils.defaultString(s);
        }

        @Override
        public String toString(final String s) {
            return StringUtils.defaultString(s);
        }
    };

    private static final StringConverter CONVERTER_STRING_INT = new StringConverter<Integer>() {
        @Override
        public Integer fromString(final String s) {
            return ((s == null) || (s.isBlank())) ? 0 : Integer.valueOf(s);
        }

        @Override
        public String toString(final Integer n) {
            if ((n == null) || (n == Integer.MIN_VALUE)){
                return "";
            }
            return String.valueOf(n);
        }
    };

    private static final StringConverter CONVERTER_STRING_DOUBLE = new StringConverter<Double>() {
        @Override
        public Double fromString(final String s) {
            return ((s == null) || (s.isBlank())) ? 0 : Double.valueOf(s);
        }

        @Override
        public String toString(final Double n) {
            if ((n == null) || (n == Double.MIN_VALUE)){
                return "";
            }
            return String.valueOf(n);
        }
    };

    final public SettingsProperty settings = new SettingsProperty();

    public PreferencesFx preferences;

    protected String asset(String key) {
        return BUNDLE.containsKey(key) ? BUNDLE.getString(key) : key;
    }

    public HyperlinkLabel configPathLabel;
    public Hyperlink modelsLink;
    public Hyperlink apiKeyLink;
    private Button clearCacheButton;
    private Button manageModelButton;


    public JeddictPreferences() {
    }

    private PromptsPanelController promptsController;
    private PreferencesFxView view;


    public JComponent getPanel() {
        final JFXPanel panel = new JFXPanel();

        //
        // With JafaFX keyboards events bubble up to the parent component;
        // this is a problem because for example if the user press ENTER in
        // a combobox, the event would make the setting panel close.
        // Let's prevent ENTER events to bubble up.
        //

        // --- SWING GLOBAL KEY INTERCEPTOR ---
        // This catches the ENTER key at the highest possible Swing level,
        // ensuring NetBeans or a JDialog never gets a chance to see it and close.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // If the key is ENTER and the focus is inside our JFXPanel...
                if (e.getKeyCode() == KeyEvent.VK_ENTER &&
                    SwingUtilities.isDescendingFrom(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), panel)) {

                    // 1. Send it down to JavaFX manually so the ComboBox can use it
                    Platform.runLater(() -> {
                        // Create an identical JavaFX KeyEvent
                        javafx.scene.input.KeyEvent fxEvent = new javafx.scene.input.KeyEvent(
                            panel.getScene(), // source
                            panel.getScene().getRoot(), // target
                            e.getID() == KeyEvent.KEY_PRESSED ? javafx.scene.input.KeyEvent.KEY_PRESSED : javafx.scene.input.KeyEvent.KEY_RELEASED,
                            "\r", "\r",
                            javafx.scene.input.KeyCode.ENTER,
                            e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown()
                        );
                        // Fire it down the scene graph
                        javafx.event.Event.fireEvent(panel.getScene().getFocusOwner(), fxEvent);
                    });

                    // 2. Return true to tell Swing: "This event is completely handled, destroy it."
                    return true;
                }
                return false; // Let all other keys pass through normally
            }
        });

        Platform.runLater(() -> {
            final Scene scene = new Scene(new StackPane(getView()));

            scene.getStylesheets().addAll(
                new NordLight().getUserAgentStylesheet(),
                "/ste/netbeans/javafx/bridge.css",
                "/io/github/jeddict/ai/settings/settings.css"
            );

            //ScenicView.show(scene);

            panel.setScene(scene);
        });

        return panel;
    }

    public PreferencesFxView getView() {
        if (view == null) {
            initComponents();

            view = preferences.getView();
            MasterDetailPane mdp = (MasterDetailPane)view.getCenter();
            mdp.resetDividerPosition();
        }

        return view;
    }

    public void refresh() {
        // initialize settings keys from current PreferencesManager so UI shows real defaults
        final PreferencesManager pm = PreferencesManager.getInstance();
        settings.bool("enableAssistant").set(pm.isAiAssistantActivated());
        settings.bool("enableInlineCompletion").set(pm.isSmartCodeEnabled());
        settings.bool("enableInlinePromptHint").set(pm.isInlinePromptHintEnabled());
        settings.bool("enableInlineHintOnEnter").set(pm.isInlineHintEnabled());
        settings.bool("enableInlineHint").set(pm.isHintsEnabled());
        settings.object("classContext").set(pm.getClassContext());
        settings.object("varClassContext").set(pm.getVarContext());
        settings.object("provider").set(pm.getProvider());
        settings.object("model").set(pm.getModel());
        settings.string("apiKeyUrl").set(pm.getProvider().getApiKeyUrl());
        settings.string("modelsUrl").set(pm.getProvider().getModelInfoUrl());
        settings.string("apiKey").set(pm.getApiKey());
        settings.string("provider_location").set(pm.getProviderLocation());
        settings.decimal("temperature").set(pm.getTemperature());
        settings.decimal("topP").set(pm.getTopP());
        settings.decimal("presencePenalty").set(pm.getPresencePenalty());
        settings.decimal("frequencyPenalty").set(pm.getFrequencyPenalty());
        settings.integer("seed").set(pm.getSeed());
        settings.integer("maxTokens").set(pm.getMaxTokens());
        settings.integer("maxCompletionTokens").set(pm.getMaxCompletionTokens());
        settings.integer("maxOutputTokens").set(pm.getMaxOutputTokens());
        settings.integer("topK").set(pm.getTopK());
        settings.bool("stream").set(pm.isStreamEnabled());
        settings.integer("timeout").set(pm.getTimeout());
        settings.integer("maxRetries").set(pm.getMaxRetries());
        settings.string("organizationId").set(pm.getOrganizationId());
        settings.string("fileExtensionToInclude").set(String.join(",", pm.getFileExtensionListToInclude()));
        settings.string("excludeDirs").set(String.join(",", pm.getExcludeDirs()));
        // map conversation context int -> label
        int convo = pm.getConversationContext();
        String convoLabel = switch (convo) {
            case 0 -> asset("AIAssistancePanel.conversationContext.option.no_past_chats");
            case 1 -> asset("AIAssistancePanel.conversationContext.option.last_chat");
            case 3 -> asset("AIAssistancePanel.conversationContext.option.last_3_chats");
            case 5 -> asset("AIAssistancePanel.conversationContext.option.last_5_chats");
            case 10 -> asset("AIAssistancePanel.conversationContext.option.last_10_chats");
            default -> asset("AIAssistancePanel.conversationContext.option.all_past_chats");
        };
        settings.object("conversationContext").set(convoLabel);
        settings.string("globalRules").set(pm.getGlobalRules());

        // headers map -> string lines
        final StringBuffer headers = new StringBuffer();
        on(pm.getCustomHeaders()).loop((key, value) -> {
            headers.append(key).append('=').append(value).append("\n");
        });
        settings.string("headers").set(headers.toString());
    }

    // --------------------------------------------------------- Private methods

    private void initComponents() {
        // lazily create JavaFX UI components to avoid initializing toolkit when running headless tests
        configPathLabel = new HyperlinkLabel(
            asset("AIAssistancePanel.configPathLabel.text") + " [%s]".formatted(configPath())
        );
        modelsLink = new Hyperlink(); modelsLink.setId("modelsUrl");
        modelsLink.setOnAction(event -> {
            modelsLink.setVisited(false); // Forces the link back to its unclicked default color
        });
        modelsLink.textProperty().bind(settings.string("modelsUrl"));

        apiKeyLink = new Hyperlink(); apiKeyLink.setId("apiKeyUrl");
        apiKeyLink.setOnAction(event -> {
            apiKeyLink.setVisited(false); // Forces the link back to its unclicked default color
        });
        apiKeyLink.textProperty().bind(settings.string("apiKeyUrl"));

        clearCacheButton = new Button(asset("AIAssistancePanel.cleanDataButton.text"));
        manageModelButton = new Button(asset("AIAssistancePanel.manageModelsButton.text"));

        //
        // Global Rules
        //
        final Setting<StringField, StringProperty> rulesSetting =
            Setting.of(null, settings.string("globalRules"));
        rulesSetting.getElement()
            .multiline(true).styleClass("global-rules-height")
            .tooltip(asset("AIAssistancePanel.globalRulesLabel.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        // initialize prompts controller with existing saved prompts so UI shows current prompts
        final PromptsPanelController ctrl = new PromptsPanelController(new java.util.HashMap<>(PreferencesManager.getInstance().getPrompts()));
        this.promptsController = ctrl;
        ctrl.table.setTooltip(new FieldTooltip(asset("AIAssistancePanel.promptTable.toolTipText")));

        preferences = PreferencesFx.of(JeddictPreferences.class,
            // Providers Assistant
            assistantCategory(),
            // Providers Category
            providersCategory(),
            // Global Rules category
            Category.of(asset("AIAssistancePanel.globalRulesPane.TabConstraints.tabTitle"),
                Group.of(asset("AIAssistancePanel.globalRulesPane.TabConstraints.tabTitle"),
                    rulesSetting
                )
            ),
            // Prompts category
            Category.of(
                asset("AIAssistancePanel.promptSettingsPane.TabConstraints.tabTitle"),
                Group.of(
                    asset("AIAssistancePanel.promptTable.titleText"),
                    Setting.of(ctrl.getView())
                )
            )
        ).crumbsVisibility(false);

        //
        // adapt a few things not properly handled or fully supported by preferencesfx
        //
        preferences.getView().parentProperty().addListener((obs, o, n) -> {
            if (n != null) {
                final MasterDetailPane mdp = (MasterDetailPane) preferences.getView().getCenter();
                final NavigationView nv = (NavigationView) mdp.getDetailNode();
                final TreeView<?> treeView = (TreeView<?>) nv.getChildren().get(1);
                mdp.setDividerPosition(0.25);
                on(clearCacheButton, manageModelButton, configPathLabel, modelsLink, apiKeyLink).loop((node) -> {
                    node.getStyleClass().add(CLASS_SETTING_CUSTOM_ELEMENT);
                    GridPane.setColumnIndex(node, 0);
                    GridPane.setColumnSpan(node, 2);
                    GridPane.setHalignment(node, HPos.RIGHT);
                    GridPane.setMargin(node, new Insets(10, 0, 0, 0));
                });

                if (treeView != null) {
                    treeView.getSelectionModel().select(0);
                }
            }
        });

        configPathLabel.setOnAction((action) -> {
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(configPath().toFile());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private Path configPath() {
        final String os = System.getProperty("os.name").toLowerCase();
        final Path userHome = Paths.get(System.getProperty("user.home"));

        if (os.contains("win")) {
            final String appData = System.getenv("APPDATA");
            final Path basePath;
            if (appData != null && !appData.isEmpty()) {
                basePath = Paths.get(appData);
            } else {
                basePath = userHome.resolve("AppData").resolve("Roaming");
            }

            return basePath.resolve("jeddict");
        } else if (os.contains("mac")) {
            return userHome.resolve("Library/Application Support").resolve("jeddict");
        } else if (os.contains("linux")) {
            return userHome.resolve(".config").resolve("jeddict");
        } else {
            return userHome;
        }
    }

    private Category assistantCategory() {
        final Setting<SingleSelectionField<AIClassContext>, ObjectProperty<AIClassContext>> classContextSetting
            = Setting.of(asset("AIAssistancePanel.classContextLabel.text"),
                FXCollections.observableArrayList(List.of(AIClassContext.values())),
                settings.object("classContext", AIClassContext.CURRENT_CLASS)
            );
        classContextSetting.getElement().tooltip(asset("AIAssistancePanel.classContextComboBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> activateAssistant
            = Setting.of(asset("AIAssistancePanel.aiAssistantActivationCheckBox.text"), settings.bool("enableAssistant", true));
        activateAssistant.getElement().tooltip(asset("AIAssistancePanel.aiAssistantActivationCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlineCompletion
            = Setting.of(asset("AIAssistancePanel.enableSmartCodeCheckBox.text"), settings.bool("enableInlineCompletion", false));
        enableInlineCompletion.getElement()
            .tooltip(asset("AIAssistancePanel.enableSmartCodeCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlinePromptHint
            = Setting.of(asset("AIAssistancePanel.enableInlinePromptHintCheckBox.text"), settings.bool("enableInlinePromptHint", false));
        enableInlinePromptHint.getElement().tooltip(asset("AIAssistancePanel.enableInlinePromptHintCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlineHintOnEnter
            = Setting.of(asset("AIAssistancePanel.enableInlineHintCheckBox.text"), settings.bool("enableInlineHintOnEnter", false));
        enableInlineHintOnEnter.getElement().tooltip(asset("AIAssistancePanel.enableInlineHintCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlineHint
            = Setting.of(asset("AIAssistancePanel.enableHintsCheckBox.text"), settings.bool("enableInlineHint", false));
        enableInlineHint.getElement().tooltip(asset("AIAssistancePanel.enableHintsCheckBox.toolTipText"));

        //
        // If assistant is activated, the assistant switches are enabled, if
        // assistant is de-activated the assistant switches are disabled
        //
        on(enableInlineCompletion, enableInlinePromptHint, enableInlineHintOnEnter, enableInlineHint)
        .loop(setting -> {
           setting.getElement().editableProperty().bind(activateAssistant.valueProperty());
        });

        final Setting<SingleSelectionField<AIClassContext>, ObjectProperty<AIClassContext>> varContextSetting
            = Setting.of(asset("AIAssistancePanel.varContextLabel.text"),
                FXCollections.observableArrayList(List.of(AIClassContext.values())),
                settings.object("varClassContext", AIClassContext.CURRENT_CLASS)
            );
        varContextSetting.getElement().tooltip(asset("AIAssistancePanel.varContextComboBox.toolTipText"));

        final Setting<SingleSelectionField<String>, ObjectProperty<String>> inlineCompletionShortcut
            = Setting.of(asset("AIAssistancePanel.aiInlineCompletionShortcutLabel.text"),
                FXCollections.observableArrayList(List.of("CTRL+SPACE", "CTRL+ALT+SPACE")),
                settings.object("inlineCompletionShortcut", "CTRL+SPACE")
            );
        inlineCompletionShortcut.getElement().tooltip(asset("AIAssistancePanel.aiInlineCompletionShortcut.toolTipText"));

        final Setting<BooleanField, BooleanProperty> showSnippetDescription
            = Setting.of(asset("AIAssistancePanel.showDescriptionCheckBox.text"),
                settings.bool("showSnippetDescription", false)
            );
        showSnippetDescription.getElement().tooltip(asset("AIAssistancePanel.showDescriptionCheckBox.toolTipText"));

        final Setting<SingleSelectionField<AIClassContext>, ObjectProperty<AIClassContext>> classContextInlineHintSetting
            = Setting.of(asset("AIAssistancePanel.classContextLabel1.text"),
                FXCollections.observableArrayList(List.of(AIClassContext.values())),
                settings.object("classContextInlineHint", AIClassContext.CURRENT_CLASS)
            );
        classContextInlineHintSetting.getElement().tooltip(asset("AIAssistancePanel.classContextInlineHintComboBox.toolTipText"));

        return Category.of(
            asset("AIAssistancePanel.assistantPane.TabConstraints.tabTitle"),
            Group.of("AI Assistant Settings",
                activateAssistant,
                enableInlineCompletion,
                enableInlinePromptHint,
                enableInlineHintOnEnter,
                enableInlineHint
            ),
            Group.of(
                asset("AIAssistancePanel.enableDevelopment.text"),
                Setting.of(
                    asset("AIAssistancePanel.enableDevelopment.text"),
                    settings.bool("development", false)
                )
            ),
            Group.of(
                "",
                Setting.of(configPathLabel)
            )
        ).expand()
            .subCategories(
                Category.of(
                    asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.tabTitle"),
                    Group.of(asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.tabTitle"),
                        classContextSetting,
                        varContextSetting,
                        inlineCompletionShortcut,
                        showSnippetDescription
                    ),
                    Group.of(asset("AIAssistancePanel.inlineHintPane.TabConstraints.tabTitle"),
                        classContextInlineHintSetting
                    ),
                    Group.of(Setting.of(new ClassContextLegend()))
                ),
                chatCategory()
            );
    }

    private Category chatCategory() {
        //
        // File Extension to Include in Context
        //
        final Setting<StringField, StringProperty> extensionsSetting
            = Setting.of(asset("AIAssistancePanel.fileExtLabel.text"), settings.string("fileExtensionToInclude"));
        extensionsSetting.getElement()
            .multiline(true)
            .tooltip(asset("AIAssistancePanel.fileExtLabel.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        //
        // Directories and Files to Exclude from Context
        //
        final Setting<StringField, StringProperty> excludeDirs
            = Setting.of(asset("AIAssistancePanel.excludeDir.text"), settings.string("excludeDirs"));
        excludeDirs.getElement()
            .multiline(true)
            .styleClass("height-m")
            .tooltip(asset("AIAssistancePanel.excludeDir.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        //
        // Conversation Context
        //
        final Setting<SingleSelectionField<String>, ObjectProperty<String>> conversationContextSetting =
            Setting.of(
                asset("AIAssistancePanel.conversationContextLabel.text"),
                FXCollections.observableArrayList(List.of(
                    asset("AIAssistancePanel.conversationContext.option.no_past_chats"),
                    asset("AIAssistancePanel.conversationContext.option.last_chat"),
                    asset("AIAssistancePanel.conversationContext.option.last_3_chats"),
                    asset("AIAssistancePanel.conversationContext.option.last_5_chats"),
                    asset("AIAssistancePanel.conversationContext.option.last_10_chats"),
                    asset("AIAssistancePanel.conversationContext.option.all_past_chats")
                )),
                settings.object("conversationContext", "Last 3 chats")
            );
        conversationContextSetting.getElement().tooltip(asset("AIAssistancePanel.conversationContextLabel.toolTipText"));


        final Setting<BooleanField, BooleanProperty> excludeJavadocSetting
            = Setting.of(
                asset("AIAssistancePanel.excludeJavadocCommentsCheckBox.text"), settings.bool("excludeJavadoc")
            );
        excludeJavadocSetting.getElement().tooltip(asset("AIAssistancePanel.excludeJavadocCommentsCheckBox.toolTipText"));

        final Setting<SingleSelectionField, ObjectProperty<String>> keyboardShortcut = Setting.of(
            asset("AIAssistancePanel.submitShortcutLabel.text"),
            FXCollections.observableArrayList(List.of("Enter", "Ctrl+Enter", "Shift+Enter")),
            settings.object("submitShortcut", "Enter")
        );
        keyboardShortcut.getElement().tooltip(asset("AIAssistancePanel.submitShortcutLabel.toolTipText"));

        final Setting<SingleSelectionField, ObjectProperty<String>> chatPlacement = Setting.of(
            asset("AIAssistancePanel.defaultAIAssistantPlacementLabel.text"),
            FXCollections.observableArrayList(List.of(
                asset("AIAssistancePanel.defaultAIAssistantPlacementOptions.left"),
                asset("AIAssistancePanel.defaultAIAssistantPlacementOptions.center"),
                asset("AIAssistancePanel.defaultAIAssistantPlacementOptions.right")
            )),
            settings.object("assistantPlacement", asset("AIAssistancePanel.defaultAIAssistantPlacementOptions.right"))
        );
        chatPlacement.getElement().tooltip(asset("AIAssistancePanel.defaultAIAssistantPlacementLabel.toolTipText"));


        return Category.of(
            asset("AIAssistancePanel.askAIPane.TabConstraints.tabTitle"),
            Group.of(
                "Chat Settings",
                keyboardShortcut,
                chatPlacement
            ),
            Group.of(
                "Context Settings",
                conversationContextSetting,
                excludeJavadocSetting,
                extensionsSetting,
                excludeDirs
            )
        );
    }

    private Category providersCategory() {
        //
        // Providers settingsa
        //

        //
        // Provider
        //
        final Setting<SingleSelectionField<GenAIProvider>, ObjectProperty<GenAIProvider>> providerSetting
            = Setting.of(asset("AIAssistancePanel.providerLabel.text"),
                FXCollections.observableArrayList(java.util.Arrays.asList(GenAIProvider.sortedValues())),
                settings.object("provider", PreferencesManager.getInstance().getProvider())
            );
        providerSetting.getElement().tooltip(asset("AIAssistancePanel.providerComboBox.toolTipText"));

        //
        // API Key
        //
        final Setting<PasswordField, StringProperty> apiKeySetting = Setting.of(
            asset("AIAssistancePanel.apiKeyLabel.text"),
            Field.ofPasswordType(settings.string("apiKey")),
            settings.string("apiKey")
        );
        apiKeySetting.getElement()
            .tooltip(asset("AIAssistancePanel.apiKeyLabel.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        //
        // Endpoint
        //
        final Setting<StringField, StringProperty> providerLocationSetting
            = Setting.of(
                asset("AIAssistancePanel.providerLocationLabel.text"),
                settings.string("provider_location"),
                VisibilityProperty.of(
                    settings.object("provider"),
                    (provider) -> {
                        if (provider == null) {
                            return true;
                        }
                        final Set preconfiguredProvider = Set.of(
                            ANTHROPIC, GOOGLE , GROQ, MISTRAL, OPEN_AI, PERPLEXITY
                        );

                        return !preconfiguredProvider.contains(provider);
                    }
                )
            );
        providerLocationSetting.getElement()
            .tooltip(asset("AIAssistancePanel.providerLocationLabel.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        //
        // Model
        //
        final ObservableList<String> models = FXCollections.observableArrayList();
        models.addAll(GenAIProvider.getModelsByProvider((GenAIProvider)settings.object("provider").get()));
        FXCollections.sort(models);

        final Setting<SingleSelectionField<String>, ObjectProperty<String>> modelSetting
            = Setting.of(
                asset("AIAssistancePanel.gptModelLabel.text"),
                models,
                settings.object("model")
            );
        modelSetting.getElement()
            .freeText(true)
            .tooltip(asset("AIAssistancePanel.gptModelLabel.toolTipText"));

        // Urls
        final PreferencesManager pm = PreferencesManager.getInstance();

        // Update API key, provider_location, models and model info when provider changes (assume provider property is GenAIProvider)
        settings.object("provider").addListener((obs, oldProv, newProv) -> {
            GenAIProvider gp = (GenAIProvider) newProv;
            // update api key and endpoint
            if (gp == null) {
                return;
            }
            settings.set("apiKey", pm.getApiKey(gp));
            settings.set("provider_location", pm.getProviderLocation(gp));
            settings.set("modelsUrl", gp.getModelInfoUrl());
            settings.set("apiKeyUrl", gp.getApiKeyUrl());

            // update model list for selected provider
            models.clear(); models.addAll(GenAIProvider.getModelsByProvider(gp));
            FXCollections.sort(models);
            modelSetting.getElement().select(0);
        });

        final Setting modelsUrlSetting = Setting.of(modelsLink);
        final Setting apiKeyUrlSetting = Setting.of(apiKeyLink);

        //
        // Headers
        //
        final Setting<StringField, StringProperty> headersSetting
            = Setting.of(asset("AIAssistancePanel.customHeadersLabel.text"), settings.string("headers"));
        headersSetting.getElement()
            .multiline(true).styleClass("height-m")
            .format(CONVERTER_STRING_STRING)
            .tooltip(asset("AIAssistancePanel.customHeadersLabel.toolTipText"));

        //
        // Inference settings
        //

        //
        // Temperature
        //
        final Setting<DoubleField, DoubleProperty> temperatureSetting
            = Setting.of(asset("AIAssistancePanel.temperatureLabel.text"), settings.decimal("temperature"), 0.0, 1.0, 2, null);
        temperatureSetting.getElement()
            .tooltip(asset("AIAssistancePanel.temperatureLabel.toolTipText"))
            .format(CONVERTER_STRING_DOUBLE);

        //
        // TopP
        //
        final Setting<DoubleField, DoubleProperty> topPSetting
            = Setting.of(asset("AIAssistancePanel.topPLabel.text"), settings.decimal("topP"), 0.0, 1.0, 2, null);
        topPSetting.getElement().tooltip(asset("AIAssistancePanel.topPLabel.toolTipText"))
                .format(CONVERTER_STRING_DOUBLE);

        //
        // TopK
        //
        final Setting<IntegerField, IntegerProperty> topKSetting
            = Setting.of(asset("AIAssistancePanel.topKLabel.text"), settings.integer("topK"));
        topKSetting.getElement().tooltip(asset("AIAssistancePanel.topKSetting.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Seed
        //
        final Setting<IntegerField, IntegerProperty> seedSetting
            = Setting.of(asset("AIAssistancePanel.seedLabel.text"), settings.integer("seed"));
        seedSetting.getElement().tooltip(asset("AIAssistancePanel.seedLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Max tokens
        //
        final Setting<IntegerField, IntegerProperty> maxTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxTokensLabel.text"), settings.integer("maxTokens"));
        maxTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxTokensLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Max output tokens
        //
        final Setting<IntegerField, IntegerProperty> maxOutputTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxOutputTokensLabel.text"), settings.integer("maxOutputTokens"));
        maxOutputTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxOutputTokensLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Max completion tokens
        //
        final Setting<IntegerField, IntegerProperty> maxCompletionTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxCompletionTokensLabel.text"), settings.integer("maxCompletionTokens"));
        maxCompletionTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxCompletionTokensLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Presense penalty
        //
        final Setting<DoubleField, DoubleProperty> presencePenaltySetting
            = Setting.of(asset("AIAssistancePanel.presencePenaltyLabel.text"), settings.decimal("presencePenalty"), -2.0, 2.0, 2, null);
        presencePenaltySetting.getElement()
            .tooltip(asset("AIAssistancePanel.presencePenaltyLabel.toolTipText"))
            .format(CONVERTER_STRING_DOUBLE);

        //
        // Frequence penalty
        //
        final Setting<DoubleField, DoubleProperty> frequencyPenaltySetting
            = Setting.of(asset("AIAssistancePanel.frequencyPenaltyLabel.text"), settings.decimal("frequencyPenalty"), -2.0, 2.0, 2, null);
        frequencyPenaltySetting.getElement()
            .tooltip(asset("AIAssistancePanel.frequencyPenaltyLabel.toolTipText"))
            .format(CONVERTER_STRING_DOUBLE);

        //
        // Streaming
        //
        final Setting<BooleanField, BooleanProperty> streamSetting
            = Setting.of(asset("AIAssistancePanel.stream.text"), settings.bool("stream", false));

        //
        // Connection timeout
        //
        final Setting<IntegerField, IntegerProperty> timeoutSetting
            = Setting.of(asset("AIAssistancePanel.timeoutLabel.text"), settings.integer("timeout"));
        timeoutSetting.getElement().tooltip(asset("AIAssistancePanel.timeoutLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Max retries
        //
        final Setting<IntegerField, IntegerProperty> maxRetriesSetting
            = Setting.of(asset("AIAssistancePanel.maxRetriesLabel.text"), settings.integer("maxRetries"));
        maxRetriesSetting.getElement().tooltip(asset("AIAssistancePanel.maxRetriesLabel.toolTipText"))
                .format(CONVERTER_STRING_INT);

        //
        // Organization identifier
        //
        final Setting<StringField, StringProperty> organizationIdSetting
            = Setting.of(asset("AIAssistancePanel.organizationIdLabel.text"), settings.string("organizationId"));
        organizationIdSetting.getElement()
            .tooltip(asset("AIAssistancePanel.organizationIdLabel.toolTipText"))
            .format(CONVERTER_STRING_STRING);

        final Tooltip tooltip = new Tooltip(asset("AIAssistancePanel.manageModelsButton.toolTipText"));
        tooltip.getStyleClass().add("simple-tooltip");
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        manageModelButton.setTooltip(tooltip);

        //
        // Build the category
        //
        return Category.of(
            asset("AIAssistancePanel.providersPane.TabConstraints.tabTitle"),
            Group.of(
                asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle"),
                providerSetting,
                providerLocationSetting,
                modelsUrlSetting,
                apiKeySetting,
                apiKeyUrlSetting,
                modelSetting,
                Setting.of(manageModelButton)
            )
        ).expand()
            .subCategories(
                Category.of(
                    asset("AIAssistancePanel.settings.inference.title"),
                    Group.of(
                        asset("AIAssistancePanel.settings.inference.group.title"),
                        temperatureSetting,
                        topPSetting,
                        topKSetting,
                        presencePenaltySetting,
                        frequencyPenaltySetting,
                        seedSetting,
                        maxTokensSetting,
                        maxOutputTokensSetting,
                        maxCompletionTokensSetting,
                        organizationIdSetting
                    )
                ),
                Category.of(
                    asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle"),
                    Group.of(
                        asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle"),
                        streamSetting,
                        timeoutSetting,
                        maxRetriesSetting,
                        headersSetting
                    )
                )
            );
    }

    public void save() {
        final PreferencesManager pm = PreferencesManager.getInstance();

        // Booleans (explicit casts so wrong types fail fast) - skip when not present
        Object v;
        v = settings.getValue("enableAssistant"); if (v != null) pm.setAiAssistantActivated((Boolean) v);
        v = settings.getValue("enableInlineCompletion"); if (v != null) pm.setSmartCodeEnabled((Boolean) v);
        v = settings.getValue("enableInlinePromptHint"); if (v != null) pm.setInlinePromptHintEnabled((Boolean) v);

        // Inline hint - prefer explicit key used by UI
        v = settings.getValue("enableInlineHintOnEnter"); if (v != null) pm.setInlineHintEnabled((Boolean) v);
        // Hints enabled
        v = settings.getValue("enableInlineHint"); if (v != null) pm.setHintsEnabled((Boolean) v);

        // Class / var contexts
        v = settings.getValue("classContext"); if (v != null) pm.setClassContext((AIClassContext) v);
        v = settings.getValue("classContext"); if (v != null) pm.setClassContextInlineHint((AIClassContext) v);
        v = settings.getValue("varClassContext"); if (v != null) pm.setVarContext((AIClassContext) v);

        // Provider and model
        Object prov = settings.getValue("provider");
        if (prov == null) {
            // nothing set in UI for provider; skip
        } else if (prov instanceof GenAIProvider) {
            pm.setProvider((GenAIProvider) prov);
        } else if (prov instanceof String) {
            String s = (String) prov;
            GenAIProvider found = GenAIProvider.OTHER;
            for (GenAIProvider p : GenAIProvider.values()) {
                if (p.name().replace("_", "").equalsIgnoreCase(s.replaceAll("\\s|_", ""))) {
                    found = p;
                    break;
                }
            }
            pm.setProvider(found);
        } else {
            throw new ClassCastException("Unsupported provider type: " + (prov == null ? "null" : prov.getClass()));
        }
        v = settings.getValue("model"); if (v != null) pm.setModel((String) v);
        v = settings.getValue("apiKey"); if (v != null) pm.setApiKey((String) v);
        v = settings.getValue("provider_location"); if (v != null) pm.setProviderLocation((String) v);

        // Numeric / double values
        v = settings.getValue("temperature"); if (v != null) pm.setTemperature(((Number) v).doubleValue());
        v = settings.getValue("topP"); if (v != null) pm.setTopP(((Number) v).doubleValue());
        v = settings.getValue("presencePenalty"); if (v != null) pm.setPresencePenalty(((Number) v).doubleValue());
        v = settings.getValue("frequencyPenalty"); if (v != null) pm.setFrequencyPenalty(((Number) v).doubleValue());
        v = settings.getValue("seed"); if (v != null) pm.setSeed(((Number) v).intValue());
        v = settings.getValue("maxTokens"); if (v != null) pm.setMaxTokens(((Number) v).intValue());
        v = settings.getValue("maxCompletionTokens"); if (v != null) pm.setMaxCompletionTokens(((Number) v).intValue());
        v = settings.getValue("maxOutputTokens"); if (v != null) pm.setMaxOutputTokens(((Number) v).intValue());
        v = settings.getValue("topK"); if (v != null) pm.setTopK(((Number) v).intValue());

        // Stream / timeout / retries
        v = settings.getValue("stream"); if (v != null) pm.setStreamEnabled((Boolean) v);
        v = settings.getValue("timeout"); if (v != null) pm.setTimeout(((Number) v).intValue());
        v = settings.getValue("maxRetries"); if (v != null) pm.setMaxRetries(((Number) v).intValue());
        v = settings.getValue("organizationId"); if (v != null) pm.setOrganizationId((String) v);

        // File extensions and excludes
        v = settings.getValue("fileExtensionToInclude"); if (v != null) pm.setFileExtensionToInclude((String) v);
        v = settings.getValue("excludeDirs"); if (v != null) pm.setExcludeDirs((String) v);

        // Conversation context mapping (string -> int)
        Object convo = settings.getValue("conversationContext");
        if (convo instanceof Number) {
            pm.setConversationContext(((Number) convo).intValue());
        } else if (convo instanceof String) {
            String s = (String) convo;
            s = s.toLowerCase();
            int ctx = 3; // default
            if (s.contains("no") || s.contains("don’t") || s.contains("don't")) ctx = 0;
            else if (s.contains("last reply") || s.contains("last chat")) ctx = 1;
            else if (s.contains("3")) ctx = 3;
            else if (s.contains("5")) ctx = 5;
            else if (s.contains("10")) ctx = 10;
            else if (s.contains("entire") || s.contains("all")) ctx = -1;
            pm.setConversationContext(ctx);
        }

        // Global rules
        pm.setGlobalRules((String) settings.getValue("globalRules"));

        // Headers parsing: support lines or comma separated pairs key:value
        final Properties p = new Properties();
        try {
            p.load(new StringReader(settings.string("headers").getValue()));
        } catch (IOException x) {}
        final Map<String, String> map = new HashMap();
        on(p.entrySet()).loop(entry -> {
            map.put((String)entry.getKey(), (String)entry.getValue());
        });
        pm.setCustomHeaders(map);

        // Prompts: if the PromptsPanelController exists, gather its items and save
        if (this.promptsController != null) {
            java.util.Map<String, String> prompts = new java.util.HashMap<>();
            this.promptsController.items.forEach(e -> {
                if (e.getKey() != null && !e.getKey().isEmpty()) prompts.put(e.getKey(), e.getValue());
            });
            pm.setPrompts(prompts);
        } else {
            // Fallback: if prompts were provided via SettingsProperty (headless tests), accept a Map
            Object maybePrompts = settings.getValue("prompts");
            if (maybePrompts instanceof java.util.Map) {
                java.util.Map<?,?> rawPrompts = (java.util.Map<?,?>) maybePrompts;
                java.util.Map<String,String> prompts = new java.util.HashMap<>();
                rawPrompts.forEach((k,val) -> {
                    if (k != null) prompts.put(k.toString(), val == null ? "" : val.toString());
                });
                pm.setPrompts(prompts);
            }
        }

        // Development mode
        pm.setDevelopment(settings.bool("development").getValue());
    }

    private class ClassContextLegend extends StackPane {
        public ClassContextLegend() {
            super();
            getChildren().add(BBCodeParser.createLayout("""
            [b]%s[/b] %s
            [b]%s[/b] %s
            [b]%s[/b] %s
            [b]%s[/b] %s""".formatted(
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.currentClass"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.currentClassDescription"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.referencedClasses"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.referencedClassesDescription"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.currentPackage"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.currentPackageDescription"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.project"),
                asset("AIAssistancePanel.inlineCompletionPane.TabConstraints.projectDescription")
            )));
            setId("class-context-legend");
        }
    }
}

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

import atlantafx.base.util.BBCodeParser;
import com.dlsc.formsfx.model.structure.BooleanField;
import com.dlsc.formsfx.model.structure.DoubleField;
import com.dlsc.formsfx.model.structure.IntegerField;
import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.util.FieldTooltip;
import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.view.NavigationView;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.HyperlinkLabel;
import static ste.lloop.Loop.on;

/**
 *
 */
public class JeddictPreferences {

    public static final String CLASS_SETTING_CUSTOM_ELEMENT = "custom-element-control";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io.github.jeddict.ai.settings.Bundle");

    final public SettingsProperty settings = new SettingsProperty();

    final public PreferencesFx preferences;

    private String asset(String key) {
        return BUNDLE.containsKey(key) ? BUNDLE.getString(key) : key;
    }

    final public HyperlinkLabel configPathLabel = new HyperlinkLabel(
        asset("AIAssistancePanel.configPathLabel.text") + " [%s]".formatted(configPath())
    );
    //final public HyperlinkLabel modelsUrl = new HyperlinkLabel("https://models");
    final public VBox modelsUrl = BBCodeParser.createLayout("[url=%s]%s[/url]".formatted("https://models", "https://models"));
    final public VBox apiKeyUrl = BBCodeParser.createLayout("[url=%s]%s[/url]".formatted("https://api-keys", "https://api-keys"));
    final private Button clearCacheButton = new Button(asset("AIAssistancePanel.cleanDataButton.text"));
    final private Button manageModelButton = new Button(asset("AIAssistancePanel.manageModelsButton.text"));

    public JeddictPreferences() {

        final Setting<StringField, StringProperty> rulesSetting =
            Setting.of(asset("AIAssistancePanel.globalRulesLabel.text"), settings.string("globalRules"));
        rulesSetting.getElement()
            .multiline(true)
            .tooltip(asset("AIAssistancePanel.globalRulesLabel.toolTipText"));

        final PromptsPanelController ctrl = new PromptsPanelController(new HashMap());
        ctrl.table.setTooltip(new FieldTooltip(asset("AIAssistancePanel.promptTable.toolTipText")));

        preferences = PreferencesFx.of(JeddictPreferences.class,
            // Providers Assistant
            assistantCategory(),
            // Providers Category
            providersCategory(),
            // Global Rules category
            Category.of(asset("AIAssistancePanel.globalRulesPane.TabConstraints.tabTitle"),
                Group.of("Rules",
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
                on(clearCacheButton, manageModelButton, configPathLabel, modelsUrl, apiKeyUrl).loop((node) -> {
                    node.getStyleClass().add(CLASS_SETTING_CUSTOM_ELEMENT);
                    GridPane.setColumnIndex(node, 0);
                    GridPane.setColumnSpan(node, 2);
                    GridPane.setHalignment(node, HPos.RIGHT);
                    GridPane.setMargin(node, new Insets(10, 0, 0, 0));
                });

                ((TextFlow)modelsUrl.getChildren().get(0)).setTextAlignment(TextAlignment.RIGHT);
                ((TextFlow)apiKeyUrl.getChildren().get(0)).setTextAlignment(TextAlignment.RIGHT);

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

    // --------------------------------------------------------- Private methods

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
        enableInlineCompletion.getElement().tooltip(asset("AIAssistancePanel.enableSmartCodeCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlinePromptHint
            = Setting.of(asset("AIAssistancePanel.enableInlinePromptHintCheckBox.text"), settings.bool("enableInlinePromptHint", false));
        enableInlinePromptHint.getElement().tooltip(asset("AIAssistancePanel.enableInlinePromptHintCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlineHintOnEnter
            = Setting.of(asset("AIAssistancePanel.enableInlineHintCheckBox.text"), settings.bool("enableInlineHintOnEnter", false));
        enableInlineHintOnEnter.getElement().tooltip(asset("AIAssistancePanel.enableInlineHintCheckBox.toolTipText"));

        final Setting<BooleanField, BooleanProperty> enableInlineHint
            = Setting.of(asset("AIAssistancePanel.enableHintsCheckBox.text"), settings.bool("enableInlineHint", false));
        enableInlineHint.getElement().tooltip(asset("AIAssistancePanel.enableHintsCheckBox.toolTipText"));
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
                settings.object("classContext", AIClassContext.CURRENT_CLASS)
            );
        classContextInlineHintSetting.getElement().tooltip(asset("AIAssistancePanel.classContextInlineHintComboBox.toolTipText"));

        return Category.of(
            "Assistant",
            Group.of("AI Assistant Settings",
                activateAssistant,
                enableInlineCompletion,
                enableInlinePromptHint,
                enableInlineHintOnEnter,
                enableInlineHint
            ),
            Group.of(
                "Development",
                Setting.of("Debug", settings.bool("debug", false))
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
        final Setting<StringField, StringProperty> extensionsSetting
            = Setting.of(asset("AIAssistancePanel.fileExtLabel.text"), settings.string("fileExtensionToInclude"));
        extensionsSetting.getElement().tooltip(asset("AIAssistancePanel.fileExtLabel.toolTipText"));
        extensionsSetting.getElement().multiline(true);

        final Setting<StringField, StringProperty> excludeDirs
            = Setting.of(asset("AIAssistancePanel.excludeDir.text"), settings.string("excludeDirs"));
        excludeDirs.getElement().multiline(true);
        excludeDirs.getElement().tooltip(asset("AIAssistancePanel.excludeDir.toolTipText"));

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
        final Setting<StringField, StringProperty> headersSetting
            = Setting.of("Headers", settings.string("headers"));
        headersSetting.getElement().multiline(true);

        final Setting<SingleSelectionField<String>, ObjectProperty<String>> providerSetting
            = Setting.of(asset("AIAssistancePanel.providerLabel.text"),
                FXCollections.observableArrayList(List.of("OpenAI", "Anthropic", "Mistral")), settings.object("provider", "")
            );
        providerSetting.getElement().tooltip(asset("AIAssistancePanel.providerComboBox.toolTipText"));

        final Setting<StringField, StringProperty> apiKeySetting
            = Setting.of(asset("AIAssistancePanel.apiKeyLabel.text"), settings.string("apiKey"));
        apiKeySetting.getElement().tooltip(asset("AIAssistancePanel.apiKeyLabel.toolTipText"));

        final Setting<StringField, StringProperty> providerLocationSetting
            = Setting.of(asset("AIAssistancePanel.providerLocationLabel.text"), settings.string("provider_location"));
        providerLocationSetting.getElement().tooltip(asset("AIAssistancePanel.providerLocationLabel.toolTipText"));

        final Setting<SingleSelectionField<String>, ObjectProperty<String>> modelSetting
            = Setting.of(
                asset("AIAssistancePanel.gptModelLabel.text"),
                FXCollections.observableArrayList(List.of("mini", "nano")),
                settings.object("model")
            );
        modelSetting.getElement().tooltip(asset("AIAssistancePanel.gptModelLabel.toolTipText"));

        final Setting<DoubleField, DoubleProperty> temperatureSetting
            = Setting.of(asset("AIAssistancePanel.temperatureLabel.text"), settings.decimal("temperature", 0.0), 0.0, 1.0, 2, null);
        temperatureSetting.getElement().tooltip(asset("AIAssistancePanel.temperatureLabel.toolTipText"));

        final Setting<DoubleField, DoubleProperty> topPSetting
            = Setting.of(asset("AIAssistancePanel.topPLabel.text"), settings.decimal("topP", 0.0), 0.0, 1.0, 2, null);
        topPSetting.getElement().tooltip(asset("AIAssistancePanel.topPLabel.toolTipText"));

        final Setting<IntegerField, DoubleProperty> topKSetting
            = Setting.of(asset("AIAssistancePanel.topKLabel.text"), settings.integer("topK", 0));
        topKSetting.getElement().tooltip(asset("AIAssistancePanel.topKSetting.toolTipText"));

        final Setting<IntegerField, IntegerProperty> seedSetting
            = Setting.of(asset("AIAssistancePanel.seedLabel.text"), settings.integer("seed", 0));
        seedSetting.getElement().tooltip(asset("AIAssistancePanel.seedLabel.toolTipText"));

        final Setting<IntegerField, IntegerProperty> maxTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxTokensLabel.text"), settings.integer("maxTokens", 5000));
        maxTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxTokensLabel.toolTipText"));

        final Setting<IntegerField, IntegerProperty> maxOutputTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxOutputTokensLabel.text"), settings.integer("maxOutputTokens", 5000));
        maxOutputTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxOutputTokensLabel.toolTipText"));

        final Setting<IntegerField, IntegerProperty> maxCompletionTokensSetting
            = Setting.of(asset("AIAssistancePanel.maxCompletionTokensLabel.text"), settings.integer("maxCompletionTokens", 5000));
        maxCompletionTokensSetting.getElement().tooltip(asset("AIAssistancePanel.maxCompletionTokensLabel.toolTipText"));

        final Setting<DoubleField, DoubleProperty> presencePenaltySetting
            = Setting.of(asset("AIAssistancePanel.presencePenaltyLabel.text"), settings.decimal("presencePenalty", 0.0), -2.0, 2.0, 2, null);
        presencePenaltySetting.getElement().tooltip(asset("AIAssistancePanel.presencePenaltyLabel.toolTipText"));

        final Setting<DoubleField, DoubleProperty> frequencyPenaltySetting
            = Setting.of(asset("AIAssistancePanel.frequencyPenaltyLabel.text"), settings.decimal("frequencyPenalty", 0.0), -2.0, 2.0, 2, null);
        frequencyPenaltySetting.getElement().tooltip(asset("AIAssistancePanel.frequencyPenaltyLabel.toolTipText"));

        final Setting<BooleanField, BooleanProperty> streamSetting
            = Setting.of(asset("AIAssistancePanel.stream.text"), settings.bool("stream", false));

        final Setting<IntegerField, IntegerProperty> timeoutSetting
            = Setting.of(asset("AIAssistancePanel.timeoutLabel.text"), settings.integer("timeout"));
        timeoutSetting.getElement().tooltip(asset("AIAssistancePanel.timeoutLabel.toolTipText"));

        final Setting<IntegerField, IntegerProperty> maxRetriesSetting
            = Setting.of(asset("AIAssistancePanel.maxRetriesLabel.text"), settings.integer("maxRetries"));
        maxRetriesSetting.getElement().tooltip(asset("AIAssistancePanel.maxRetriesLabel.toolTipText"));

        final Setting<StringField, StringProperty> organizationIdSetting
            = Setting.of(asset("AIAssistancePanel.organizationIdLabel.text"), settings.string("organizationId"));
        organizationIdSetting.getElement().tooltip(asset("AIAssistancePanel.organizationIdLabel.toolTipText"));

        final Tooltip tooltip = new Tooltip(asset("AIAssistancePanel.manageModelsButton.toolTipText"));
        tooltip.getStyleClass().add("simple-tooltip");
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        manageModelButton.setTooltip(tooltip);

        return Category.of(
            asset("AIAssistancePanel.providersPane.TabConstraints.tabTitle"),
            Group.of(
                asset("AIAssistancePanel.providerSettingsPane.TabConstraints.tabTitle"),
                providerSetting,
                providerLocationSetting,
                Setting.of(modelsUrl),
                apiKeySetting,
                Setting.of(apiKeyUrl),
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

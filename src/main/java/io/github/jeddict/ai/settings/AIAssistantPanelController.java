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

import io.github.jeddict.ai.models.GPT4AllModelFetcher;
import io.github.jeddict.ai.models.GroqModelFetcher;
import io.github.jeddict.ai.models.LMStudioModelFetcher;
import io.github.jeddict.ai.models.OllamaModelFetcher;
import io.github.jeddict.ai.models.PerplexityModelFetcher;
import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIModelRegistry;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import static io.github.jeddict.ai.models.Constant.DEEPINFRA_URL;
import static io.github.jeddict.ai.models.Constant.DEEPSEEK_URL;
import io.github.jeddict.ai.copilot.RunCopilotProxy;
import io.github.jeddict.ai.util.FileUtil;
import io.github.jeddict.ai.components.TokenUsageChartFactory;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.Lookup;
import static ste.lloop.Loop.on;

public class AIAssistantPanelController implements Initializable {

    @FXML private TabPane tabPane;

    // Providers Tab
    @FXML private ComboBox<GenAIProvider> providerComboBox;
    @FXML private VBox providerLocationPane;
    @FXML private TextField providerLocationField;
    @FXML private VBox apiKeyPane;
    @FXML private Hyperlink apiKeyInfo;
    @FXML private PasswordField apiKeyField;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private Hyperlink modelsInfo;
    @FXML private Button manageModelsButton;
    @FXML private Label modelHelp;
    @FXML private CheckBox aiAssistantActivationCheckBox;
    @FXML private CheckBox enableSmartCodeCheckBox;
    @FXML private CheckBox enableInlinePromptHintCheckBox;
    @FXML private CheckBox enableInlineHintCheckBox;
    @FXML private CheckBox enableHintsCheckBox;
    @FXML private Hyperlink configPathBtn;

    private final PreferencesManager preferencesManager = PreferencesManager.getInstance();
    private AIAssistantSettingsControllerFX optionsController;

    private static final Map<String, Integer> CONTEXT_OPTIONS = new LinkedHashMap<>();
    static {
        CONTEXT_OPTIONS.put("Don’t include past replies", 0);
        CONTEXT_OPTIONS.put("Include last reply", 1);
        CONTEXT_OPTIONS.put("Include last 3 replies", 3);
        CONTEXT_OPTIONS.put("Include last 5 replies", 5);
        CONTEXT_OPTIONS.put("Include last 10 replies", 10);
        CONTEXT_OPTIONS.put("Include entire conversation", -1);
    }

    private static final String DEFAULT_COPILOT_PROVIDER_LOCATION = "http://localhost:4141/v1";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupTables();
        setupListeners();
        setupHyperlinks();
        setupButtons();
    }

    public void setOptionsController(AIAssistantSettingsControllerFX optionsController) {
        this.optionsController = optionsController;
    }

    private void notifyChanged() {
        if (optionsController != null) {
            optionsController.changed();
        }
    }

    private void setupComboBoxes() {
        providerComboBox.setItems(FXCollections.observableArrayList(GenAIProvider.sortedValues()));
    }

    private void setupTables() {

    }

    private void setupListeners() {
        providerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleProviderChange(newVal);
                updateModelComboBox(newVal);
                updateProviderSettingsVisibility(newVal);
                notifyChanged();
            }
        });

        modelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateModelHelp(newVal);
                notifyChanged();
            }
        });

        aiAssistantActivationCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean activated = newVal;
            enableHintsCheckBox.setDisable(!activated);
            enableSmartCodeCheckBox.setDisable(!activated);
            enableInlineHintCheckBox.setDisable(!activated);
            enableInlinePromptHintCheckBox.setDisable(!activated);
            if (!activated) {
                enableHintsCheckBox.setSelected(false);
                enableSmartCodeCheckBox.setSelected(false);
                enableInlineHintCheckBox.setSelected(false);
                enableInlinePromptHintCheckBox.setSelected(false);
            }
            notifyChanged();
        });

        // Add listeners to notify changes for all relevant controls
        on(enableSmartCodeCheckBox, enableInlinePromptHintCheckBox, enableInlineHintCheckBox, enableHintsCheckBox).loop((cb) -> {
            cb.selectedProperty().addListener((obs, oldVal, newVal) -> notifyChanged());
        });

        providerLocationField.textProperty().addListener((obs, oldVal, newVal) -> notifyChanged());
        apiKeyField.textProperty().addListener((obs, oldVal, newVal) -> notifyChanged());
    }

    private void setupHyperlinks() {
        apiKeyInfo.setOnAction(e -> browse(apiKeyInfo.getText()));
        modelsInfo.setOnAction(e -> browse(modelsInfo.getText()));
        configPathBtn.setOnAction(e -> {
             Path configPath = FileUtil.getConfigPath();
             launchExplorer(configPath.toFile());
        });
    }

    private void setupButtons() {
        ContextMenu modelContextMenu = new ContextMenu();
        MenuItem addManuallyItem = new MenuItem("Add model manually");
        addManuallyItem.setOnAction(ev -> openAddModelManuallyDialog());
        MenuItem addFromRemoteItem = new MenuItem("Add models from remote");
        addFromRemoteItem.setOnAction(ev -> openAddModelsFromRemoteDialog());
        MenuItem removeSelectedItem = new MenuItem("Remove selected model");
        removeSelectedItem.setOnAction(ev -> {
            String selected = modelComboBox.getValue();
            if (selected != null) {
                modelComboBox.getItems().remove(selected);
                notifyChanged();
            }
        });
        MenuItem clearAllItem = new MenuItem("Clear all models");
        clearAllItem.setOnAction(ev -> {
            modelComboBox.getItems().clear();
            notifyChanged();
        });

        modelContextMenu.getItems().addAll(addManuallyItem, addFromRemoteItem, removeSelectedItem, new SeparatorMenuItem(), clearAllItem);

        manageModelsButton.setOnAction(e -> {
            modelContextMenu.show(manageModelsButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });
    }

    private void openAddModelManuallyDialog() {
        Dialog<GenAIModel> dialog = new Dialog<>();
        dialog.setTitle("Add Custom Model");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<GenAIProvider> providerCombo = new ComboBox<>(FXCollections.observableArrayList(GenAIProvider.values()));
        providerCombo.setValue(providerComboBox.getValue());
        TextField nameField = new TextField();
        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(3);
        TextField inputPriceField = new TextField("0.0");
        TextField outputPriceField = new TextField("0.0");

        grid.add(new Label("Provider:"), 0, 0);
        grid.add(providerCombo, 1, 0);
        grid.add(new Label("Model Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);
        grid.add(new Label("Input Price:"), 0, 3);
        grid.add(inputPriceField, 1, 3);
        grid.add(new Label("Output Price:"), 0, 4);
        grid.add(outputPriceField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    return new GenAIModel(
                        providerCombo.getValue(),
                        nameField.getText(),
                        descArea.getText(),
                        Double.parseDouble(inputPriceField.getText()),
                        Double.parseDouble(outputPriceField.getText())
                    );
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<GenAIModel> result = dialog.showAndWait();
        result.ifPresent(model -> {
            List<GenAIModel> models = preferencesManager.getGenAIModelList(model.provider().name());
            models.add(model);
            preferencesManager.setGenAIModelList(models, model.provider().name());
            if (providerComboBox.getValue() == model.provider()) {
                if (!modelComboBox.getItems().contains(model.name())) {
                    modelComboBox.getItems().add(model.name());
                }
            }
        });
    }

    private void openAddModelsFromRemoteDialog() {
        GenAIProvider selectedProvider = providerComboBox.getValue();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add models from remote");
        alert.setHeaderText("Fetching models for " + selectedProvider);
        alert.setContentText("Remote fetching is being processed...");
        alert.show();
    }

    private void handleProviderChange(GenAIProvider selectedProvider) {
        boolean showApiKey = false;
        boolean showLocation = false;
        String location = "";

        if (selectedProvider == GenAIProvider.DEEPINFRA
                || selectedProvider == GenAIProvider.DEEPSEEK
                || selectedProvider == GenAIProvider.GROQ
                || selectedProvider == GenAIProvider.CUSTOM_OPEN_AI) {
            showApiKey = true;
            showLocation = true;
            if (selectedProvider == GenAIProvider.DEEPINFRA) location = DEEPINFRA_URL;
            else if (selectedProvider == GenAIProvider.DEEPSEEK) location = DEEPSEEK_URL;
            else if (selectedProvider == GenAIProvider.GROQ) location = new GroqModelFetcher().getAPIUrl();
        } else if (selectedProvider == GenAIProvider.GOOGLE
                || selectedProvider == GenAIProvider.OPEN_AI
                || selectedProvider == GenAIProvider.MISTRAL
                || selectedProvider == GenAIProvider.ANTHROPIC) {
            showApiKey = true;
            showLocation = false;
        } else if (selectedProvider == GenAIProvider.COPILOT_PROXY) {
            showApiKey = false;
            showLocation = false;
        } else if (selectedProvider == GenAIProvider.PERPLEXITY) {
            showApiKey = true;
            showLocation = false;
            location = new PerplexityModelFetcher().getAPIUrl();
        } else {
            showApiKey = false;
            showLocation = true;
            switch (selectedProvider) {
                case OLLAMA -> location = new OllamaModelFetcher().getAPIUrl();
                case LM_STUDIO -> location = new LMStudioModelFetcher().getAPIUrl();
                case GPT4ALL -> location = new GPT4AllModelFetcher().getAPIUrl();
            }
        }

        apiKeyPane.setVisible(showApiKey);
        apiKeyPane.setManaged(showApiKey);
        providerLocationPane.setVisible(showLocation);
        providerLocationPane.setManaged(showLocation);
        if (!location.isEmpty()) {
            providerLocationField.setText(location);
        }

        apiKeyField.setText(preferencesManager.getApiKey(selectedProvider));

        String apiKeyUrl = selectedProvider.getApiKeyUrl();
        if (showApiKey && !apiKeyUrl.isEmpty()) {
            apiKeyInfo.setText(apiKeyUrl);
            apiKeyInfo.setVisible(true);
        } else {
            apiKeyInfo.setVisible(false);
        }

        String modelInfoUrl = selectedProvider.getModelInfoUrl();
        if (!modelInfoUrl.isEmpty()) {
            modelsInfo.setText(modelInfoUrl);
            modelsInfo.setVisible(true);
        } else {
            modelsInfo.setVisible(false);
        }

        RunCopilotProxy proxy = Lookup.getDefault().lookup(RunCopilotProxy.class);
        if (selectedProvider == GenAIProvider.COPILOT_PROXY) {
            if (!proxy.isRunning()) {
                proxy.startProxy((e) -> Platform.runLater(() -> updateModelComboBox(selectedProvider)));
            }
        } else {
            if (proxy.isRunning()) {
                proxy.closeProxy();
            }
        }
    }

    private void updateModelComboBox(GenAIProvider selectedProvider) {
        List<String> models = getModelList(selectedProvider);
        modelComboBox.setItems(FXCollections.observableArrayList(models));
        if (!models.isEmpty()) {
            modelComboBox.getSelectionModel().select(0);
        }
    }

    private List<String> getModelList(GenAIProvider selectedProvider) {
        Set<String> genAIModels = preferencesManager.getGenAIModelMap(selectedProvider.name()).keySet();
        List<String> models;
        if (genAIModels != null && !genAIModels.isEmpty()) {
            models = new ArrayList<>(genAIModels);
        } else {
            models = GenAIModelRegistry.getModels().values().stream()
                .filter(model -> model.provider().equals(selectedProvider))
                .map(GenAIModel::name)
                .collect(Collectors.toList());
        }
        Collections.sort(models);
        return models;
    }

    private void updateModelHelp(String modelName) {
        GenAIModel aIModel = preferencesManager.getGenAIModelByName(providerComboBox.getSelectionModel().getSelectedItem().name(), modelName);
        if (aIModel != null) {
            String descr = StringUtils.defaultIfBlank(aIModel.description(), aIModel.name());
            modelHelp.setText(StringUtils.abbreviate(descr, 100) + "...\nIn.Price:" + aIModel.inputPrice() + ", Out.Price:" + aIModel.outputPrice());
        } else {
            modelHelp.setText("");
        }
    }

    private void updateProviderSettingsVisibility(GenAIProvider selectedProvider) {
        manageModelsButton.setVisible(false);

        if(selectedProvider == GenAIProvider.CUSTOM_OPEN_AI
                || selectedProvider == GenAIProvider.GROQ
                || selectedProvider == GenAIProvider.COPILOT_PROXY
                || selectedProvider == GenAIProvider.GPT4ALL
                || selectedProvider == GenAIProvider.LM_STUDIO
                || selectedProvider == GenAIProvider.OLLAMA) {
            manageModelsButton.setVisible(true);
        }
    }

    public void load() {
        aiAssistantActivationCheckBox.setSelected(preferencesManager.isAiAssistantActivated());
        enableInlineHintCheckBox.setSelected(preferencesManager.isInlineHintEnabled());
        enableInlinePromptHintCheckBox.setSelected(preferencesManager.isInlinePromptHintEnabled());
        enableHintsCheckBox.setSelected(preferencesManager.isHintsEnabled());
        enableSmartCodeCheckBox.setSelected(preferencesManager.isSmartCodeEnabled());

        GenAIProvider provider = preferencesManager.getProvider();
        providerComboBox.setValue(provider);
        handleProviderChange(provider);
        modelComboBox.setValue(preferencesManager.getModel());

        int contextVal = preferencesManager.getConversationContext();
        String contextKey = CONTEXT_OPTIONS.entrySet().stream()
                .filter(entry -> entry.getValue() == contextVal)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Include last 3 replies");

        // Load Tables
        ObservableList<StringWrapper> excludeDirs = FXCollections.observableArrayList();
        preferencesManager.getExcludeDirs().forEach(dir -> excludeDirs.add(new StringWrapper(dir)));
        excludeDirs.add(new StringWrapper(""));

        ObservableList<HeaderEntry> headers = FXCollections.observableArrayList();
        preferencesManager.getCustomHeaders().forEach((k, v) -> headers.add(new HeaderEntry(k, v)));
        headers.add(new HeaderEntry("", ""));

        ObservableList<PromptEntry> prompts = FXCollections.observableArrayList();
        preferencesManager.getPrompts().forEach((k, v) -> prompts.add(new PromptEntry(k, v)));

        final Path configPath = FileUtil.getConfigPath();
        configPathBtn.setText(configPath.toAbsolutePath().toString());

        loadStats();
    }

    private void loadStats() {
        Platform.runLater(() -> {
            VBox statsBox = new VBox(10);
            SwingNode inputChartNode = new SwingNode();
            SwingNode outputChartNode = new SwingNode();
            SwingNode combinedChartNode = new SwingNode();

            TokenUsageChartFactory.resetTheme();
            inputChartNode.setContent(TokenUsageChartFactory.createInputChartPanel());
            outputChartNode.setContent(TokenUsageChartFactory.createOutputChartPanel());
            combinedChartNode.setContent(TokenUsageChartFactory.createCombinedChartPanel());

            statsBox.getChildren().addAll(combinedChartNode, inputChartNode, outputChartNode);
        });
    }

    public void store() {
        preferencesManager.setAiAssistantActivated(aiAssistantActivationCheckBox.isSelected());
        preferencesManager.setProvider(providerComboBox.getValue());
        preferencesManager.setModel(modelComboBox.getValue());
        preferencesManager.setModelList(new ArrayList<>(modelComboBox.getItems()));
        preferencesManager.setInlineHintEnabled(enableInlineHintCheckBox.isSelected());
        preferencesManager.setInlinePromptHintEnabled(enableInlinePromptHintCheckBox.isSelected());
        preferencesManager.setHintsEnabled(enableHintsCheckBox.isSelected());
        preferencesManager.setSmartCodeEnabled(enableSmartCodeCheckBox.isSelected());

        GenAIProvider selectedProvider = providerComboBox.getValue();
        if (selectedProvider != null) {
            switch (selectedProvider) {
                case CUSTOM_OPEN_AI:
                case DEEPINFRA:
                case DEEPSEEK:
                case GROQ:
                    preferencesManager.setApiKey(apiKeyField.getText());
                    preferencesManager.setProviderLocation(providerLocationField.getText());
                    break;
                case GOOGLE:
                case OPEN_AI:
                case MISTRAL:
                case ANTHROPIC:
                case PERPLEXITY:
                    preferencesManager.setApiKey(apiKeyField.getText());
                    break;
                case OLLAMA:
                case LM_STUDIO:
                case GPT4ALL:
                    preferencesManager.setProviderLocation(providerLocationField.getText());
                    break;
                case COPILOT_PROXY:
                    preferencesManager.setApiKey("Ignored");
                    preferencesManager.setProviderLocation(DEFAULT_COPILOT_PROVIDER_LOCATION);
                    break;
            }
        }
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.MIN_VALUE; }
    }

    private Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return Integer.MIN_VALUE; }
    }

    public boolean valid() {
        // Implement validation logic if needed
        return true;
    }

    private void browse(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void launchExplorer(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Helper classes for TableView
    public static class StringWrapper {
        private final SimpleStringProperty value;
        public StringWrapper(String value) { this.value = new SimpleStringProperty(value); }
        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public SimpleStringProperty valueProperty() { return value; }
    }

    public static class HeaderEntry {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;
        public HeaderEntry(String k, String v) {
            this.key = new SimpleStringProperty(k);
            this.value = new SimpleStringProperty(v);
        }
        public String getKey() { return key.get(); }
        public void setKey(String k) { key.set(k); }
        public SimpleStringProperty keyProperty() { return key; }
        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public SimpleStringProperty valueProperty() { return value; }
    }

    public static class PromptEntry {
        private final SimpleStringProperty name;
        private final SimpleStringProperty value;
        public PromptEntry(String n, String v) {
            this.name = new SimpleStringProperty(n);
            this.value = new SimpleStringProperty(v);
        }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public SimpleStringProperty valueProperty() { return value; }
    }
}

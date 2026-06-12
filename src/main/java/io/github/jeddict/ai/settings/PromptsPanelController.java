/**
 * Copyright 2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

import atlantafx.base.theme.Styles;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.Optional;
import javafx.scene.Node;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import animatefx.animation.ZoomIn;
import animatefx.animation.ZoomOut;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import static io.github.jeddict.ai.util.UIUtil.GLOBAL_STYLESHEETS;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class PromptsPanelController {
    public final TableView<Map.Entry<String, String>> table = new TableView<>();
    public final ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList();

    private final Map<String, String> prompts;

    private StackPane root;
    private VBox mainContainer;
    private VBox editContainer;

    private final StringProperty nameProperty = new SimpleStringProperty();
    private final StringProperty contentProperty = new SimpleStringProperty();
    private Form modelForm;
    private Button actionButton;

    private int editingIndex = -1;

    public PromptsPanelController(final Map<String, String> prompts) {
        this.prompts = prompts;
        items.setAll(prompts.entrySet());
        createTable();
        createEditContainer();
    }

    private void createEditContainer() {
        modelForm = Form.of(
            Group.of(
                Field.ofStringType(nameProperty)
                    .label("Name:")
                    .required("Name is required")
                    .id("name"),
                Field.ofStringType(contentProperty)
                    .label("Prompt:")
                    .required("Prompt is required")
                    .multiline(true)
                    .styleClass("height-m")
                    .id("prompt")
            )
        );

        FormRenderer formRenderer = new FormRenderer(modelForm);
        formRenderer.getStylesheets().add("/com/dlsc/preferencesfx/formsfx/view/renderer/style.css");
        formRenderer.getStylesheets().addAll(GLOBAL_STYLESHEETS);
        VBox.setVgrow(formRenderer, Priority.ALWAYS);

        actionButton = new Button("Create");
        actionButton.setId("action");
        actionButton.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED, Styles.ACCENT);
        actionButton.disableProperty().bind(modelForm.validProperty().not());

        ((TextField)formRenderer.lookup("#name .text-field")).textProperty().subscribe(name-> {
            Boolean existing = on(items).loop((entry) -> {
                if (entry.getKey().equals(name)) {
                    _break_(true);
                }
            });

            if ((existing != null) && existing) {
                actionButton.setText("Save");
            } else {
                actionButton.setText((editingIndex == -1) ? "Create" : "Save");
            }
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);

        HBox actions = new HBox(8, actionButton, cancel);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        editContainer = new VBox(formRenderer, actions);
        editContainer.setId("edit_prompt");
        editContainer.setPadding(new Insets(10));
        editContainer.getStyleClass().add("opaque-panel");
        editContainer.setVisible(false);
        editContainer.setManaged(false);

        actionButton.setOnAction(event -> {
            modelForm.persist();
            String n = nameProperty.get().trim();
            String c = contentProperty.get();
            final Map.Entry<String, String> e = Map.entry(n, c);

            // find if name already exists
            int existingIndex = -1;
            for (int i=0; i<items.size(); i++) {
                if (items.get(i).getKey().equals(n)) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex != -1) {
                prompts.put(n, c);
                items.set(existingIndex, e);
                if (editingIndex != -1 && existingIndex != editingIndex) {
                    // we renamed to an existing one, remove the old one
                    String oldKey = items.get(editingIndex).getKey();
                    prompts.remove(oldKey);
                    items.remove(editingIndex);
                }
            } else {
                prompts.put(n, c);
                if (editingIndex == -1) {
                    items.add(e);
                } else {
                    // rename
                    String oldKey = items.get(editingIndex).getKey();
                    prompts.remove(oldKey);
                    items.set(editingIndex, e);
                }
            }
            showTable();
        });

        cancel.setOnAction(e -> showTable());
    }

    private void showEditor(int index) {
        this.editingIndex = index;
        if (index == -1) {
            nameProperty.set("");
            contentProperty.set("");
        } else {
            Map.Entry<String, String> entry = items.get(index);
            nameProperty.set(entry.getKey());
            contentProperty.set(entry.getValue());
        }

        modelForm.reset(); // ensure validation state is cleared for new entry

        mainContainer.setVisible(false);
        mainContainer.setManaged(false);
        editContainer.setVisible(true);
        editContainer.setManaged(true);
        new ZoomIn(editContainer).setSpeed(3.0).play();
    }

    private void showTable() {
        ZoomOut zoomOut = new ZoomOut(editContainer);
        zoomOut.setSpeed(2.5);
        zoomOut.setOnFinished(e -> {
            editContainer.setVisible(false);
            editContainer.setManaged(false);
        });
        mainContainer.setVisible(true);
        mainContainer.setManaged(true);
        zoomOut.play();
    }

    private void createTable() {
        TableColumn<Map.Entry<String, String>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getKey()));
        nameCol.setMinWidth(200);
        nameCol.setMaxWidth(200);

        TableColumn<Map.Entry<String, String>, String> promptCol = new TableColumn<>("Prompt");
        promptCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(firstLine(cell.getValue().getValue())));

        promptCol.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setWrapText(false);
                lbl.setMaxWidth(Double.MAX_VALUE);
                setPrefHeight(Control.USE_COMPUTED_SIZE);
                setGraphic(lbl);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    lbl.setText(null);
                } else {
                    lbl.setText(item);
                }
            }

            @Override
            public void updateIndex(int i) {
                super.updateIndex(i);
                // attach click handler to open popup
                this.setOnMouseClicked(evt -> {
                    if (getItem() == null) return;
                    int row = getIndex();
                    if (row < 0 || row >= items.size()) return;
                    showEditor(row);
                });
            }
        });

        table.setPrefWidth(2000); table.setPrefHeight(375); // I could not find a way to do it with CSS
        table.setMaxWidth(Double.MAX_VALUE);
        table.getColumns().setAll(nameCol, promptCol);
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private String firstLine(String content) {
        if (content == null) return "";
        String[] lines = content.split("\\r?\\n", -1);
        String first = lines.length == 0 ? "" : lines[0];
        // For deterministic tests, clip to 200 chars and append ellipsis if longer
        if (first.length() > 200) {
            return first.substring(0, 197) + "...";
        }
        return first;
    }

    public Node getView() {
        if (root != null) return root;

        VBox.setVgrow(table, Priority.ALWAYS);
        HBox tableWrapper = new HBox(table);
        HBox.setHgrow(table, Priority.ALWAYS);
        tableWrapper.setMaxWidth(Double.MAX_VALUE);

        mainContainer = new VBox(tableWrapper);
        mainContainer.setId("prompts");
        mainContainer.setMaxWidth(Double.MAX_VALUE);
        mainContainer.getStyleClass().add("opaque-panel");
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        // add and delete button area
        Button add = new Button(null, new FontIcon(Feather.PLUS));
        add.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
        add.setId("add");
        add.setOnAction(e -> showEditor(-1));

        Button delete = new Button(null, new FontIcon(Feather.MINUS));
        delete.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
        delete.setId("delete");
        delete.setDisable(true);
        delete.setOnAction(e -> {
            Map.Entry<String, String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }

            ButtonType yesType = new ButtonType("Yes", ButtonBar.ButtonData.OTHER);
            ButtonType noType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Shall I really delete prompt '%s' ?".formatted(selected.getKey()),
                yesType, noType
            );
            final DialogPane dialog = confirm.getDialogPane();

            dialog.getStylesheets().addAll(GLOBAL_STYLESHEETS);
            dialog.lookupButton(yesType).getStyleClass().add(Styles.SMALL);
            dialog.lookupButton(noType).getStyleClass().add(Styles.SMALL);
            ((Label)dialog.lookup(".label.content")).setAlignment(Pos.CENTER_LEFT);

            confirm.setHeaderText(null);
            confirm.initStyle(StageStyle.UNDECORATED);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == yesType) {
                prompts.remove(selected.getKey());
                items.remove(selected);
            }
        });

        table.getSelectionModel().selectedIndexProperty().addListener((obs, o, v) -> {
            delete.setDisable(v == null || v.intValue() < 0);
        });

        HBox bottom = new HBox(add, delete);
        mainContainer.getChildren().add(bottom);

        // placeholder when there is no content
        add = new Button(null, new FontIcon(Feather.PLUS));
        add.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
        add.setId("table_placeholder");
        add.setOnAction(e -> showEditor(-1));

        HBox placeholder = new HBox(5); // 5px spacing
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getChildren().addAll(new Label("Press"), add, new Label("to create a prompt"));

        table.setPlaceholder(placeholder);

        root = new StackPane(mainContainer, editContainer);
        root.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(root, Priority.ALWAYS);

        return root;
    }

    // Test helper: return the preview text shown in the table for a given row
    public String getPreviewForRow(int index) {
        if (index < 0 || index >= items.size()) return "";
        return firstLine(items.get(index).getValue());
    }
}

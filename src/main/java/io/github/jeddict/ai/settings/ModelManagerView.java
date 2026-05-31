package io.github.jeddict.ai.settings;

import animatefx.animation.ZoomIn;
import animatefx.animation.ZoomOut;
import atlantafx.base.theme.Styles;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javax.swing.FocusManager;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class ModelManagerView extends HBox {

    public final ModelManagerModel model;

    public final ObjectProperty<GenAIProvider> providerProperty = new SimpleObjectProperty();
    public final ListProperty<GenAIModel> modelsProperty = new SimpleListProperty(FXCollections.observableArrayList());
    public final ObjectProperty<String> selectedModelProperty = new SimpleObjectProperty();
    public final StringProperty endpoint = new SimpleStringProperty();

    private final Button addButton = new Button(null, new FontIcon(Feather.PLUS));
    private final Button deleteButton = new Button(null, new FontIcon(Feather.MINUS));
    private final Button remoteButton = new Button(null, new FontIcon(Feather.DOWNLOAD_CLOUD));

    public ModelManagerView() {
        this.model = new ModelManagerModel();
        getChildren().addAll(addButton, deleteButton, remoteButton);
        setAlignment(Pos.TOP_RIGHT);
        setPadding(new Insets(0, 20, 0, 0));

        addButton.setId("add_model");
        addButton.setTooltip(new Tooltip("Add model"));
        addButton.setOnAction(event -> Platform.runLater(() -> addModel()));

        deleteButton.setId("delete_model");
        deleteButton.setTooltip(new Tooltip("Delete selected model"));
        deleteButton.setOnAction(event -> deleteModel());

        remoteButton.setId("remote_models");
        remoteButton.setTooltip(new Tooltip("Select models from remote"));
        remoteButton.setOnAction(event -> {
            final ModelUpdaterDialog updater = new ModelUpdaterDialog(
                FocusManager.getCurrentManager().getActiveWindow(),
                providerProperty.get(),
                endpoint.get(),
                modelsProperty.get()
            );

            updater.updateModels();
        });

        on(addButton, deleteButton, remoteButton).loop((button) -> {
            button.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
        });
    }

    public void addModel() {
        model.reset();
        StringField nameField = Field.ofStringType(model.nameProperty())
            .label("Model Name:")
            .required("Model name is required")
            .labelSpan(3)
            .id("name");

        Form modelForm = Form.of(
            Group.of(
                nameField,
                Field.ofStringType(model.descriptionProperty())
                    .label("Description:")
                    .multiline(true)
                    .styleClass("height-m")
                    .labelSpan(3)
                    .id("description"),
                Field.ofDoubleType(model.inputPriceProperty())
                    .label("Input Price:")
                    .labelSpan(3)
                    .id("input_price"),
                Field.ofDoubleType(model.outputPriceProperty())
                    .label("Output Price:")
                    .labelSpan(3)
                    .id("output_price")
            )
        ).title("Add New Model");

        final Parent parent = getParent();
        if (!(parent instanceof Pane paneParent) || !(parent.getParent() instanceof Pane container)) {
            return;
        }
        final FormRenderer formRenderer = new FormRenderer(modelForm);
        formRenderer.getStylesheets().add("/com/dlsc/preferencesfx/formsfx/view/renderer/style.css");
        VBox.setVgrow(formRenderer, javafx.scene.layout.Priority.ALWAYS);

        Button save = new Button("Save");
        save.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED, Styles.ACCENT);
        save.disableProperty().bind(nameField.validProperty().not());

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);

        HBox actions = new HBox(10, save, cancel);
        actions.setPadding(new Insets(10, 0, 0, 0));
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox formContainer = new VBox(formRenderer, actions);
        formContainer.setPadding(new Insets(10));
        formContainer.setMaxWidth(Double.MAX_VALUE);
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        save.setOnAction(e -> {
            modelForm.persist();
            final GenAIModel m = new GenAIModel(
                providerProperty.get(),
                model.nameProperty().get(),
                model.descriptionProperty().get(),
                model.inputPriceProperty().get(),
                model.outputPriceProperty().get()
            );
            Platform.runLater(() -> modelsProperty.get().add(m));
            returnToParent(formContainer, parent, container);
        });

        cancel.setOnAction(e -> returnToParent(formContainer, parent, container));

        container.getChildren().add(formContainer);

        paneParent.setVisible(false);
        paneParent.setManaged(false);

        new ZoomIn(formContainer).setSpeed(3.0).play();
    }

    private void returnToParent(Node form, Node parent, Pane container) {
        ZoomOut zoomOut = new ZoomOut(form);
        zoomOut.setSpeed(3.0);
        zoomOut.setOnFinished(e -> {
            container.getChildren().remove(form);
            parent.setVisible(true);
            parent.setManaged(true);
        });
        zoomOut.play();
    }

    public void deleteModel() {
        final GenAIModel selected = on(modelsProperty).loop((m) -> {
            if (m.fullName().equals(selectedModelProperty.get())) {
                _break_(m);
            }
        });

        modelsProperty.remove(selected);
    }
}

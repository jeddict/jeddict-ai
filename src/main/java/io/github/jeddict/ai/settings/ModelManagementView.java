package io.github.jeddict.ai.settings;

import atlantafx.base.theme.Styles;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import static io.github.jeddict.ai.util.UIUtil.GLOBAL_STYLESHEETS;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javax.swing.FocusManager;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class ModelManagementView extends HBox {

    public final ModelManagementModel model;

    public final ObjectProperty<GenAIProvider> providerProperty = new SimpleObjectProperty();
    public final ListProperty<GenAIModel> modelsProperty = new SimpleListProperty(FXCollections.observableArrayList());
    public final ObjectProperty<String> selectedModelProperty = new SimpleObjectProperty();
    public final StringProperty endpoint = new SimpleStringProperty();

    private final Button addButton = new Button(null, new FontIcon(Feather.PLUS));
    private final Button deleteButton = new Button(null, new FontIcon(Feather.MINUS));
    private final Button remoteButton = new Button(null, new FontIcon(Feather.DOWNLOAD_CLOUD));

    public ModelManagementView() {
        this.model = new ModelManagementModel();
        getChildren().addAll(addButton, deleteButton, remoteButton);
        setAlignment(Pos.TOP_RIGHT);

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
            .styleClass(Styles.SMALL)
            .labelSpan(3)
            .id("name");

        Form modelForm = Form.of(
            Group.of(
                nameField,
                Field.ofStringType(model.descriptionProperty())
                    .label("Description:")
                    .multiline(true)
                    .styleClass(".height-m ")
                    .labelSpan(3)
                    .id("description"),
                Field.ofDoubleType(model.inputPriceProperty())
                    .label("Input Price:")
                    .styleClass(Styles.SMALL)
                    .labelSpan(3)
                    .id("input_price"),
                Field.ofDoubleType(model.outputPriceProperty())
                    .label("Output Price:")
                    .styleClass(Styles.SMALL)
                    .labelSpan(3)
                    .id("output_price")
            )
        );

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Add Custom Model");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(GLOBAL_STYLESHEETS);

        dialogPane.setContent(new FormRenderer(modelForm));
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.lookupButton(ButtonType.OK).getStyleClass().addAll(Styles.SMALL);
        dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(nameField.validProperty().not());
        dialogPane.lookupButton(ButtonType.CANCEL).getStyleClass().addAll(Styles.SMALL);
        dialogPane.setPrefWidth(600);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            modelForm.persist();
            final GenAIModel m = new GenAIModel(
                providerProperty.get(),
                model.nameProperty().get(),
                model.descriptionProperty().get(),
                model.inputPriceProperty().get(),
                model.outputPriceProperty().get()
            );
            Platform.runLater(() -> modelsProperty.get().add(m));
        }
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

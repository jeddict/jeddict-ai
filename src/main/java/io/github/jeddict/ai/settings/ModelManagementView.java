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
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;

public class ModelManagementView extends MenuButton {

    public final ModelManagementModel model;

    public final ObjectProperty<GenAIProvider> providerProperty = new SimpleObjectProperty();
    public final ListProperty<GenAIModel> modelsProperty = new SimpleListProperty(FXCollections.observableArrayList());

    private final MenuItem addManuallyItem = new MenuItem("Add model manually");

    private Window owner = null;


    public ModelManagementView() {
        this.model = new ModelManagementModel();
        getItems().add(addManuallyItem);
        getItems().add(new MenuItem("Add models from remote"));

        addManuallyItem.setOnAction(event -> Platform.runLater(() -> addModelManually()));
    }

    public void addModelManually() {
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
        if (owner != null) {
            dialog.initOwner(owner);
        }
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
}

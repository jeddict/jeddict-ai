package io.github.jeddict.ai.settings;

import atlantafx.base.theme.Styles;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import static io.github.jeddict.ai.util.UIUtil.GLOBAL_STYLESHEETS;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

public class ModelManagementController {
    private final ModelManagementModel model;

    public ModelManagementController(ModelManagementModel model) {
        this.model = model;
    }

    public void addModelManually(Window owner) {
        StringField nameField = Field.ofStringType(model.nameProperty())
            .label("Model Name:")
            .required("Model name is required")
            .styleClass(Styles.SMALL)
            .labelSpan(3);

        Form modelForm = Form.of(
            Group.of(
                nameField,
                Field.ofStringType(model.descriptionProperty())
                    .label("Description:")
                    .multiline(true)
                    .styleClass(".height-m ")
                    .labelSpan(3),
                Field.ofDoubleType(model.inputPriceProperty())
                    .label("Input Price:")
                    .styleClass(Styles.SMALL)
                    .labelSpan(3),
                Field.ofDoubleType(model.outputPriceProperty())
                    .label("Output Price:")
                    .styleClass(Styles.SMALL)
                    .labelSpan(3)
            )
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Add Custom Model");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(GLOBAL_STYLESHEETS);

        dialogPane.setContent(new FormRenderer(modelForm));
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.lookupButton(ButtonType.OK).getStyleClass().addAll(Styles.SMALL);
        dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(nameField.validProperty().not());
        dialogPane.lookupButton(ButtonType.CANCEL).getStyleClass().addAll(Styles.SMALL);
        dialogPane.setPrefWidth(600);

        dialog.showAndWait();
    }
}

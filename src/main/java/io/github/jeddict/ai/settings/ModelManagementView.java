package io.github.jeddict.ai.settings;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

public class ModelManagementView extends MenuButton {

    private final ModelManagementController controller;
    private final ModelManagementModel model;

    public ModelManagementView() {
        this.model = new ModelManagementModel();
        this.controller = new ModelManagementController(model);

        MenuItem addModelManuallyItem = new MenuItem("Add model manually");
        addModelManuallyItem.setOnAction(e -> controller.addModelManually(getScene().getWindow()));
        getItems().add(addModelManuallyItem);

        getItems().add(new MenuItem("Add models from remote"));
    }
}

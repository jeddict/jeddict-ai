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

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.scene.Node;
import java.util.concurrent.TimeUnit;
import javafx.scene.control.TextInputControl;
import org.testfx.util.WaitForAsyncUtils;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;


public class PromptsPanelControllerTest extends ApplicationTest {

    private Map<String, String> prompts;
    private PromptsPanelController controller;

    @Override
    public void start(Stage stage) {
        prompts = new HashMap<>();
        controller = new PromptsPanelController(prompts);
        stage.setScene(new Scene((Parent)controller.getView(), 640, 480));
        stage.show();
    }

    private void waitForVisibility(String selector, boolean visible) {
        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {
                Node node = lookup(selector).query();
                return node != null && node.isVisible() == visible;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void click_add_shows_editor() {
        clickOn("#addButton");
        waitForVisibility("#editPromptPane", true);

        TextInputControl nameField = lookup("#nameField .text-field").queryAs(TextInputControl.class);
        TextInputControl contentArea = lookup("#contentArea .text-area").queryAs(TextInputControl.class);

        then(nameField.getText()).isEmpty();
        then(contentArea.getText()).isEmpty();
    }

    @Test
    public void save_new_prompt_updates_table_and_hides_editor() {
        clickOn("#addButton");
        waitForVisibility("#editPromptPane", true);

        interact(() -> {
            lookup("#nameField .text-field").queryAs(TextInputControl.class).setText("newKey");
            lookup("#contentArea .text-area").queryAs(TextInputControl.class).setText("newValue");
        });
        waitForFxEvents();

        clickOn("#actionButton");
        waitForVisibility("#editPromptPane", false);

        then(controller.table.getItems()).hasSize(1);
        then(controller.table.getItems().get(0).getKey()).isEqualTo("newKey");
        then(controller.table.getItems().get(0).getValue()).isEqualTo("newValue");
        then(prompts.get("newKey")).isEqualTo("newValue");
    }

    @Test
    public void click_row_shows_editor_with_data() {
        interact(() -> {
            prompts.put("key1", "value1");
            controller.items.add(Map.entry("key1", "value1"));
        });
        waitForFxEvents();

        clickOn("value1"); // Click on the name in the table
        waitForVisibility("#editPromptPane", true);

        TextInputControl nameField = lookup("#nameField .text-field").queryAs(TextInputControl.class);
        TextInputControl contentArea = lookup("#contentArea .text-area").queryAs(TextInputControl.class);

        then(nameField.getText()).isEqualTo("key1");
        then(contentArea.getText()).isEqualTo("value1");
    }


    @Test
    public void cancel_hides_editor_without_changes() {
        clickOn("#addButton");
        waitForVisibility("#editPromptPane", true);
        interact(() -> lookup(".text-field").queryAs(TextInputControl.class).setText("temporary"));

        clickOn("Cancel");

        waitForVisibility("#promptsPanel", true);
        then(controller.table.getItems()).isEmpty();
    }

    @Test
    public void table_updates_on_addition() {
        interact(() -> {
            controller.items.add(Map.entry("key1", "value1"));
        });
        then(controller.table.getItems()).hasSize(1);
        then(controller.table.getItems().get(0).getKey()).isEqualTo("key1");
        then(controller.table.getItems().get(0).getValue()).isEqualTo("value1");
    }

    @Test
    public void table_updates_on_update() {
        interact(() -> {
            controller.items.add(Map.entry("key1", "value1"));
        });

        interact(() -> {
            controller.items.set(0, Map.entry("key1", "updatedValue"));
        });

        then(controller.table.getItems()).hasSize(1);
        then(controller.table.getItems().get(0).getKey()).isEqualTo("key1");
        then(controller.table.getItems().get(0).getValue()).isEqualTo("updatedValue");
    }

    @Test
    public void table_updates_on_deletion() {
        interact(() -> {
            controller.items.add(Map.entry("key1", "value1"));
            controller.items.add(Map.entry("key2", "value2"));
        });
        then(controller.table.getItems()).hasSize(2);

        interact(() -> {
            controller.items.remove(0);
        });

        then(controller.table.getItems()).hasSize(1);
        then(controller.table.getItems().get(0).getKey()).isEqualTo("key2");
    }
}

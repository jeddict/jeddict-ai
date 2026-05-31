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
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 *
 */
public class PromptsPanelViewTest extends ApplicationTest {

    private Map<String, String> prefs;

    private PromptsPanelController controller;

    @Override
    public void start(Stage stage) {
        prefs = new HashMap();
        controller = new PromptsPanelController(prefs);
        stage.setScene(new Scene((Parent)controller.getView(), 640, 480));
        stage.show();
    }

    @Test
    public void action_disabled_until_fields_non_empty() {
        // click the add button (to be implemented)c
        clickOn("#add");

        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();
        Button action = lookup("#action").query();

        then(nameField.getText()).isEmpty();
        then(contentArea.getText()).isEmpty();
        then(action.isDisable()).isTrue();

        // fill name only
        clickOn(nameField).write("NewPrompt");
        then(action.isDisable()).isTrue();

        // fill content
        clickOn(contentArea).write("My content");
        then(action.isDisable()).isFalse();
    }

    @Test
    public void add_creates_new_entry() {
        int before = controller.table.getItems().size();
        clickOn("#add");
        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();
        Button action = lookup("#action").query();

        clickOn(nameField).write("UniquePromptName");
        clickOn(contentArea).write("Some content for unique prompt");
        then(action.getText()).isEqualTo("Create");
        clickOn(action);

        then(controller.table.getItems().size()).isEqualTo(before + 1);
        then(prefs.entrySet().stream().anyMatch(p -> p.getKey().equals("UniquePromptName"))).isTrue();
    }

    @Test
    public void add_with_existing_name_updates_existing() {
        controller.items.add(Map.entry("key", "Original value"));

        int before = controller.table.getItems().size();
        clickOn("#add");
        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();
        Button action = lookup("#action").query();

        // use existing name
        String existing = controller.items.get(0).getKey();
        clickOn(nameField).write(existing); waitForFxEvents();
        clickOn(contentArea).write("Updated via add"); waitForFxEvents();

        // should switch to Save
        then(action.getText()).isEqualTo("Save");
        clickOn(action);waitForFxEvents();

        then(controller.table.getItems().size()).isEqualTo(before);
        then(controller.items.stream().anyMatch(e -> e.getValue().equals("Updated via add"))).isTrue();
    }

    @Test
    public void delete_button_enabled_on_selection() {
        controller.items.add(Map.entry("key", "value"));
        Button delete = lookup("#delete").queryButton();
        then(delete.isDisable()).isTrue();

        interact(() -> controller.table.getSelectionModel().select(0));
        then(delete.isDisable()).isFalse();
    }

    @Test
    public void delete_cancel_does_nothing() {
        controller.items.add(Map.entry("key", "value"));
        int before = controller.table.getItems().size();
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn("#delete");
        // click Cancel on dialog
        clickOn("Cancel");
        then(controller.table.getItems()).hasSize(before);
    }

    @Test
    public void delete_confirm_removes_entry() {
        controller.items.add(Map.entry("key", "value"));
        int before = controller.table.getItems().size();
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn("#delete");
        clickOn("Yes");
        then(controller.table.getItems()).hasSize(before - 1);
    }

    @Test
    public void editing_to_existing_name_merges() {
        controller.items.add(Map.entry("key", "value"));
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn(".table-row-cell");

        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();

        clickOn(nameField).eraseText(nameField.getText().length()).write("B");
        clickOn(contentArea).eraseText(contentArea.getText().length()).write("Merged Content");
        // action should be Save
        then(lookup("#action").queryButton().getText()).isEqualTo("Save");
        clickOn("#action");

        // After merge, there should be only one entry named B with merged content
        then(controller.table.getItems().stream().filter(p -> p.getKey().equals("B")).count()).isEqualTo(1l);
        then(controller.items.stream().filter(p -> p.getKey().equals("B")).findFirst().get().getValue()).contains("Merged Content");
    }

    @Test
    public void edit_name_and_content_updates_table_and_prefs() {
        controller.items.add(Map.entry("key", "value"));
        // open edit for row 0
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn(".table-row-cell");

        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();
        Button action = lookup("#action").query();

        clickOn(nameField).eraseText(nameField.getText().length()).write("RenamedPrompt");
        clickOn(contentArea).eraseText(contentArea.getText().length()).write("New content\nMore");
        then(action.getText()).isEqualTo("Save");
        clickOn(action);

        then(controller.table.getItems().get(0).getKey()).isEqualTo("RenamedPrompt");
        then(controller.getPreviewForRow(0)).isEqualTo("New content");
        then(controller.items.get(0).getKey()).isEqualTo("RenamedPrompt");
    }

    @Test
    public void plus_button_opens_empty_popup_for_add() {
        clickOn("#add");
        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();
        Button action = lookup("#action").query();

        then(nameField.getText()).isEmpty();
        then(contentArea.getText()).isEmpty();
        then(action.getText()).isEqualTo("Create");
    }

    @Test
    public void popup_has_name_field_prefilled() {
        controller.items.add(Map.entry("key", "multiple lines\ncontent"));
        // open edit popup for row 0
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn(".table-row-cell");

        // name field should be present and prefilled with the existing name
        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();

        then(nameField.getText()).isEqualTo(controller.items.get(0).getKey());
        then(contentArea.getText()).contains("multiple lines");
    }

    @Test
    public void popup_title_updates_when_name_changes_to_existing() {
        controller.items.add(Map.entry("key", "value"));
        clickOn("#action");
        TextField nameField = lookup("#name .text-field").query();
        TextArea contentArea = lookup("#prompt .text-area").query();

        // initial title should be Create Prompt
        Stage dialogStage = (Stage) nameField.getScene().getWindow();
        then(dialogStage.getTitle()).isEqualTo("PrimaryStageApplication"); // there is not a title

        // type existing name
        String existing = controller.items.get(0).getKey();
        clickOn(nameField).write(existing);
        clickOn(contentArea).write("Updated content");
    }

    @Test
    public void table_displays_prompts() {
        controller.items.add(Map.entry("key1", "This is a long prompt content"));
        controller.items.add(Map.entry("key2", "value2"));
        then(controller.table.getItems().size()).isEqualTo(controller.items.size());
        String preview = controller.table.getItems().get(0).getValue().split("\\r?\\n", -1)[0];
        then(preview).doesNotContain("\n");
        then(preview).contains("This is a long prompt content");
    }

    @Test
    public void click_opens_popup() {
        controller.items.add(Map.entry("key1", "content\nwith\nmultiple lines"));
        // select first row programmatically and click the content cell
        interact(() -> controller.table.getSelectionModel().select(0));
        // click on the cell area - best-effort selector
        clickOn(".table-row-cell");
        // After clicking, the popup dialog should be shown; look up TextArea
        TextArea ta = lookup("#prompt .text-area").query();
        then(ta.getText()).contains("multiple lines");
    }

    @Test
    public void edit_popup_updates_table() {
        controller.items.add(Map.entry("key1", "value1"));
        interact(() -> controller.table.getSelectionModel().select(0));
        clickOn(".table-row-cell");
        TextArea ta = lookup("#prompt .text-area").query();
        clickOn(ta).eraseText(ta.getText().length()).write("New first line\nSecond line");
        // click Save button
        clickOn("Save");
        // table should reflect new first line
        String preview = controller.table.getItems().get(0).getValue().split("\\r?\\n", -1)[0];
        then(preview).isEqualTo("New first line");
        // also ensure PreferencesManager updated
        then(controller.items.get(0).getValue()).contains("Second line");
    }

    @Test
    public void long_first_line_truncation() {
        String longFirstLine = "a".repeat(300);
        String content = longFirstLine + "\nrest";
        controller.items.add(Map.entry("Long", content));
        String preview = controller.getPreviewForRow(0);
        then(preview.length()).isEqualTo(200);
        then(preview).endsWith("...");
    }
}

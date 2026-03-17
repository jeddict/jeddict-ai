/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
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

package io.github.jeddict.ai.components;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import static io.github.jeddict.ai.util.Icons.ICON_CHEVRON_DOWN;
import static io.github.jeddict.ai.util.Icons.ICON_CHEVRON_RIGHT;
import static io.github.jeddict.ai.util.Icons.ICON_TERMINAL;
import io.github.jeddict.ai.test.UITestBase;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolExecutionPaneTest extends UITestBase {

    private static final ToolExecutionRequest EXECUTION1 =
        ToolExecutionRequest.builder()
            .id("execution1").name("thisIsATool1")
            .arguments("{}")
            .build();
    private static final ToolExecutionRequest EXECUTION2 =
        ToolExecutionRequest.builder()
            .id("execution2").name("thisIsATool2")
            .arguments("{ \"a1\": \"v1\", \"a2\": \"v2\", \"a3\": \"v3\" }")
            .build();

    private static final String RESULT1 = "this is the first result";
    private static final String RESULT2 = "this is the second result";

    private ToolExecutionPane executionPane;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        frame.setTitle("Test ToolExecutionPane");
    }

    // ── Collapsed state (default) ──────────────────────────────────────────

    @Test
    public void initially_collapsed_shows_header_only() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        then(executionPane.isVisible()).isTrue();
        then(executionPane.isExpanded()).isFalse();

        window.panel("header").requireVisible();
        window.label("terminalIcon").requireText(ICON_TERMINAL);
        window.label("chevron").requireText(ICON_CHEVRON_RIGHT);

        // details panel exists but must not be visible (use FINDER so hidden panels are found)
        JPanel details = (JPanel) FINDER.findByName(executionPane, "details", false);
        then(details.isVisible()).isFalse();
    }

    @Test
    public void header_shows_human_readable_tool_name() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        // "thisIsATool1" → "This is a tool 1"
        window.label("toolName").requireText("This is a tool 1");
    }

    // ── Expanding ─────────────────────────────────────────────────────────

    @Test
    public void clicking_header_expands_details() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        GuiActionRunner.execute(() -> executionPane.toggle());

        then(executionPane.isExpanded()).isTrue();
        window.label("chevron").requireText(ICON_CHEVRON_DOWN);
    }

    @Test
    public void expanded_shows_result_text() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        GuiActionRunner.execute(() -> executionPane.toggle());

        // Both the result JPanel and JTextArea are named "result"; use UIUtil to find by type
        java.util.List<JTextArea> resultAreas = io.github.jeddict.ai.util.UIUtil.find(executionPane, JTextArea.class);
        then(resultAreas).hasSize(1);
        String text = GuiActionRunner.execute((java.util.concurrent.Callable<String>) resultAreas.get(0)::getText);
        then(text).isEqualTo(RESULT1);
    }

    @Test
    public void no_arguments_no_chip_when_expanded() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        GuiActionRunner.execute(() -> executionPane.toggle());

        then(FINDER.findAll(executionPane, (component) -> component instanceof ArgumentChip))
            .hasSize(0);
    }

    @Test
    public void one_chip_each_argument_when_expanded() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION2, RESULT2));

        GuiActionRunner.execute(() -> executionPane.toggle());

        then(FINDER.findAll(executionPane, (component) -> component instanceof ArgumentChip))
            .hasSize(3);
    }

    // ── Collapsing again ──────────────────────────────────────────────────

    @Test
    public void clicking_header_again_collapses_details() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        GuiActionRunner.execute(() -> executionPane.toggle());  // expand
        GuiActionRunner.execute(() -> executionPane.toggle());  // collapse

        then(executionPane.isExpanded()).isFalse();
        window.label("chevron").requireText(ICON_CHEVRON_RIGHT);

        JPanel details = (JPanel) FINDER.findByName(executionPane, "details", false);
        then(details.isVisible()).isFalse();
    }
}

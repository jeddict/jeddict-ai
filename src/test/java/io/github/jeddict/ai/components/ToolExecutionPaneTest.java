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
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.core.BasicComponentFinder;
import org.assertj.swing.core.BasicComponentPrinter;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.core.ComponentPrinter;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolExecutionPaneTest {

    private static final ComponentPrinter PRINTER = BasicComponentPrinter.printerWithCurrentAwtHierarchy();
    private static final ComponentFinder FINDER = BasicComponentFinder.finderWithCurrentAwtHierarchy();

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

    private FrameFixture window;
    private JFrame frame;
    private Container content;
    private ToolExecutionPane executionPane;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    void beforeEach() {
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle(("Test ToolExecutionPane"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content = frame.getContentPane();
        content.setPreferredSize(new Dimension(300, 300));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        window.cleanUp();
    }

    @Test
    public void no_arguments_no_chip() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));
        
        then(executionPane.isVisible()).isTrue();
        window.panel(executionPane.getName()).requireVisible();
        window.panel("arguments").requireVisible();
        window.panel("result").requireVisible();
        window.panel("result").textBox().requireText(RESULT1);
        
        then(FINDER.findAll(executionPane, (component) -> component instanceof ArgumentChip))
            .hasSize(0);
    }

    @Test
    public void one_chip_each_argument() {
        content.add(executionPane = new ToolExecutionPane(EXECUTION2, RESULT2));

        then(executionPane.isVisible()).isTrue();
        window.panel(executionPane.getName()).requireVisible();
        window.panel("arguments").requireVisible();
        window.panel("result").requireVisible();
        window.panel("result").textBox().requireText(RESULT2);
        
        then(FINDER.findAll(executionPane, (component) -> component instanceof ArgumentChip))
            .hasSize(3);
    }

}

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
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.BDDAssertions;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolExecutionPaneTest {

    private static final String VALUE100 = StringUtils.repeat("abcdefghij", 10);

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
    private static final ToolExecutionRequest EXECUTION3 =
        ToolExecutionRequest.builder()
            .id("execution3").name("thisIsATool3")
            .arguments("{\"arg1\":\"%s\",\"arg2\":\"val2\"}".formatted(VALUE100))
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
        content.add(executionPane = new ToolExecutionPane(EXECUTION1, RESULT1));

        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        window.cleanUp();
    }

    @Test
    public void inizialization() {
        then(executionPane.isVisible()).isTrue();
        window.panel("root").panel("headerPane").requireVisible();
        window.panel("root").panel("resultPane").requireVisible();
        window.panel("root").panel("headerPane").label("name").requireText(EXECUTION1.name());
        window.panel("root").panel("resultPane").textBox("result").requireText(RESULT1);
        BDDAssertions.thenThrownBy(() -> {
            window.panel("root").panel("headerPane").panel("chip");
        }).isInstanceOf(ComponentLookupException.class);
    }

    @Test
    public void one_chip_each_argument() {
        content.remove(executionPane);  // remove the old one
        content.add(executionPane = new ToolExecutionPane(EXECUTION2, RESULT2));

        for (int i=1; i<4; ++i) {
            window.panel("root").panel("headerPane").panel("a"+i).label().requireText(
                Pattern.compile(".*a%d:.*v%d.*".formatted(i, i))
            );
        }
        window.panel("root").panel("resultPane").textBox("result").requireText(RESULT2);
    }

    @Test
    public void abbreviate_long_arguments() {
        content.remove(executionPane);  // remove the old one
        content.add(executionPane = new ToolExecutionPane(EXECUTION3, RESULT2));

        then(window.panel("root").panel("headerPane").panel("arg1").label().text())
            .contains(StringUtils.abbreviateMiddle(VALUE100, " ... ", 80))
            .doesNotContain(VALUE100);
        window.panel("root").panel("headerPane").panel("arg2").label().requireText(".*val2.*");
    }
}

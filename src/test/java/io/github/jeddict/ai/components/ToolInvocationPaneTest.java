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
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_NAME;
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_VALUE;
import static io.github.jeddict.ai.components.ToolInvocationPane.ARGUMENT_SAMPLE_TOOL_NAME;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
public class ToolInvocationPaneTest {

    private static final String VALUE100 = StringUtils.repeat("abcdefghij", 10);
    private static final ComponentPrinter PRINTER = BasicComponentPrinter.printerWithCurrentAwtHierarchy();
    private static final ComponentFinder FINDER = BasicComponentFinder.finderWithCurrentAwtHierarchy();

    private static final Pair<String,String>[] ARGUMENTS1 = new Pair[] {
        Pair.of("a1", "v1"), Pair.of("a2", "v2"), Pair.of("a3", "v3")
    };

    private FrameFixture window;
    private JFrame frame;
    private Container content;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    void beforeEach() {
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle(("Test ArgumentPane"));
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
    public void no_argument_contructor_create_a_sample_argument_pane() {
        //
        // This is particularly useful to add the component to the GUI Builder
        // palette
        //
        final ToolInvocationPane pane = new ToolInvocationPane();
        content.add(pane);
        window.panel(ARGUMENT_SAMPLE_TOOL_NAME).requireVisible();
        window.panel("arguments").panel(CHIP_SAMPLE_ARGUMENT_NAME).requireVisible();
        window.panel("arguments").label("argument")
            .requireText(".*%s:.* %s.*".formatted(CHIP_SAMPLE_ARGUMENT_NAME, CHIP_SAMPLE_ARGUMENT_VALUE));
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(1);
    }
    
    @Test
    public void constructors_with_executions() {
        ToolInvocationPane pane = 
            new ToolInvocationPane("callMe", Map.of("phone", "+1234567890"));
        
        content.add(pane);
        
        window.panel("callMe").requireVisible();
        window.panel("arguments").panel("phone").requireVisible();
        window.panel("arguments").label("phone")
            .requireText(".*%s:.* %s.*".formatted("phone", "\\+1234567890"));
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(1);
        
        pane = new ToolInvocationPane(
            ToolExecutionRequest.builder()
            .name("callMeAgain").arguments("{\"phone\":\"+1234567890\",\"mobile\":\"+1222333444\"}")
            .build()
        );
        
        content.removeAll(); content.add(pane);
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(2);
    }
}

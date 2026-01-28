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
import java.beans.PropertyChangeEvent;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.assertj.swing.core.BasicComponentFinder;
import org.assertj.swing.core.BasicComponentPrinter;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.core.ComponentPrinter;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolExecutionConfirmationPaneTest {
    
    private static final ToolExecutionRequest EXEC_NO_ARGS =
        ToolExecutionRequest.builder().name("helloNoArgs").build();
    
    private static final ToolExecutionRequest EXEC_WITH_ARGS =
        ToolExecutionRequest.builder()
            .name("helloWithArgs")
            .arguments("{\"arg1\":\"value1\"}")
            .build();
    
    
    private static final ComponentPrinter PRINTER = BasicComponentPrinter.printerWithCurrentAwtHierarchy();

    private FrameFixture window;
    private JFrame frame;
    private ToolExecutionConfirmationPane confirmationPane;
    
    private ComponentFinder finder;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    void beforeEach() {
        finder = BasicComponentFinder.finderWithCurrentAwtHierarchy();
        
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle(("Test ToolExecutionConfirmationPane"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container content = frame.getContentPane();
        content.setPreferredSize(new Dimension(300, 300));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(confirmationPane = new ToolExecutionConfirmationPane());

        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    public void tearDown() {
      window.cleanUp();
    }

    @Test
    public void ToolExecutionConfirmationPane_initialization() {
        then(confirmationPane.isVisible()).isFalse();
        then(confirmationPane.getMaximumSize().width).isEqualTo(Integer.MAX_VALUE);
        then(confirmationPane.getMaximumSize().height).isEqualTo(confirmationPane.getPreferredSize().height);
    }

    @Test
    public void showVisible_shows_the_given_message() throws Exception {
        confirmationPane.showMessage(EXEC_NO_ARGS);
        
        window.panel(confirmationPane.getName()).requireVisible()
            .label("name").requireText("helloNoArgs");
        window.panel(confirmationPane.getName())
            .label("confirmationLabel").requireText(".* helloNoArgs.*");
        
        thenThrownBy(() -> finder.findByType(
            window.panel(confirmationPane.getName()).target(), ArgumentChip.class
        )).isInstanceOf(ComponentLookupException.class);

        confirmationPane.showMessage(EXEC_WITH_ARGS);
        window.panel(confirmationPane.getName()).requireVisible()
            .label("name").requireText("helloWithArgs");
        window.panel(confirmationPane.getName()).panel("arg1").requireVisible();
    }
    
    @Test
    public void execute_the_tool_on_Accept() throws Exception {
        final int[] action = new int[] { -1 };
        
        //
        // ACCEPT
        //
        confirmationPane.addPropertyChangeListener(
            JOptionPane.VALUE_PROPERTY,
            (PropertyChangeEvent e) -> {
                action[0] = (int)e.getNewValue();
            }
        );
        confirmationPane.showMessage(EXEC_NO_ARGS);
        
        window.button("acceptAction").click();
        
        then(action[0]).isEqualTo(JOptionPane.YES_OPTION);
        then(confirmationPane.isVisible()).isFalse();
        
        //
        // REJECT
        //
        confirmationPane.showMessage(EXEC_WITH_ARGS);
        
        window.button("rejectAction").click();
        
        then(action[0]).isEqualTo(JOptionPane.NO_OPTION);
        then(confirmationPane.isVisible()).isFalse();
    }

}

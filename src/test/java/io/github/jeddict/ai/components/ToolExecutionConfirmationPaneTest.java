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
import static io.github.jeddict.ai.components.ToolExecutionConfirmationPane.MAX_SIZE;
import java.awt.Container;
import java.awt.Dimension;
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Timeout;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolExecutionConfirmationPaneTest {

    private FrameFixture window;
    private JFrame frame;
    private ToolExecutionConfirmationPane confirmationPane;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    void beforeEach() {
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
    public void showVisible_shows_the_given_message_with_max_height() throws Exception {
        final String TEXT = "hello\n";

        confirmationPane.showMessage(TEXT);
        window.optionPane(Timeout.timeout(250)).requireVisible()
            .panel("OptionPane.realBody").textBox()
            .requireNotEditable()
            .requireText(TEXT);
        then(
            window.optionPane(Timeout.timeout(250)).scrollPane().target().getSize().height
        ).isEqualTo(MAX_SIZE.height);

        //
        // the message shall not expand over the max size
        //
        confirmationPane.showMessage(StringUtils.repeat(TEXT, 10));
        window.optionPane(Timeout.timeout(250)).requireVisible()
            .panel("OptionPane.realBody").textBox()
            .requireNotEditable()
            .requireText(Pattern.compile("(%s){10}".formatted(Pattern.quote(TEXT))));
        then(
            window.optionPane(Timeout.timeout(250)).scrollPane().target().getSize().height
        ).isEqualTo(MAX_SIZE.height);
    }
}

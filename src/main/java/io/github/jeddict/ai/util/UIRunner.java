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

package io.github.jeddict.ai.util;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.github.jeddict.ai.components.ToolExecutionConfirmationPane;
import io.github.jeddict.ai.components.ToolExecutionPane;
import io.github.jeddict.ai.components.ToolInvocationPane;
import static java.awt.BorderLayout.SOUTH;
import java.awt.Color;
import java.awt.Container;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 *
 */
public class UIRunner {

    private ToolExecutionConfirmationPane confirmationPane;
    private ToolInvocationPane invocationPane;
    private ToolExecutionPane executionPane;

    public static void main(final String[] args) {
        new UIRunner();
    }

    private UIRunner() {
        JFrame frame = new JFrame("Jeddict UI Testing Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        final Container content = frame.getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        final ToolExecutionRequest execution = ToolExecutionRequest.builder()
            .name("helloTool").arguments("{\"argument1\":\"value1\",\"argument2\":\"value2\"}").build();
        content.add(confirmationPane = new ToolExecutionConfirmationPane());
        content.add(invocationPane = new ToolInvocationPane());
        content.add(executionPane = new ToolExecutionPane(execution, "This is the result\n1\n2\n3\n4\n5"));
        content.setBackground(Color.white);

        frame.setVisible(true);
        
        final JPanel controls = new JPanel();
        
        confirmationPane.setBackground(Color.white);

        confirmationPane.showMessage(execution);
        
        confirmationPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, (e) -> {
            JOptionPane.showConfirmDialog(content, "Hello");
        });
        
        content.add(controls, SOUTH);
    }
}

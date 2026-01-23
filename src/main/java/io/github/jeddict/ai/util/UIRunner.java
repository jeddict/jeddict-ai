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

import io.github.jeddict.ai.components.ToolExecutionConfirmationPane;
import java.awt.BorderLayout;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 */
public class UIRunner {

    private ToolExecutionConfirmationPane confirmationPane;

    public static void main(final String[] args) {
        new UIRunner();
    }

    private UIRunner() {
        JFrame frame = new JFrame("Jeddict UI Testing Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        final Container content = frame.getContentPane();

        content.setLayout(new BorderLayout());
        content.add(confirmationPane = new ToolExecutionConfirmationPane(), CENTER);

        frame.setVisible(true);

        final JPanel controls = new JPanel();
        final JButton showConfirmationBtn = new JButton("Confirmation");

        showConfirmationBtn.addActionListener(e -> confirmationPane.showMessage(
        """
        one

        two

        three

        four

        five
        """));

        controls.add(showConfirmationBtn);

        content.add(controls, SOUTH);
    }
}

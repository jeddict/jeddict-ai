/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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

import io.github.jeddict.ai.components.AssistantChat;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Shiwani Gupta
 */
public class UIUtil {

    //
    // Colors
    //
    public static final Color COLOR_JEDDICT_MAIN = new Color(92, 159, 194);
    public static final Color COLOR_JEDDICT_ACCENT1 = new Color(0xd6, 0x33, 0x84);

    //
    // Fonts
    //
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_NORMAL_TEXT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 12);

    public static String queryToEnhance() {
        // Create a JTextArea for multiline input
        JTextArea textArea = new JTextArea(10, 30); // 10 rows, 30 columns
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        // Add the text area to a JScrollPane
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Create a JPanel to hold the scroll pane
        JPanel panel = new JPanel();
        panel.add(scrollPane);

        // Show the custom dialog
        int option = JOptionPane.showConfirmDialog(null,
                panel,
                "Please provide details about what to update in this method:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                AssistantChat.icon
        );

        // Check the user's choice
        if (option != JOptionPane.OK_OPTION) {
            return null; // Exit if the user cancels the input
        }

        String query = textArea.getText().trim();

        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Update details are required. Operation aborted.",
                    "No Input",
                    JOptionPane.ERROR_MESSAGE
            );
            return null; // Exit if no input is provided
        }
        return query;
    }

    public static String askForInitialCommitMessage() {
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        JPanel panel = new JPanel();
        panel.add(scrollPane);

        int option = JOptionPane.showConfirmDialog(null,
                panel,
                "Please enter the initial commit message (optional).",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                AssistantChat.icon
        );

        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        String initialMessage = textArea.getText().trim();
        if (initialMessage.isEmpty()) {
            return null;
        }

        return initialMessage;
    }

}

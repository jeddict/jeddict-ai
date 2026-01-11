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

import static io.github.jeddict.ai.util.EditorUtil.getFontFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 */
public class ToolExecutionConfirmationPane extends JOptionPane {

    protected static final Dimension MAX_SIZE = new Dimension(Integer.MAX_VALUE, 150);

    public ToolExecutionConfirmationPane() {
        super("", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);

        setVisible(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(92, 159, 194), 3),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setBackground(Color.WHITE);
        setForeground(getTextColorFromMimeType(MIME_PLAIN_TEXT));
        setOpaque(true);
        setFont(getFontFromMimeType(MIME_PLAIN_TEXT));
        setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    public void showMessage(final String message) {
        final JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setBackground(getBackground());
        textArea.setLineWrap(true);       // Wrap text to the next line
        textArea.setWrapStyleWord(true); // Wrap at word boundaries

        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(MAX_SIZE); scrollPane.setMaximumSize(MAX_SIZE);

        setMessage(scrollPane);
        setValue(JOptionPane.UNINITIALIZED_VALUE);
        setVisible(true);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

}

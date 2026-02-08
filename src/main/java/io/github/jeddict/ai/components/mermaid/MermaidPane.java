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
package io.github.jeddict.ai.components.mermaid;

import static io.github.jeddict.ai.components.mermaid.MermaidClassDiagramViewer.createMermaidClassDiagramView;
import static io.github.jeddict.ai.components.mermaid.MermaidERDViewer.createMermaidERDView;
import io.github.jeddict.ai.response.TextBlock;
import static io.github.jeddict.ai.util.ColorUtil.isDarkColor;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.EditorKit;

/**
 *
 * @author Gaurav Gupta
 */
public class MermaidPane extends JTabbedPane {

    public static JComponent createMermaidDiagramView(String mermaidText) {
        String normalized = mermaidText.trim().toLowerCase();

        if (normalized.contains("classdiagram")) {
            return createMermaidClassDiagramView(mermaidText);
        } else if (normalized.contains("erdiagram")) {
            return createMermaidERDView(mermaidText);
        } else {
            JLabel label = new JLabel("Unsupported or unknown diagram type.");
            label.setForeground(Color.RED);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(label, BorderLayout.CENTER);
            return panel;
        }
    }

    public JEditorPane createPane(final TextBlock content) {
        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        JTabbedPane tabbedPane = this;

        JPanel visualPanel = new JPanel();
        visualPanel.setLayout(new GridBagLayout()); // Center the canvas nicely

        visualPanel.add(createMermaidDiagramView(content.getContent()));
        visualPanel.setBackground(backgroundColor);
        tabbedPane.addTab("Mermaid", visualPanel);

        JEditorPane editorPane = new JEditorPane();
        EditorKit editorKit = createEditorKit(JAVA_MIME);
        editorPane.setEditorKit(editorKit);
        editorPane.setText(content.getContent());
        tabbedPane.addTab("Source", editorPane);
        final boolean[] reRender = {true};
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reRender[0] = true;
                content.setContent(editorPane.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reRender[0] = true;
                content.setContent(editorPane.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reRender[0] = true;
                content.setContent(editorPane.getText());
            }
        });
        boolean isDarkTheme = isDarkColor(backgroundColor);
        tabbedPane.setBackgroundAt(0, backgroundColor);
        tabbedPane.setBackgroundAt(1, backgroundColor);
        tabbedPane.setForegroundAt(0, textColor);
        tabbedPane.setForegroundAt(1, textColor);
        tabbedPane.setUI(new ColoredTabbedPaneUI(backgroundColor));

        Runnable updateCanvas = () -> {
            if (reRender[0]) {
                reRender[0] = false;
                visualPanel.removeAll();
                visualPanel.add(createMermaidDiagramView(content.getContent()));
                visualPanel.revalidate();
                visualPanel.repaint();
            }
        };

        SwingUtilities.invokeLater(updateCanvas);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                SwingUtilities.invokeLater(updateCanvas);
            }
        });
        return editorPane;
    }

    public class ColoredTabbedPaneUI extends BasicTabbedPaneUI {

        private final Color tabAreaBackground;

        public ColoredTabbedPaneUI(Color tabAreaBackground) {
            this.tabAreaBackground = tabAreaBackground;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected Insets getTabAreaInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(tabAreaBackground);
            g.fillRect(0, 0, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight));
            super.paintTabArea(g, tabPlacement, selectedIndex);
        }
    }


}

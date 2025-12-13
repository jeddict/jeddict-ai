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
package io.github.jeddict.ai.components;

import io.github.jeddict.ai.response.Block;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getHTMLContent;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import org.apache.batik.swing.JSVGCanvas;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 *
 * @author Gaurav Gupta
 */
public class MarkdownPane extends JTabbedPane {

    public static JEditorPane createHtmlPane(JEditorPane editorPane, String content, JComponent component) {
        editorPane.setBorder(BorderFactory.createEmptyBorder()); // No border
        editorPane.setMargin(new Insets(1, 0, 1, 0));
        editorPane.setContentType("text/html");
        editorPane.setEditorKit(new HTMLEditorKit());
        editorPane.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                try {
                    if (e.getURL() != null) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        editorPane.setEditable(false);
        editorPane.setText(getHTMLContent(getHtmlWrapWidth(component), content));
        return editorPane;
    }

    public static int getHtmlWrapWidth(JComponent component) {
        int width = component.getWidth() / 70;
        Insets insets = component.getInsets();
        width -= insets.left + insets.right;
        return Math.max(0, width);
    }

    public JEditorPane createPane(final Block content, JComponent component) {
        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        JTabbedPane tabbedPane = this;
        
        
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(parser.parse(content.getContent()));

        JEditorPane viewPane = new JEditorPane();
        createHtmlPane(viewPane, html, component);
        tabbedPane.addTab("View", viewPane);

        JEditorPane editorPane = new JEditorPane();
        EditorKit editorKit = createEditorKit(JAVA_MIME);
        editorPane.setEditorKit(editorKit);
        editorPane.setText(content.getContent());
        tabbedPane.addTab("Source", editorPane);
        
        final boolean[] reRender = {true};
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reRender[0]= true;
                content.setContent(editorPane.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reRender[0]= true;
                content.setContent(editorPane.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                reRender[0]= true;
                content.setContent(editorPane.getText());
            }
        });

        tabbedPane.setBackgroundAt(0, backgroundColor);
        tabbedPane.setBackgroundAt(1, backgroundColor);
        tabbedPane.setForegroundAt(0, textColor);
        tabbedPane.setForegroundAt(1, textColor);
        tabbedPane.setUI(new ColoredTabbedPaneUI(backgroundColor));


        Runnable updateViewer = () -> {
            String mdContent = editorPane.getText();
            if (reRender[0] == true) {
                reRender[0] = false;
                String newhtml = renderer.render(parser.parse(mdContent));
                viewPane.setText(getHTMLContent(getHtmlWrapWidth(component), newhtml));
            }
        };

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                SwingUtilities.invokeLater(updateViewer);
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

    private void addContextMenu(JSVGCanvas canvas, String svgContent) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openInBrowserItem = new JMenuItem("Open in Browser");

        openInBrowserItem.addActionListener(e -> {
            try {
                File tempFile = File.createTempFile("temp_svg_", ".svg");
                tempFile.deleteOnExit(); // Clean up later
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(svgContent);
                }
                Desktop.getDesktop().browse(tempFile.toURI());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(canvas, "Failed to open in browser: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        popupMenu.add(openInBrowserItem);

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
//                    openFullViewPopup(svgContent);
                }
            }
        });
    }

}

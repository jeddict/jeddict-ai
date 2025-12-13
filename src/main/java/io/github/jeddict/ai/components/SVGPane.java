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
import static io.github.jeddict.ai.util.ColorUtil.isDarkColor;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Dimension2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.EditorKit;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.util.XMLResourceDescriptor;

/**
 *
 * @author Gaurav Gupta
 */
public class SVGPane extends JTabbedPane {
    
    
    public JEditorPane createPane(final Block content) {
        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        JTabbedPane tabbedPane = this;

        JSVGCanvas canvas = new JSVGCanvas();
        canvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        canvas.setDisableInteractions(true);
        JPanel umlPanel = new JPanel();
        umlPanel.setLayout(new GridBagLayout()); // Center the canvas nicely
        umlPanel.add(canvas);
        umlPanel.setBackground(backgroundColor);
        tabbedPane.addTab("PlantUML", umlPanel);

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
        boolean isDarkTheme = isDarkColor(backgroundColor);
        tabbedPane.setBackgroundAt(0, backgroundColor);
        tabbedPane.setBackgroundAt(1, backgroundColor);
        tabbedPane.setForegroundAt(0, textColor);
        tabbedPane.setForegroundAt(1, textColor);
        tabbedPane.setUI(new ColoredTabbedPaneUI(backgroundColor));

        Runnable updateCanvas = () -> {
            String umlContent = editorPane.getText();
            if (reRender[0] == true) {
                reRender[0] = false;
                if (isDarkTheme) {
                    umlContent = addDarkTheme(umlContent, backgroundColor, textColor);
                }
                String svgContent = convertPlantUmlToSvg(umlContent);
                loadSVG(canvas, svgContent);
                addContextMenu(canvas, svgContent);
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

    public static String addDarkTheme(String content, Color backgroundColor, Color textColor) {
        String skinParams
                = """
            skinparam backgroundColor #1E1E1E
            skinparam shadowing false
            skinparam ArrowColor #CCCCCC
            skinparam ArrowFontColor #FFFFFF
            
            skinparam shadowing false
            
            ' Node styles
            skinparam rectangle {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            skinparam actor {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            skinparam usecase {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            skinparam class {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
                AttributeFontColor #DDDDDD
                MethodFontColor #DDDDDD
            }
            
            skinparam component {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            ' Arrows and connections
            skinparam ArrowColor #CCCCCC
            skinparam ArrowFontColor #FFFFFF
            
            ' Notes
            skinparam note {
                BackgroundColor #3C3C3C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            ' Titles
            skinparam title {
                FontColor #FFFFFF
            }
            
            ' Activity Diagrams
            skinparam activity {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            
            ' Sequence diagrams
            skinparam sequence {
                ActorBackgroundColor #2C2C2C
                LifeLineBorderColor #CCCCCC
                LifeLineBackgroundColor #2C2C2C
                ParticipantBackgroundColor #2C2C2C
                ParticipantBorderColor #CCCCCC
                ParticipantFontColor #FFFFFF
            }
            
            ' Swimlanes
            skinparam swimlane {
                BackgroundColor #2C2C2C
                BorderColor #CCCCCC
                FontColor #FFFFFF
            }
            """;

        if (textColor != null) {
            skinParams = skinParams.replace("White", "#" + Integer.toHexString(textColor.getRGB()).substring(2).toUpperCase());
        }
        if (backgroundColor != null) {
            skinParams = skinParams.replace("#1E1E1E", "#" + Integer.toHexString(backgroundColor.getRGB()).substring(2).toUpperCase());
        }

        int index = content.indexOf("@startuml");
        if (index == -1) {
            throw new IllegalArgumentException("Invalid PlantUML content: missing @startuml");
        }

        int insertPos = content.indexOf("\n", index);
        if (insertPos == -1) {
            // If no newline after @startuml, append after @startuml directly
            insertPos = index + "@startuml".length();
            return content.substring(0, insertPos) + "\n" + skinParams + content.substring(insertPos);
        } else {
            // Insert after the first line (@startuml line)
            return content.substring(0, insertPos + 1) + skinParams + content.substring(insertPos + 1);
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
                    openFullViewPopup(svgContent);
                }
            }
        });
    }

    private void openFullViewPopup(String svgContent) {
        JDialog dialog = new JDialog((Frame) null, "Full View", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JSVGCanvas fullViewCanvas = new JSVGCanvas();
        fullViewCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        fullViewCanvas.setDisableInteractions(false); // Allow zooming/panning in full view if you want

        dialog.getContentPane().add(new JScrollPane(fullViewCanvas)); // Add scroll if too big
        dialog.setSize(800, 600); // Or full screen: Toolkit.getDefaultToolkit().getScreenSize()
        dialog.setLocationRelativeTo(null); // Center on screen

        SwingUtilities.invokeLater(() -> loadSVG(fullViewCanvas, svgContent));

        dialog.setVisible(true);
    }

    public void loadSVG(JSVGCanvas svgCanvas, String svgContent) {
        try {
            // Convert the SVG string to InputStream
            InputStream inputStream = new ByteArrayInputStream(svgContent.getBytes("UTF-8"));
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);

            // Create the SVGDocument
            org.w3c.dom.svg.SVGDocument svgDocument = factory.createSVGDocument("http://www.w3.org/2000/svg", inputStream);
            svgCanvas.setSVGDocument(svgDocument);

            svgCanvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
                @Override
                public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        Dimension2D docSize = svgCanvas.getSVGDocumentSize();
                        if (docSize != null) {
                            int width = (int) Math.ceil(docSize.getWidth());
                            int height = (int) Math.ceil(docSize.getHeight());
                            svgCanvas.setPreferredSize(new Dimension(width, height));
                            svgCanvas.revalidate();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a PlantUML string to SVG format.
     *
     * @param plantUmlString The PlantUML string to be converted.
     * @return The SVG representation of the PlantUML string, or null if
     * conversion fails.
     */
    public String convertPlantUmlToSvg(String plantUmlString) {
        // Create a ByteArrayOutputStream to hold the SVG output
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Create a SourceStringReader with the PlantUML string
            SourceStringReader reader = new SourceStringReader(plantUmlString);

            // Output the image in SVG format
            reader.outputImage(outputStream, new FileFormatOption(FileFormat.SVG));

            // Return the SVG as a string
            String svgContent = outputStream.toString("UTF-8");
            svgContent = svgContent.replaceAll("text-decoration\\s*=\\s*\"wavy underline\"", "text-decoration=\"underline\"");
            return svgContent;
        } catch (IOException e) {
            e.printStackTrace(); // Handle exceptions appropriately in your application
        }
        return null; // Return null if conversion fails
    }

}

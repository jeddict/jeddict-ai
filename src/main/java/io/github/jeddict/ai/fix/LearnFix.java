/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.fix;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.Action;
import io.github.jeddict.ai.AssistantTopComponent;
import io.github.jeddict.ai.JeddictChatModel;
import io.github.jeddict.ai.util.StringUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import static io.github.jeddict.ai.util.UIUtil.askQueryAboutClass;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class LearnFix extends JavaFix {

    private final TreePath treePath;
    private final Action action;

    public LearnFix(TreePathHandle tpHandle, Action action, TreePath treePath) {
        super(tpHandle);
        this.treePath = treePath;
        this.action = action;
    }

    @Override
    protected String getText() {
        if (action == Action.LEARN) {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_LEARN",
                    StringUtil.convertToCapitalized(treePath.getLeaf().getKind().toString()));
        } else if (action == Action.QUERY) {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_QUERY",
                    StringUtil.convertToCapitalized(treePath.getLeaf().getKind().toString()));
        }
        return null;
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        Tree leaf = tc.getPath().getLeaf();

        if (leaf.getKind() == CLASS || leaf.getKind() == INTERFACE) {
            String response = null;
            if (action == Action.QUERY) {
                String query = askQueryAboutClass();
                if (query == null) {
                    return;
                }
                response = new JeddictChatModel().generateHtmlDescriptionForClass(treePath.getCompilationUnit().toString(), null, query);

            } else {
                response = new JeddictChatModel().generateHtmlDescriptionForClass(treePath.getCompilationUnit().toString());

            }
            displayHtmlContent(copy, tc.getPath(), removeCodeBlockMarkers(response), ((ClassTree) leaf).getSimpleName().toString());
        }
    }

    private JButton prevButton, nextButton, saveButton;
    private AssistantTopComponent topComponent;
    private final List<String> responseHistory = new ArrayList<>();
    private int currentResponseIndex = -1;
    String javaCode = null;

    private void displayHtmlContent(WorkingCopy copy, TreePath tp, String htmlContent, String title) {
        SwingUtilities.invokeLater(() -> {
            try {
                File tempFile = File.createTempFile("tempHtml", ".html");
                tempFile.deleteOnExit();
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(htmlContent);
                }

                Preferences prefs = Preferences.userNodeForPackage(AssistantTopComponent.class);
                prefs.putBoolean(AssistantTopComponent.PREFERENCE_KEY, true);
                topComponent = new AssistantTopComponent(title + " AI Assistance");
                topComponent.getEditorPane().setPage(tempFile.toURI().toURL());
                topComponent.getEditorPane().addHyperlinkListener((HyperlinkEvent e) -> {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                JScrollPane scrollPane = new JScrollPane(topComponent.getEditorPane());
                topComponent.add(scrollPane, BorderLayout.CENTER);

                // Create a panel for the text field and buttons
                JPanel bottomPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);

                // Previous Button
                prevButton = new JButton("\u2190"); // Left arrow (←)
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                bottomPanel.add(prevButton, gbc);

                // Next Button
                nextButton = new JButton("\u2192");
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                bottomPanel.add(nextButton, gbc);

                // Text Field
                JTextField questionField = new JTextField();
                questionField.setPreferredSize(new Dimension(300, questionField.getPreferredSize().height)); // Set preferred size
                gbc.gridx = 2;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0; // Allow text field to expand
                bottomPanel.add(questionField, gbc);

                // Ask Button
                JButton submitButton = new JButton("Ask");
                gbc.gridx = 3;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0; // No expansion
                bottomPanel.add(submitButton, gbc);

                // Save Button
                saveButton = new JButton("Copy");
                gbc.gridx = 4;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0; // No expansion
                bottomPanel.add(saveButton, gbc);

                saveButton.addActionListener(e -> {
                    try {
                        StringSelection selection = new StringSelection(javaCode);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                JButton upButton = new JButton("\u2191"); // Up arrow (↑)
                gbc.gridx = 5;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0; // No expansion
                bottomPanel.add(upButton, gbc);

                upButton.addActionListener(e -> {
                    try {
                        File latestTempFile = File.createTempFile(title, ".html");
                        latestTempFile.deleteOnExit();
                        try (FileWriter writer = new FileWriter(latestTempFile)) {
                            writer.write(responseHistory.get(currentResponseIndex));
                        }
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(latestTempFile.toURI());
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

                // Create a common action listener method
                ActionListener submitActionListener = e -> {
                    String question = questionField.getText();
                    if (!question.isEmpty()) {
                        submitButton.setText("Loading...");
                        submitButton.setEnabled(false);
                        handleQuestion(question, topComponent.getEditorPane(), submitButton);
                    }
                };

                submitButton.addActionListener(submitActionListener);
                questionField.addActionListener(submitActionListener);

                responseHistory.add(htmlContent);
                currentResponseIndex = responseHistory.size() - 1;

                prevButton.addActionListener(e -> {
                    if (currentResponseIndex > 0) {
                        currentResponseIndex--;
                        topComponent.getEditorPane().setText(responseHistory.get(currentResponseIndex));
                        updateNavigationButtons(prevButton, nextButton);
                    }
                });
                nextButton.addActionListener(e -> {
                    if (currentResponseIndex < responseHistory.size() - 1) {
                        currentResponseIndex++;
                        topComponent.getEditorPane().setText(responseHistory.get(currentResponseIndex));
                        updateNavigationButtons(prevButton, nextButton);
                    }
                });

                updateNavigationButtons(prevButton, nextButton);

                topComponent.add(bottomPanel, BorderLayout.SOUTH);
                topComponent.open();
                topComponent.requestActive();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (topComponent != null) {
                        topComponent.close();
                    }
                }));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleQuestion(String question, JEditorPane editorPane, JButton submitButton) {
        SwingUtilities.invokeLater(() -> {
            try {
                String prevChat = responseHistory.get(responseHistory.size() - 1);
                if (action == Action.LEARN) {
                    if (responseHistory.size() - 1 == 0) {
                        prevChat = null;
                    }
                }
                String response = new JeddictChatModel().generateHtmlDescriptionForClass(treePath.getCompilationUnit().toString(), prevChat, question);
                response = removeCodeBlockMarkers(response);
                if (responseHistory.isEmpty() || !response.equals(responseHistory.get(responseHistory.size() - 1))) {
                    responseHistory.add(response);
                    currentResponseIndex = responseHistory.size() - 1;
                }

                Pattern pattern = Pattern.compile("<code[^>]*type=\"full\"[^>]*class=\"java\"[^>]*>(.*?)</code>", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    javaCode = matcher.group(1);
                } else {
                    javaCode = null;
                }
                editorPane.setText(response);
                editorPane.setCaretPosition(editorPane.getDocument().getLength()); // Scroll to the bottom
                submitButton.setText("Ask");
                submitButton.setEnabled(true);
                saveButton.setEnabled(javaCode != null);
                updateNavigationButtons(prevButton, nextButton);
            } catch (Exception e) {
                e.printStackTrace();
                submitButton.setText("Ask");
                submitButton.setEnabled(true);
            }
        });
    }

    private void updateNavigationButtons(JButton prevButton, JButton nextButton) {
        prevButton.setEnabled(currentResponseIndex > 0);
        nextButton.setEnabled(currentResponseIndex < responseHistory.size() - 1);
    }

}

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
import io.github.jeddict.ai.JeddictChatModel;
import io.github.jeddict.ai.util.StringUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
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
import org.openide.windows.TopComponent;

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
            String htmlContent = new JeddictChatModel().generateHtmlDescriptionForClass(treePath.getCompilationUnit().toString());
            displayHtmlContent(removeCodeBlockMarkers(htmlContent), ((ClassTree) leaf).getSimpleName().toString());
        }
    }

    private void displayHtmlContent(String htmlContent, String title) {
        SwingUtilities.invokeLater(() -> {
            try {
                File tempFile = File.createTempFile("tempHtml", ".html");
                tempFile.deleteOnExit();
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(htmlContent);
                }

                TopComponent topComponent = new TopComponent();
                topComponent.setName(title + " AI Assistance");
                topComponent.setLayout(new BorderLayout());

                // Create JEditorPane for displaying HTML content
                JEditorPane editorPane = new JEditorPane();
                editorPane.setContentType("text/html");
                editorPane.setPage(tempFile.toURI().toURL());
                editorPane.setEditable(false); // Make sure it's not editable

                // Add a HyperlinkListener to handle link clicks if needed
                editorPane.addHyperlinkListener((HyperlinkEvent e) -> {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                // Add the JEditorPane to a JScrollPane
                JScrollPane scrollPane = new JScrollPane(editorPane);
                topComponent.add(scrollPane, BorderLayout.CENTER);

                // Create a panel for the text field and button
                JPanel bottomPanel = new JPanel(new BorderLayout());

                // Create a text field for user input
                JTextField questionField = new JTextField();
                bottomPanel.add(questionField, BorderLayout.CENTER);

                // Create a button to submit the question
                JButton submitButton = new JButton("Ask");
                bottomPanel.add(submitButton, BorderLayout.EAST);
                ActionListener submitActionListener = e -> {
                    String question = questionField.getText();
                    if (!question.isEmpty()) {
                        submitButton.setText("Loading...");
                        submitButton.setEnabled(false);
                        handleQuestion(question, editorPane, submitButton);
                    }
                };

                submitButton.addActionListener(submitActionListener);
                questionField.addActionListener(submitActionListener);

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
                String response = new JeddictChatModel().generateHtmlDescriptionForClass(treePath.getCompilationUnit().toString(), question);

                // Append the question and response to the editor pane
//                String existingContent = editorPane.getText();
//                String newContent = existingContent + "<br><b>User:</b> " + question + "<br><b>Assistant:</b> " + response;
                editorPane.setText(removeCodeBlockMarkers(response));
                editorPane.setCaretPosition(editorPane.getDocument().getLength()); // Scroll to the bottom

                // Restore the button text and enable it
                submitButton.setText("Ask");
                submitButton.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                // Restore the button text and enable it in case of error
                submitButton.setText("Ask");
                submitButton.setEnabled(true);
            }
        });
    }

}

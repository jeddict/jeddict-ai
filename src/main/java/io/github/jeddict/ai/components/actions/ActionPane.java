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
package io.github.jeddict.ai.components.actions;

import io.github.jeddict.ai.agent.FileAction;
import static io.github.jeddict.ai.components.AssistantChat.createEditorKit;
import io.github.jeddict.ai.components.diff.DiffView;
import io.github.jeddict.ai.components.diff.FileStreamSource;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.text.EditorKit;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 * {@code ActionPane} is a Swing component that extends {@code JTabbedPane} and
 * provides a user interface for displaying file-related actions (create, delete, update)
 * within the Jeddict AI Assistant. It shows source code, diffs, and confirmation
 * prompts to the user.
 */
public class ActionPane extends JTabbedPane {

    private static final Logger LOG = Logger.getLogger(ActionPane.class.getCanonicalName());

    public final ActionPaneController ctrl;

    /**
     * Constructs a new {@code ActionPane}.
     *
     * @param project The NetBeans project associated with the action.
     * @param action The {@code FileAction} to be performed.
     */
    public ActionPane(final Project project, final FileAction action) {
        this.ctrl = new ActionPaneController(project, action);
    }

    /**
     * Creates and returns a {@code JEditorPane} displaying the content of the file
     * associated with the {@code FileAction}. This method also sets up tabs for
     * displaying diffs or confirmation prompts based on the action type.
     *
     * @return A {@code JEditorPane} displaying the file content.
     */
    public JEditorPane createPane() {
        //
        // show two tabs, one with the provided source and one with the diff
        //
        //
        // TODO: refactor and use DiffUtil?
        //
        final File file = new File(ctrl.fullActionPath);
        final FileObject fo = FileUtil.toFileObject(file);
        final String mimeType = (fo != null)
                ? fo.getMIMEType()
                : io.github.jeddict.ai.util.FileUtil.mimeType(file.getName());

        //
        // If fo is not null, the source file is there and can be compared with
        // the content provided by the AI. If fo is null, it is either create
        // or delete based on the action and we show a confirmation tab.
        //
        // If the action is update but fo is null, it means that the file has
        // has been deleted, therefore we turn the update action into a create.
        //
        final String realAction = ("update".equals(ctrl.action.action()) && (fo == null))
            ? "create" : ctrl.action.action();
        if ("update".equals(realAction)) {
            addDiffTab(fo, mimeType);
        } else {
            addConfirmationTab(realAction);
        }

        JEditorPane editorPane = new JEditorPane();
        EditorKit editorKit = createEditorKit(mimeType);
        editorPane.setEditorKit(editorKit);
        editorPane.setText(ctrl.action.content());
        editorPane.setEditable(false);
        addTab("Source", editorPane);

        return editorPane;
    }

    // --------------------------------------------------------- private methods

    /**
     * Adds a "Diff" tab to the {@code JTabbedPane} displaying the difference
     * between the original file and the modified content provided by the AI.
     *
     * @param fo The {@code FileObject} representing the original file.
     * @param mimeType The MIME type of the file content.
     */
    private void addDiffTab(final FileObject fo, final String mimeType) {
        try {
            final StreamSource left = StreamSource.createSource(
                    "Modified " + ctrl.action.path(),
                    "Modified " + ctrl.action.path(),
                    mimeType,
                    new StringReader(ctrl.action.content())
            );
            final StreamSource right = new FileStreamSource(fo);

            addTab("Diff", new DiffView(left, right));
        } catch (IOException x) {
            final String msg = "error creating a diff view for action " + ctrl.action;
            LOG.severe(msg);
            Exceptions.printStackTrace(x);
        }
    }

    /**
     * Adds a "Confirmation" tab to the {@code JTabbedPane} for actions that
     * involve creating or deleting files. This tab provides a button for the
     * user to confirm the action.
     *
     * @param action The type of action (e.g., "create", "delete").
     */
    private void addConfirmationTab(final String action) {
        final String projectDir = ctrl.project.getProjectDirectory().getPath();

        final JButton okButton = new JButton(action);
        okButton.addActionListener((event) -> {
            try {
                if ("create".equals(action)) {
                    LOG.finest(() -> "Creating file " + ctrl.fullActionPath);
                    ctrl.createFile();
                } else if ("delete".equals(action)) {
                    LOG.finest(() -> "Deleting file " + ctrl.fullActionPath);
                    ctrl.deleteFile();
                }
                okButton.setEnabled(false);
            } catch (IOException x) {
                //
                // TODO: show an error to the user
                //
                LOG.finest(() -> "Failed to " + action + " file " + ctrl.fullActionPath + ": " + x.getMessage());
                Exceptions.printStackTrace(x);
            }
        });

        JOptionPane confirmationPane = new JOptionPane(
                new String[]{
                    String.format(
                            "<html><b>Do you want me to %s the below file?</b></html>", action
                    ),
                    projectDir,
                    "↳ " + ctrl.fullActionPath.substring(projectDir.length() + 1),},
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION,
                null,
                new Object[]{okButton}
        );

        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tabPanel.add(confirmationPane);
        addTab("Confirmation", tabPanel);
    }
}

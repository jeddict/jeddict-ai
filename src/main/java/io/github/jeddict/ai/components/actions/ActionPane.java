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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
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
public class ActionPane extends JPanel {

    private static final Logger LOG = Logger.getLogger(ActionPane.class.getCanonicalName());

    public final ActionPaneController ctrl;

    private final JTabbedPane sourcePane;
    private DiffView diffView;

    /**
     * Constructs a new {@code ActionPane}.
     *
     * @param project The NetBeans project associated with the action.
     * @param action The {@code FileAction} to be performed.
     */
    public ActionPane(final Project project, final FileAction action) {
        this.ctrl = new ActionPaneController(project, action);
        setPreferredSize(new Dimension(600, 600));
        setBorder(BorderFactory.createLineBorder(Color.white, 5));
        setLayout(new BorderLayout());
        add(sourcePane = new JTabbedPane(), BorderLayout.CENTER);
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
        }

        addConfirmationTab(realAction);

        JEditorPane editorPane = new JEditorPane();
        EditorKit editorKit = createEditorKit(mimeType);
        editorPane.setEditorKit(editorKit);
        editorPane.setText(ctrl.action.content());
        editorPane.setEditable(false);
        sourcePane.addTab("Source", editorPane);

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
            final FileStreamSource right = new FileStreamSource(fo);

            sourcePane.addTab("Diff", diffView = new DiffView(left, right));
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
        add(new ConfirmationPane(action), BorderLayout.NORTH);
    }

    // ---
    private class ConfirmationPane extends JOptionPane {

        private static final Insets INSETS = new Insets(10, 10, 10, 10);

        public ConfirmationPane(final String action) {
            super(null, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_OPTION);

            final String projectDir = ctrl.project.getProjectDirectory().getPath();

            //
            // When the confirmation button is pressed we should execute the
            // action; however in case of update or create the user has reviewed
            // the code in the diff viewer. Changes may be different from the
            // the ones provided by the AI due to such interaction, and can be
            // directly saved in the UI. Therefore in such cases there is
            // nothing more to do from a code perspective. The controller is
            // still called to make it aware of the interaction.
            //
            final JButton okButton = new JButton(action);
            okButton.addActionListener((event) -> {
                LOG.finest(() -> "Executing " + action + " on " + ctrl.fullActionPath);
                try {
                    if ("create".equals(action) || "update".equals(action)) {
                        diffView.saveBase();
                    }
                    ctrl.executeAction();
                    okButton.setEnabled(false);
                } catch (IOException x) {
                    //
                    // TODO: show an error to the user
                    //
                    LOG.finest(() -> "Failed to " + action + " file " + ctrl.fullActionPath + ": " + x.getMessage());
                    Exceptions.printStackTrace(x);
                }
            });

            setBorder(new EtchedBorder());

            //
            // TODO: The message may be different for different actions; it will
            // be dynamic, but for now, we make address only the delete and
            // update/create cases specifically
            //
            final String msg = ("delete".equals(action))
                             ? "<html><b>Do you want me to delete the below file?</b></html>"
                             : "<html><b>Review the below file and press " + action + " when ready"
                             ;
            setMessage(
                new String[]{
                    msg,
                    projectDir,
                    "↳ " + ctrl.fullActionPath.substring(projectDir.length() + 1)
                }
            );
            setOptions(new Object[] { okButton });

        }

        @Override
        public Insets getInsets() {
            return INSETS;
        }
    }
}

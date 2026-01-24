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
package io.github.jeddict.ai.components.diff;

import io.github.jeddict.ai.components.diff.DiffPaneController.UserAction;
import static io.github.jeddict.ai.components.diff.DiffPaneController.UserAction.ACCEPT;
import static io.github.jeddict.ai.components.diff.DiffPaneController.UserAction.REJECT;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.EditorKit;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 * {@code DiffPane} is a Swing component that extends {@code JPanel} and
 * provides a user interface for displaying diffs and accepting/rejecting changes.
 */
public class DiffPane extends JPanel {

    private static final Logger LOG = Logger.getLogger(DiffPane.class.getCanonicalName());

    public final DiffPaneController ctrl;

    private final JTabbedPane sourcePane;
    private DiffView diffView;
    private JEditorPane sourceView;
    private JButton btnAccept, btnReject;

    /**
     * Constructs a new {@code DiffPane}.
     *
     * @param project The NetBeans project associated with the action.
     * @param path The path of the file.
     * @param content The proposed content.
     */
    public DiffPane(final Project project, final String path, final String content) {
        this.ctrl = new DiffPaneController(project, path, content);

        btnAccept = new JButton("Accept");
        btnReject = new JButton("Reject");

        setPreferredSize(new Dimension(600, 600));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(92, 159, 194), 3),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        setLayout(new BorderLayout());
        add(sourcePane = new JTabbedPane(), BorderLayout.CENTER);
        sourcePane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(194, 194, 194)));

        //
        // Accept/Reject button panel
        //
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnAccept.addActionListener((ActionEvent event) -> {
            //
            // Disable the interface to prevent additional interactions
            //
            setEnabled(false);;

            //
            // Save the content (if diffView is null, there is no diff and a new
            // file is created saving the proposed wource
            //
            LOG.finest(() -> "changes accepted, saving %s".formatted(new File(project.getProjectDirectory().getPath(), path).toString()));

            //
            // update the source text with the content of the source editor so
            // that in the case of a new file, save() will use the latest text
            //
            ctrl.content = sourceView.getText();

            ctrl.save();

            //
            // Call tool's callback
            ctrl.onDone.accept(ACCEPT);
        });

        btnReject.addActionListener((ActionEvent event) -> {
            //
            // Disable the interface to prevent additional interactions
            //
            setEnabled(false);

            LOG.finest("changes rejected");
            ctrl.onDone.accept(REJECT);
        });

        //
        // To make the OK button look like the default button of a standard
        // JOptiopnPane...
        //
        btnAccept.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                JRootPane root = SwingUtilities.getRootPane(btnAccept);
                if (root != null) {
                    root.setDefaultButton(btnAccept);
                }
            }
            @Override public void ancestorRemoved(AncestorEvent event) {}
            @Override public void ancestorMoved(AncestorEvent event) {}
        });

        buttonPanel.add(btnAccept);
        buttonPanel.add(btnReject);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 10));

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates and returns a {@code JEditorPane} displaying the content of the file.
     */
    public void createPane() {
        //
        // show two tabs, one with the provided source and one with the diff
        //
        final File file = new File(ctrl.fullPath);
        final FileObject fo = FileUtil.toFileObject(file);
        final String mimeType = (fo != null)
                ? fo.getMIMEType()
                : io.github.jeddict.ai.util.FileUtil.mimeType(file.getName());

        //
        // If fo is not null, the source file is there and can be compared with
        // the content provided by the AI. Otherwise, only the new content will
        // be displayed.
        //

        // diff tab
        if (fo != null) {
            LOG.finest("adding the diff tab");
            addDiffTab(fo, mimeType);
            //
            // find should not return null, it throws a DataObjectNotFoundException
            // if the object is not found
            //
            try {
                DataObject.find(ctrl.fileObject).addPropertyChangeListener(diffView);
            } catch (DataObjectNotFoundException x) {
                // nothign to do, the file should be there, unless deleted in
                // the meantime
            }
        }

        // suggested content tab
        LOG.finest("adding the content tab");
        addSourceTab(ctrl.content, mimeType);
    }

    public void onDone(final Consumer<UserAction> action) {
        this.ctrl.onDone = action;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        btnAccept.setEnabled(enabled);
        btnReject.setEnabled(enabled);
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
                    "Modified " + ctrl.path,
                    "Modified " + ctrl.path,
                    mimeType,
                    new StringReader(ctrl.content)
            );
            final FileStreamSource right = new FileStreamSource(fo);

            sourcePane.addTab("Diff", diffView = new DiffView(left, right));
        } catch (IOException x) {
            final String msg = "error creating a diff view for " + ctrl.path;
            LOG.severe(msg);
            Exceptions.printStackTrace(x);
        }
    }

    private void addSourceTab(final String content, final String mimeType) {
        sourceView = new JEditorPane();
        EditorKit editorKit = createEditorKit(mimeType);
        sourceView.setEditorKit(editorKit);
        sourceView.setText(ctrl.content);
        sourceView.setEditable(true);
        sourcePane.addTab("Source", new JScrollPane(sourceView));
    }
}

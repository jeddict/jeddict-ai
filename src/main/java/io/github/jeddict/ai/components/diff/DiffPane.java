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

import io.github.jeddict.ai.components.ToolInvocationPane;
import io.github.jeddict.ai.components.diff.DiffPaneController.UserAction;
import static io.github.jeddict.ai.components.diff.DiffPaneController.UserAction.ACCEPT;
import static io.github.jeddict.ai.components.diff.DiffPaneController.UserAction.REJECT;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import io.github.jeddict.ai.util.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.text.EditorKit;
import org.netbeans.api.project.Project;
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
    private final JButton btnAccept, btnReject;

    /**
     * Constructs a new {@code DiffPane}.
     *
     * @param project The NetBeans project associated with the action.
     * @param path The path of the file.
     * @param content The modified content.
     */
    public DiffPane(final Project project, final String path, final String content) {
        this.ctrl = new DiffPaneController(project, path, content);

        btnAccept = new JButton("Accept");
        btnReject = new JButton("Reject");

        setPreferredSize(new Dimension(600, 600));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtil.COLOR_JEDDICT_ACCENT1)
        ));
        setBackground(UIUtil.COLOR_JEDDICT_MAIN_BACKGROUND);
        setLayout(new BorderLayout());
        ;
        
        //
        // Invocation Pane
        //
        add(
            new ToolInvocationPane("interactiveFileEditor", Map.of("path", path)), 
            BorderLayout.NORTH
        );
        
        //
        // Diff UI
        //
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
            getParent().remove(this);
            
            //
            // Save the modified (if diffView is null, there is no diff and a new
            // file is created saving the proposed wource
            //
            LOG.finest(() -> "changes accepted, saving %s".formatted(new File(project.getProjectDirectory().getPath(), path).toString()));

            ctrl.save(modifiedContent());
            
            //
            // Call tool's callback
            //
            if (ctrl.onDone != null) {
                ctrl.onDone.accept(ACCEPT);
            }
        });

        btnReject.addActionListener((ActionEvent event) -> {
            //
            // Disable the interface to prevent additional interactions
            //
            getParent().remove(this);

            LOG.finest("changes rejected");
            
            //
            // Call tool's callback
            //
            if (ctrl.onDone != null) {
                ctrl.onDone.accept(REJECT);
            }
        });

        //
        // To make the OK button look like the default button of a standard
        // JOptiopnPane...
        //
        UIUtil.makeDefaultButton(btnAccept);

        buttonPanel.add(btnAccept);
        buttonPanel.add(btnReject);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 10));

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates and returns a {@code JEditorPane} displaying the modified of the file.
     */
    public void createPane() {
        //
        // show two tabs, one with the provided source and one with the diff
        //
        final Path path = ctrl.fullPath();
        final String mimeType = (ctrl.original != null)
                ? ctrl.original.getMIMEType()
                : io.github.jeddict.ai.util.FileUtil.mimeType(path.getFileName().toString());
        
        LOG.finest(() -> "create pane for %s with mime type %s".formatted(ctrl.original.getPath(), mimeType));
        //
        // If fo is not null, the source file is there and can be compared with
        // the modified provided by the AI. Otherwise, only the new modified will
        // be displayed.
        //

        // diff tab
        if (!ctrl.isNewFile) {
            LOG.finest("adding the diff tab");
            addDiffTab(mimeType);
            //
            // find should not return null, it throws a DataObjectNotFoundException
            // if the object is not found
            //
            try {
                DataObject.find(ctrl.original).addPropertyChangeListener(diffView);
            } catch (DataObjectNotFoundException x) {
                // nothign to do, the file should be there, unless deleted in
                // the meantime
            }
        }

        // suggested modified tab
        LOG.finest("adding the content tab");
        addSourceTab(mimeType);
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
between the modified file and the modified modified provided by the AI.
     *
     * @param fo The {@code FileObject} representing the modified file.
     * @param mimeType The MIME type of the file modified.
     */
    private void addDiffTab(final String mimeType) {
        try {
            sourcePane.addTab("Diff", diffView = new DiffView(
                new FileStreamSource(ctrl.original, "Original " + ctrl.original.getNameExt()), // left
                new FileStreamSource(ctrl.modified, "Modified " + ctrl.modified.getNameExt())  // right
            ));
        } catch (IOException x) {
            LOG.severe(() -> "error creating a diff view for %s: %s".formatted(ctrl.path, String.valueOf(x)));
            Exceptions.printStackTrace(x);
        }
    }

    private void addSourceTab(final String mimeType) {
        sourceView = new JEditorPane();
        EditorKit editorKit = createEditorKit(mimeType);
        sourceView.setEditorKit(editorKit);
        try {
            sourceView.setText(ctrl.modified.asText());
        } catch (IOException x) {
            sourceView.setText("Ups! " + x);
            Exceptions.printStackTrace(x);
        }
        sourceView.setEditable(true);
        sourcePane.addTab("Source", new JScrollPane(sourceView));
    }
    
    /**
     * This method gets the content of the modified source. Since this is provided
     * as new memory FileObject there is not a SaveCookie nor the content is
     * kept in sync with the file (or at least I could not fix a way to do it).
     * 
     * The method finds the second JEditorPane in the hierarchy and returns its text.
     * 
     * @return the modified content in the diff pane
     */
    private String modifiedContent() {
        if (ctrl.isNewFile) {
            return sourceView.getText();
        }
        
        List<JEditorPane> editors = UIUtil.find(this, JEditorPane.class);
        
        //
        // If there is only one editor, the file is new, otherwise the first one
        // contains the modified content, the second one the modified content
        //
        if (editors.size() < 2) {
            final String msg = "ups... unexpected number of editors in DiffPane: " + editors;
            LOG.severe(msg);
            throw new IllegalStateException(msg);
        }
        
        return editors.get(1).getText();
    }
}

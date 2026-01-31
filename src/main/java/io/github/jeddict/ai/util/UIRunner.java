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

package io.github.jeddict.ai.util;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.github.jeddict.ai.components.ToolExecutionConfirmationPane;
import io.github.jeddict.ai.components.ToolExecutionPane;
import io.github.jeddict.ai.components.ToolInvocationPane;
import io.github.jeddict.ai.components.diff.DiffPane;
import static java.awt.BorderLayout.SOUTH;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;

/**
 *
 */
public class UIRunner {

    private ToolExecutionConfirmationPane confirmationPane;
    private ToolInvocationPane invocationPane;
    private ToolExecutionPane executionPane;
    private DiffPane diffPane;
    private JPopupMenu contextMenu;
    
    private final Logger LOG = Logger.getLogger(UIRunner.class.getName());

    public static void main(final String[] args) {
        new UIRunner();
    }

    public UIRunner() {
        JFrame frame = new JFrame("Jeddict UI Testing Utility");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 800);

        // Create a menu bar
        JMenuBar menuBar = new JMenuBar();

        // Create a menu
        JMenu menu = new JMenu("Menu");

        // Create menu items
        JMenuItem toolsExecutionUIItem = new JMenuItem("Tools execution UI");
        JMenuItem diffToolItem = new JMenuItem("Diff tool");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Add action listeners to menu items
        toolsExecutionUIItem.addActionListener(e -> {
            // Action for Tools execution UI
            JOptionPane.showMessageDialog(frame, "Tools execution UI selected");
        });

        diffToolItem.addActionListener(e -> {
            // Action for Diff tool
            showDiffPane(frame);
        });

        exitItem.addActionListener(e -> {
            frame.dispose();
        });

        // Add menu items to the menu
        menu.add(toolsExecutionUIItem);
        menu.add(diffToolItem);
        menu.add(exitItem);

        // Add the menu to the menu bar
        menuBar.add(menu);

        // Set the menu bar for the frame
        frame.setJMenuBar(menuBar);

        // Create context menu (right-click menu) reusing the same menu items
        contextMenu = new JPopupMenu();
        contextMenu.add(toolsExecutionUIItem);
        contextMenu.add(diffToolItem);
        contextMenu.add(exitItem);

        final Container content = frame.getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        // Add mouse listener for context menu
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showContextMenu(e, contextMenu);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                showContextMenu(e, contextMenu);
            }
        });

        final ToolExecutionRequest execution = ToolExecutionRequest.builder()
            .name("helloTool").arguments("{\"argument1\":\"value1\",\"argument2\":\"value2\"}").build();
        content.add(confirmationPane = new ToolExecutionConfirmationPane());
        content.add(invocationPane = new ToolInvocationPane());
        content.add(executionPane = new ToolExecutionPane(execution, "This is the result\n1\n2\n3\n4\n5"));
        content.setBackground(Color.white);

        frame.setVisible(true);
        
        final JPanel controls = new JPanel();
        
        confirmationPane.setBackground(Color.white);

        confirmationPane.showMessage(execution);
        
        confirmationPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, (e) -> {
            JOptionPane.showConfirmDialog(content, "Hello");
        });
        
        content.add(controls, SOUTH);
    }

    private void showDiffPane(JFrame frame) {
        try {
            Project project = selectProject();
            if (project == null) {
                JOptionPane.showMessageDialog(frame, "Could not determine project for SayHello.java", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String relativePath = "src/test/java/io/github/jeddict/ai/test/SayHello.java";
            // Convert to FileObject
            FileObject fileObject = project.getProjectDirectory().getFileObject(relativePath);
            if (fileObject == null) {
                JOptionPane.showMessageDialog(frame, "Could not convert SayHello.java to FileObject", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            final String content = "new line at the top\n" + fileObject.asText() + "\nnew line at the bottom";

            // Remove existing components
            frame.getContentPane().removeAll();

            // Add DiffPane to the frame with the project, path, and content
            diffPane = new DiffPane(project, relativePath, content);
            diffPane.createPane();
            frame.getContentPane().add(diffPane);

        } catch (IOException ex) {
            Logger.getLogger(UIRunner.class.getName()).log(Level.SEVERE, "Error reading SayHello.java", ex);
            JOptionPane.showMessageDialog(frame, "Error reading SayHello.java: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception ex) {
            Logger.getLogger(UIRunner.class.getName()).log(Level.SEVERE, "Error creating DiffPane", ex);
            JOptionPane.showMessageDialog(frame, "Error creating DiffPane: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Refresh the frame
        frame.revalidate();
        frame.repaint();
    }
    
    private void showContextMenu(MouseEvent e, JPopupMenu contextMenu) {
        if (e.isPopupTrigger()) {
            contextMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private Project selectProject() {
        final Project[] openProjects = org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        if (openProjects.length == 1) {
            return openProjects[0];
        } else if (openProjects.length > 1) {
            JComboBox<Project> projectComboBox = new JComboBox<>(openProjects);
            projectComboBox.setRenderer(new javax.swing.ListCellRenderer<>() {
                private final javax.swing.DefaultListCellRenderer defaultRenderer = new javax.swing.DefaultListCellRenderer();

                @Override
                public java.awt.Component getListCellRendererComponent(javax.swing.JList<? extends Project> list, Project value, int index, boolean isSelected, boolean cellHasFocus) {
                    String displayName = (value == null) ? "" : ProjectUtils.getInformation(value).getDisplayName();
                    return defaultRenderer.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
                }
            });
            NotifyDescriptor descriptor = new NotifyDescriptor(
                projectComboBox,
                "Select Project for AI Agent Mode",
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.QUESTION_MESSAGE,
                null,
                NotifyDescriptor.OK_OPTION
            );
            Object dialogResult = DialogDisplayer.getDefault().notify(descriptor);
            if (NotifyDescriptor.OK_OPTION.equals(dialogResult)) {
                Project selectedProject = (Project) projectComboBox.getSelectedItem();
                if (selectedProject != null) {
                    DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(
                            "Connected chat to project: " + ProjectUtils.getInformation(selectedProject).getDisplayName(),
                            NotifyDescriptor.INFORMATION_MESSAGE
                        )
                    );

                    return selectedProject;
                }
            }
        } else {
            NotifyDescriptor.Message msg = new NotifyDescriptor.Message(
                    "Unable to select a project",
                    NotifyDescriptor.WARNING_MESSAGE
            );
            DialogDisplayer.getDefault().notify(msg);
        }

        return null;
    }
}
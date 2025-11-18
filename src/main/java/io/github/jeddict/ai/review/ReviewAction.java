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
package io.github.jeddict.ai.review;

import io.github.jeddict.ai.actions.BaseGitAction;
import io.github.jeddict.ai.actions.BaseProjectContextAction;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.hints.AssistantChatManager;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

/**
 * An action that performs a code review on the git diff of the current project using AI.
 * This action is available in the project's context menu and is only enabled
 * for Git projects.
 */
@ActionID(
        category = "Project",
        id = "io.github.jeddict.ai.review.CTL_ReviewAction")
@ActionRegistration(
        displayName = "#CTL_ReviewAction", lazy = false, asynchronous = true)
@ActionReferences({
    @ActionReference(path = "Projects/Actions", position = 100),})
@Messages({"CTL_ReviewAction=AI Git Diff Code Review"})
public final class ReviewAction extends BaseGitAction {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Action createContextAction(boolean enable, Project project) {
        return new ContextAction(enable, project);
    }

    /**
     * The context-aware action that performs the code review.
     */
    private static final class ContextAction extends BaseProjectContextAction {

        /**
         * Constructs a new ContextAction.
         *
         * @param enable true to enable the action, false to disable it.
         * @param project the project to which the action belongs.
         */
        private ContextAction(boolean enable, Project project) {
            super(Bundle.CTL_ReviewAction(), project, enable);
        }

        /**
         * Asks the user for code review input, gathers the git diff, and then
         * uses the AI assistant to perform a code review.
         *
         * @param evt the action event.
         */
        @Override
        public void actionPerformed(ActionEvent evt) {
            CodeReviewInput input = ReviewAction.askForCodeReview(project);
            if (input == null) {
                return;
            }

            StringBuilder gitdiff = new StringBuilder();
            String gitCommand = input.gitCommand;
            java.util.List<String> selectedFiles = input.selectedFiles;

            File projectDir = FileUtil.toFile(project.getProjectDirectory());

            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                for (String file : selectedFiles) {
                    // Build git diff command for this single file
                    String gitDiffCommandForFile = gitCommand + " --unified=0 -- " + file;

                    gitdiff.append("\n\ngit ").append(gitDiffCommandForFile).append("\n\n");
                    String diffOutput = runGitCommand(gitDiffCommandForFile, project);
                    gitdiff.append(diffOutput).append("\n");

                    gitdiff.append("---- File: ").append(file).append(" ----\n");
                    File actualFile = new File(projectDir, file);
                    if (actualFile.exists() && actualFile.isFile()) {
                        try {
                            String content = java.nio.file.Files.readString(actualFile.toPath());
                            gitdiff.append(content).append("\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                    gitdiff.append("\n"); // separate files by new lines
                }
            } else {
                // If no specific files selected, fallback to original behavior (diff for all)
                gitdiff.append("git ").append(gitCommand).append(" --unified=0 \n\n");
                String diffOutput = runGitCommand(gitCommand + " --unified=0", project);
                gitdiff.append(diffOutput);

                gitdiff.append("git ").append(gitCommand).append(" --name-only \n\n");
                String nameOnlyOutput = runGitCommand(gitCommand + " --name-only", project);
                gitdiff.append(nameOnlyOutput);
            }

            /*
            String granularityInstruction = switch (input.selectedGranularity.toLowerCase()) {
                case "high" ->
                    """
        Be exhaustive. Suggest all potential improvements, even minor ones. Include naming, formatting, comments, and any best practice observations.
    """;
                case "low" ->
                    """
        Be minimal. Only report critical issues such as bugs, performance bottlenecks, or major violations of best practices.
    """;
                default ->
                    """
        Provide a balanced review. Focus on important improvements but avoid nitpicking trivial details.
    """;
            };

            String featureContext = (input.contextMessage != null && !input.contextMessage.isBlank())
                    ? "### Feature Context:\n" + input.contextMessage.trim() + "\n"
                    : "";
            */

            String featureContext = (input.contextMessage != null)
                    ? input.contextMessage.trim()
                    : "";
            Map<String, String> params = Map.of(
                "diff", gitdiff.toString(),
                "granularity", input.selectedGranularity.toLowerCase(),
                "feature", featureContext
            );

            final AssistantChatManager learnFix =
                new AssistantChatManager(io.github.jeddict.ai.completion.Action.QUERY, project, params);
            learnFix.askQueryForCodeReview();
        }
    }

    /**
     * Runs a Git command in the project's root directory.
     *
     * @param command the command to run.
     * @param project the project in which to run the command.
     * @return the output of the command.
     */
    private static String runGitCommand(String command, Project project) {
        StringBuilder output = new StringBuilder();
        try {
            String[] args = command.split("\\s+");
            String[] commandArray = new String[args.length + 1];
            commandArray[0] = "git";
            System.arraycopy(args, 0, commandArray, 1, args.length);

            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            File projectDir = FileUtil.toFile(project.getProjectDirectory());
            processBuilder.directory(projectDir);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    /**
     * Prompts the user for code review input.
     *
     * @param project the project for which the code review is being performed.
     * @return the user's input, or null if the user cancels the dialog.
     */
    private static CodeReviewInput askForCodeReview(Project project) {
        Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);

        java.util.List<String> commitList = new java.util.ArrayList<>();
        commitList.add("Uncommitted Changes");

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "log", "--oneline");
            File projectDir = FileUtil.toFile(project.getProjectDirectory());
            pb.directory(projectDir);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    commitList.add(line);
                }
            }
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        JPanel contextPanel = new JPanel(new BorderLayout());
        JLabel contextLabel = new JLabel("Enter context or feature description for the code review (optional).");
        contextLabel.setForeground(textColor);
        contextLabel.setBackground(backgroundColor);
        contextPanel.add(contextLabel, BorderLayout.NORTH);
        JTextArea contextTextArea = new JTextArea(5, 40);
        contextTextArea.setWrapStyleWord(true);
        contextTextArea.setLineWrap(true);
        contextPanel.add(new JScrollPane(contextTextArea), BorderLayout.CENTER);

        JComboBox<String> granularityComboBox = new JComboBox<>(new String[]{
            "Balanced",
            "High",
            "Low"
        });
        granularityComboBox.setSelectedItem("Balanced");
        granularityComboBox.setForeground(textColor);
        granularityComboBox.setBackground(backgroundColor);
        JPanel granularityPanel = new JPanel(new BorderLayout());
        JLabel granularityLabel = new JLabel("How detailed should the review be?");
        granularityLabel.setForeground(textColor);
        granularityLabel.setBackground(backgroundColor);
        granularityPanel.add(granularityLabel, BorderLayout.NORTH);
        granularityPanel.add(granularityComboBox, BorderLayout.CENTER);

        JPanel commitPanel = new JPanel(new BorderLayout());
        commitPanel.setVisible(false);
        JLabel commitLabel = new JLabel("Select base commit for git diff:");
        commitLabel.setForeground(textColor);
        commitLabel.setBackground(backgroundColor);
        commitPanel.add(commitLabel, BorderLayout.NORTH);

        JComboBox<String> commitComboBox = new JComboBox<>(commitList.toArray(new String[0]));
        commitComboBox.setForeground(textColor);
        commitComboBox.setBackground(backgroundColor);
        commitPanel.add(commitComboBox, BorderLayout.CENTER);

        JPanel filesContainer = new JPanel(new BorderLayout());
        JCheckBox selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.setForeground(textColor);
        selectAllCheckBox.setBackground(backgroundColor);
        selectAllCheckBox.setEnabled(false);
        filesContainer.add(selectAllCheckBox, BorderLayout.NORTH);
        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        JScrollPane filesScrollPane = new JScrollPane(filesPanel);
        filesScrollPane.setPreferredSize(new java.awt.Dimension(400, 150));
        filesContainer.add(filesScrollPane, BorderLayout.CENTER);

        // We will create a JOptionPane to get the OK button reference and control its enabled state
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Create topPanel to hold context and granularity vertically
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(contextPanel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(granularityPanel);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(commitPanel, BorderLayout.CENTER);
        panel.add(filesContainer, BorderLayout.SOUTH);

        contextTextArea.setBackground(backgroundColor);
        contextTextArea.setForeground(textColor);
        contextTextArea.setCaretColor(textColor);
        contextPanel.setBackground(backgroundColor);
        granularityPanel.setBackground(backgroundColor);
        commitPanel.setBackground(backgroundColor);
        filesContainer.setBackground(backgroundColor);
        filesPanel.setBackground(backgroundColor);
        topPanel.setBackground(backgroundColor);
        panel.setBackground(backgroundColor);

        for (Component comp : filesPanel.getComponents()) {
            if (comp instanceof JLabel || comp instanceof JCheckBox) {
                comp.setForeground(textColor);
                comp.setBackground(backgroundColor);
            }
        }
        UIManager.put("OptionPane.background", backgroundColor);
        UIManager.put("Panel.background", backgroundColor);
        UIManager.put("OptionPane.messageForeground", textColor);

        JOptionPane optionPane = new JOptionPane(panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                AssistantChat.icon);

        String projectName = (project != null) ? project.getProjectDirectory().getName() : "Project";
        JDialog dialog = optionPane.createDialog(null, Bundle.CTL_ReviewAction() + " - " + projectName);

        Runnable updateOkButton = new Runnable() {
            @Override
            public void run() {
                JButton okButton = findButton(optionPane, "OK");
                if (okButton != null) {
                    boolean anySelected = false;
                    for (Component comp : filesPanel.getComponents()) {
                        if (comp instanceof JCheckBox) {
                            JCheckBox cb = (JCheckBox) comp;
                            if (cb.isSelected()) {
                                anySelected = true;
                                break;
                            }
                        }
                    }
                    okButton.setEnabled(anySelected);
                }
            }

            private JButton findButton(Container container, String text) {
                for (Component comp : container.getComponents()) {
                    if (comp instanceof JButton) {
                        JButton btn = (JButton) comp;
                        if (text.equals(btn.getText())) {
                            return btn;
                        }
                    } else if (comp instanceof Container) {
                        JButton btn = findButton((Container) comp, text);
                        if (btn != null) {
                            return btn;
                        }
                    }
                }
                return null;
            }
        };

        // Runnable to load files for the selected commit
        Runnable loadFiles = () -> {
            SwingUtilities.invokeLater(() -> {
                selectAllCheckBox.setEnabled(false);
                filesPanel.removeAll();
                filesPanel.add(new JLabel("Loading files..."));
                filesPanel.revalidate();
                filesPanel.repaint();
            });

            String selected = (String) commitComboBox.getSelectedItem();
            java.util.Set<String> filesSet = new java.util.LinkedHashSet<>();

            if ("Uncommitted Changes".equals(selected)) {
                filesSet.addAll(runGitDiffNameOnly(project, "diff --name-only HEAD"));
                filesSet.addAll(runGitDiffNameOnly(project, "diff --name-only --cached"));
                filesSet.addAll(runGitLsFiles(project, "ls-files --others --exclude-standard"));
            } else {
                String commitHash = selected.split("\\s+", 2)[0];
                String diffCmd = "diff --name-only " + commitHash + "~1 HEAD";
                filesSet.addAll(runGitDiffNameOnly(project, diffCmd));
            }

            java.util.List<String> files = new java.util.ArrayList<>(filesSet);

            SwingUtilities.invokeLater(() -> {
                filesPanel.removeAll();
                if (files.isEmpty()) {
                    filesPanel.add(new JLabel("No files found for the selected commit."));
                    selectAllCheckBox.setEnabled(false);
                } else {
                    selectAllCheckBox.setEnabled(true);

                    for (String file : files) {
                        JCheckBox cb = new JCheckBox(file, true);
                        cb.setForeground(textColor);
                        cb.setBackground(backgroundColor);
                        filesPanel.add(cb);
                    }
                    selectAllCheckBox.setSelected(true);

                    // Add listener for selectAll checkbox
                    selectAllCheckBox.addActionListener(ev -> {
                        boolean selectedAll = selectAllCheckBox.isSelected();
                        for (Component comp : filesPanel.getComponents()) {
                            if (comp instanceof JCheckBox) {
                                ((JCheckBox) comp).setSelected(selectedAll);
                            }
                        }
                        updateOkButton.run();
                    });

                    // Add listeners to checkboxes to update OK button state
                    for (Component comp : filesPanel.getComponents()) {
                        if (comp instanceof JCheckBox) {
                            ((JCheckBox) comp).addActionListener(e -> {
                                // Update selectAll checkbox state
                                boolean allSelected = true;
                                for (Component c2 : filesPanel.getComponents()) {
                                    if (c2 instanceof JCheckBox) {
                                        JCheckBox cb2 = (JCheckBox) c2;
                                        if (!cb2.isSelected()) {
                                            allSelected = false;
                                            break;
                                        }
                                    }
                                }
                                selectAllCheckBox.setSelected(allSelected);
                                updateOkButton.run();
                            });
                        }
                    }
                }
                filesPanel.revalidate();
                filesPanel.repaint();
                updateOkButton.run();
            });
        };

        commitComboBox.addActionListener(e -> loadFiles.run());

        // Initial load files
        loadFiles.run();

        // Show the dialog
        dialog.setVisible(true);

        int option = JOptionPane.CLOSED_OPTION;
        Object selectedValue = optionPane.getValue();
        if (selectedValue instanceof Integer) {
            option = (Integer) selectedValue;
        }

        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        String selectedCommit = (String) commitComboBox.getSelectedItem();
        String gitCommand;

        if ("Uncommitted Changes".equals(selectedCommit)) {
            gitCommand = "diff";
        } else {
            String commitHash = selectedCommit.split("\\s+", 2)[0];
            gitCommand = "diff " + commitHash + " HEAD";
        }

        String contextMessage = contextTextArea.getText().trim();
        if (contextMessage.isEmpty()) {
            contextMessage = null;
        }

        java.util.List<String> selectedFiles = new java.util.ArrayList<>();
        for (Component comp : filesPanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comp;
                if (cb.isSelected()) {
                    selectedFiles.add(cb.getText());
                }
            }
        }

        String selectedGranularity = ((String) granularityComboBox.getSelectedItem()).toLowerCase();
        return new CodeReviewInput(gitCommand, contextMessage, selectedGranularity, selectedFiles);
    }

    /**
     * Runs a git diff command and returns a list of file names.
     *
     * @param project the project in which to run the command.
     * @param gitCommand the git command to run.
     * @return a list of file names.
     */
    private static java.util.List<String> runGitDiffNameOnly(Project project, String gitCommand) {
        java.util.List<String> files = new java.util.ArrayList<>();
        try {
            String[] args = gitCommand.split("\\s+");
            String[] commandArray = new String[args.length + 1];
            commandArray[0] = "git";
            System.arraycopy(args, 0, commandArray, 1, args.length);

            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            File projectDir = FileUtil.toFile(project.getProjectDirectory());
            processBuilder.directory(projectDir);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        files.add(line.trim());
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Runs a git ls-files command and returns a list of file names.
     *
     * @param project the project in which to run the command.
     * @param gitCommand the git command to run.
     * @return a list of file names.
     */
    private static java.util.List<String> runGitLsFiles(Project project, String gitCommand) {
        java.util.List<String> files = new java.util.ArrayList<>();
        try {
            String[] args = gitCommand.split("\\s+");
            String[] commandArray = new String[args.length + 1];
            commandArray[0] = "git";
            System.arraycopy(args, 0, commandArray, 1, args.length);

            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            File projectDir = FileUtil.toFile(project.getProjectDirectory());
            processBuilder.directory(projectDir);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        files.add(line.trim());
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * A data class to hold the input for a code review.
     */
    public static class CodeReviewInput {

        String gitCommand;
        String contextMessage;
        java.util.List<String> selectedFiles;
        String selectedGranularity;

        CodeReviewInput(String gitCommand, String contextMessage, String selectedGranularity, java.util.List<String> selectedFiles) {
            this.gitCommand = gitCommand;
            this.contextMessage = contextMessage;
            this.selectedFiles = selectedFiles;
            this.selectedGranularity = selectedGranularity;
        }
    }
}

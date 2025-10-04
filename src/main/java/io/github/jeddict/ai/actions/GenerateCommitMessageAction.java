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
package io.github.jeddict.ai.actions;

import io.github.jeddict.ai.hints.AssistantChatManager;
import io.github.jeddict.ai.util.UIUtil;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Shiwani Gupta
 */
/**
 * An action that generates a commit message for the current project using AI.
 * This action is available in the project's context menu and is only enabled
 * for Git projects.
 *
 * @author Shiwani Gupta
 */
@ActionID(
    category = "Project",
    id = "io.github.jeddict.ai.actions.GenerateCommitMessageAction"
)
@ActionRegistration(
    displayName = "#CTL_GenerateCommitMessageAction",
    lazy = false,
    iconInMenu = true,
    asynchronous = true,
    iconBase = "icons/logo16.png"
)
@ActionReferences({
    @ActionReference(path = "Projects/Actions", position = 100),}
)
@Messages(
    {"CTL_GenerateCommitMessageAction=AI Commit Message"}
)
public final class GenerateCommitMessageAction extends BaseGitAction {

    private static final Logger LOG = Logger.getLogger(GenerateCommitMessageAction.class.getPackageName());

    /**
     * {@inheritDoc}
     */
    @Override
    protected Action createContextAction(boolean enable, Project project) {
        return new GenerateCommitMessageAction.ContextAction(enable, project);
    }

    /**
     * The context-aware action that generates the commit message.
     */
    private static final class ContextAction extends BaseProjectContextAction {

        /**
         * Constructs a new ContextAction.
         *
         * @param enable true to enable the action, false to disable it.
         * @param project the project to which the action belongs.
         */
        private ContextAction(boolean enable, Project project) {
            super(Bundle.CTL_GenerateCommitMessageAction(), project, enable);
        }

        /**
         * Gathers the git diff and status, prompts the user for an initial
         * commit message, and then uses the AI assistant to generate a commit
         * message.
         *
         * @param evt the action event.
         */
        @Override
        public void actionPerformed(ActionEvent evt) {
            StringBuilder sb = new StringBuilder();
            sb.append("git diff \n\n");
            String diffOutput = runGitCommand("diff");
            sb.append(diffOutput);
            sb.append("\n\n git status \n\n");
            String statusOutput = runGitCommand("status");
            sb.append(statusOutput);
            String intitalCommitMessage = UIUtil.askForInitialCommitMessage();
            AssistantChatManager learnFix = new AssistantChatManager(io.github.jeddict.ai.completion.Action.QUERY, project);
            learnFix.askQueryForProjectCommit(project, sb.toString(), intitalCommitMessage);
        }

        /**
         * Runs a Git command in the project's root directory.
         *
         * @param command the command to run.
         * @return the output of the command.
         */
        private String runGitCommand(String command) {
            StringBuilder output = new StringBuilder();
            try {
                String[] args = command.split("\s+");
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
                // In a real application, proper logging should be used.
                e.printStackTrace();
            }
            return output.toString();
        }
    }
}

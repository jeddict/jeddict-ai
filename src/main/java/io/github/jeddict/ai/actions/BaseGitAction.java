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

import io.github.jeddict.ai.settings.PreferencesManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

/**
 * A base class for actions that are context-aware and specific to Git projects.
 * This class provides the common logic for checking if a project is a Git
 * repository and if the AI assistant is enabled, and enables or disables the
 * action accordingly.
 */
public abstract class BaseGitAction extends AbstractAction implements ContextAwareAction {
    final Logger LOG = Logger.getLogger(getClass().getPackageName());

    @Override
    public void actionPerformed(ActionEvent ev) {
        // This can remain empty as the context action will handle the logic
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        LOG.info("createContextAwareInstance " + actionContext);
        Project project = actionContext.lookup(Project.class);
        boolean enabled = project != null
                        && isGitRepository(project)
                        && PreferencesManager.getInstance().isAiAssistantActivated();
        LOG.info("createContextAwareInstance.enable " + enabled);
        Action a = createContextAction(enabled, project);
        LOG.info("a " + a + " " + a.isEnabled() + " " + a.getValue(DynamicMenuContent.HIDE_WHEN_DISABLED));
        return a;
    }

    private boolean isGitRepository(Project project) {
        File projectDir = FileUtil.toFile(project.getProjectDirectory());
        if (projectDir != null) {
            File gitDir = new File(projectDir, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        }
        return false;
    }

    /**
     * Creates a context-aware action for the given project.
     *
     * @param enable true to enable the action, false to disable it.
     * @param project the project to which the action belongs.
     * @return the created action.
     */
    protected abstract Action createContextAction(boolean enable, Project project);
}

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
import io.github.jeddict.ai.settings.PreferencesManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * An action that allows the user to ask AI questions about a package or a file.
 * This action is available in the context menu of a package or a file in the
 * Projects view.
 */
@ActionID(
    category = "Project",
    id = "io.github.jeddict.ai.actions.AskAIPackageAction"
)
@ActionRegistration(
    displayName = "#CTL_AskAIPackageAction",
        lazy = false,
        asynchronous = true,
        iconBase = "icons/logo16.png"
)
@ActionReferences({
    @ActionReference(path = "Projects/package/Actions", position = 100),
    @ActionReference(path = "Loaders/text/x-java/Actions", position=100),
    @ActionReference(path = "Loaders/folder/any/Actions", position = 300),
    @ActionReference(path = "Toolbars/Build", position = 100)})
@Messages({"CTL_AskAIPackageAction=AI Assistant"})
public final class AskAIPackageAction extends AbstractAction implements ContextAwareAction {

    /**
     * Opens the AI assistant chat window.
     *
     * @param ev the action event.
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        AssistantChatManager learnFix = new AssistantChatManager(io.github.jeddict.ai.completion.Action.QUERY);
        learnFix.openChat(null, "", null, "AI Assistant", null);
    }

    /**
     * Creates a context-aware instance of this action.
     *
     * @param actionContext the lookup context.
     * @return a new instance of the context-aware action.
     */
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        if (actionContext != null) {
            List<FileObject> selectedFileObjects = new ArrayList<>(actionContext.lookupAll(FileObject.class));
            if (selectedFileObjects.isEmpty()) {
                return new AskAIPackageAction.ContextAction(false, null);
            }
            return new AskAIPackageAction.ContextAction(
                    PreferencesManager.getInstance().isAiAssistantActivated(),
                    selectedFileObjects
            );
        }
        return new AskAIPackageAction.ContextAction(false, null);
    }

    /**
     * The context-aware action that opens the AI chat window for the selected
     * package or file.
     */
    private static final class ContextAction extends BaseContextAction {

        private final List<FileObject> selectedFileObjects;

        /**
         * Constructs a new ContextAction.
         *
         * @param enable true to enable the action, false to disable it.
         * @param selectedPackages the list of selected packages or files.
         */
        private ContextAction(boolean enable, List<FileObject> selectedPackages) {
            super(Bundle.CTL_AskAIPackageAction(), enable);
            this.selectedFileObjects = selectedPackages;
        }

        /**
         * Opens the AI chat window for the selected package or file.
         *
         * @param evt the action event.
         */
        @Override
        public void actionPerformed(ActionEvent evt) {
            AssistantChatManager learnFix = new AssistantChatManager(io.github.jeddict.ai.completion.Action.QUERY, selectedFileObjects);
            Iterator<? extends FileObject> selectedPackagesIterator = selectedFileObjects.iterator();
            if (selectedPackagesIterator.hasNext()) {
                Project project = FileOwnerQuery.getOwner(selectedPackagesIterator.next());
                String projectName = ProjectUtils.getInformation(project).getDisplayName();
                learnFix.openChat(null, "", null, projectName, null);
            }
        }

    }
}

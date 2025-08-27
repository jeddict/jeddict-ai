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

import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.hints.AssistantChatManager;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.*;
import org.openide.filesystems.FileObject;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * An action that adds the selected file(s) or folder(s) to the current AI
 * chat session context. This action is available in the context menu of files
 * and folders in the Projects view.
 */
@ActionID(
    category = "Project",
    id = "io.github.jeddict.ai.actions.AddToChatContextAction"
)
@ActionRegistration(
    displayName = "#CTL_AddToChatContextAction",
    lazy = false,
    iconInMenu = true,
    iconBase = "icons/logo16.png",
    asynchronous = false
)
@ActionReferences({
    @ActionReference(path = "Projects/package/Actions", position = 101),
    @ActionReference(path = "Loaders/text/x-java/Actions", position = 101),
    @ActionReference(path = "Loaders/folder/any/Actions", position = 301)
})
@Messages({"CTL_AddToChatContextAction=Add to Chat Session Context"})
public final class AddToChatContextAction extends AbstractAction implements ContextAwareAction {

    /**
     * This method is never called directly. The action is handled by the
     * context-aware instance.
     *
     * @param e the action event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // It will never be called directly—only through ContextAction
    }

    /**
     * Creates a context-aware instance of this action.
     *
     * @param context the lookup context.
     * @return a new instance of the context-aware action.
     */
    @Override
    public Action createContextAwareInstance(Lookup context) {
        List<FileObject> files = new ArrayList<>(context.lookupAll(FileObject.class));
        boolean en = !files.isEmpty() && isAssistantChatOpened();
        return new ContextAction(en, files);
    }

    /**
     * Returns true if the AI assistant chat is opened, false otherwise.
     *
     * @return true if the AI assistant chat is opened, false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return isAssistantChatOpened();
    }

    /**
     * Checks if the AI assistant chat window is opened and showing.
     *
     * @return true if the chat window is opened and showing, false otherwise.
     */
    private static boolean isAssistantChatOpened() {
        Set<? extends Mode> modes = WindowManager.getDefault().getModes();
        for (Mode mode : modes) {
            TopComponent[] openedTopComponents = WindowManager.getDefault().getOpenedTopComponents(mode);
            for (TopComponent tc : openedTopComponents) {
                if (tc instanceof AssistantChat) {
                    if (tc.isOpened() && tc.isShowing()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the opened AI assistant chat window.
     *
     * @return the opened chat window, or null if it is not opened.
     */
    private static TopComponent getOpenedAssistantChat() {
        Set<? extends Mode> modes = WindowManager.getDefault().getModes();
        for (Mode mode : modes) {
            TopComponent[] openedTopComponents = WindowManager.getDefault().getOpenedTopComponents(mode);
            for (TopComponent tc : openedTopComponents) {
                if (tc instanceof AssistantChat) {
                    if (tc.isOpened() && tc.isShowing()) {
                        return tc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The context-aware action that adds the selected files to the chat
     * session.
     */
    private static final class ContextAction extends BaseContextAction {

        private final List<FileObject> files;

        /**
         * Constructs a new ContextAction.
         *
         * @param enabled true to enable the action, false to disable it.
         * @param files the list of files to add to the chat session.
         */
        private ContextAction(boolean enabled, List<FileObject> files) {
            super(Bundle.CTL_AddToChatContextAction(), enabled);
            this.files = files;
        }

        /**
         * Adds the selected files to the chat session context.
         *
         * @param e the action event.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            TopComponent openedAssistantChat = getOpenedAssistantChat();
            if (openedAssistantChat != null) {
                Object clientProperty = openedAssistantChat.getClientProperty(AssistantChatManager.ASSISTANT_CHAT_MANAGER_KEY);
                if (clientProperty instanceof WeakReference) {
                    Object tmp = ((WeakReference) clientProperty).get();
                    if (tmp instanceof AssistantChatManager) {
                        ((AssistantChatManager) tmp).addToSessionContext(files);
                    }
                }
            }
        }

        /**
         * Returns true if the AI assistant chat is opened, false otherwise.
         *
         * @return true if the AI assistant chat is opened, false otherwise.
         */
        @Override
        public boolean isEnabled() {
            return isAssistantChatOpened();
        }

    }
}

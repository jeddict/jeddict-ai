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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * An action that opens the Chat from the build button bar
 */
@ActionID(
    category = "Project",
    id = "io.github.jeddict.ai.actions.ToolbarAction"
)
@ActionRegistration(
    displayName = "#CTL_ToolbarAction",
        lazy = true,
        asynchronous = true,
        iconBase = "icons/logo.png"
)
@ActionReferences({
    @ActionReference(path = "Toolbars/Build", position = 100)}
)
@Messages(
    {"CTL_ToolbarAction=Jeddict AI Assistant"}
)
public final class ToolbarAction extends AbstractAction {

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
}

/*
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

import io.github.jeddict.ai.util.UIRunner;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 * An action that opens the Chat from the build button bar
 */
@ActionID(
    category = "Project",
    id = "io.github.jeddict.ai.actions.DevToolbarAction"
)
@ActionRegistration(
    displayName = "#CTL_JeddictDevToolsAction",
    lazy = false,
    asynchronous = true,
    iconBase = "icons/logo16.png"
)
@ActionReferences({
    @ActionReference(path = "Projects/Actions", position = 100)}
)
@Messages(
    {"CTL_JeddictDevToolsAction=Jeddict Dev"}
)
public final class DevMenuAction extends AbstractAction implements ContextAwareAction {
    //
    // TODO: use a setting instead
    //
    private static final boolean ENABLED = false; // Change this condition as needed
    
    /**
     * This method is never called directly. The action is handled by the
     * context-aware instance.
     *
     * @param ev the action event.
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    /**
     * Creates a context-aware instance of this action.
     *
     * @param actionContext the lookup context.
     * @return a new instance of the context-aware action.
     */
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DevMenuAction.ContextAction(ENABLED);
    }
    
    private static final class ContextAction 
        extends BaseProjectContextAction
        implements Presenter.Menu {
        
        private JMenuItem menuItem = null;
        
        private ContextAction(final boolean isEnabled) {
            super(Bundle.CTL_JeddictDevToolsAction(), null, isEnabled);
        }

        /**
         * Opens the AI assistant chat window.
         *
         * @param ev the action event.
         */
        @Override
        public void actionPerformed(ActionEvent ev) {
            new UIRunner();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            if (menuItem == null) {
                menuItem = new JMenuItem(this);
            }
            menuItem.setVisible(menuItem.isEnabled());
            return menuItem;
        }
    }

}
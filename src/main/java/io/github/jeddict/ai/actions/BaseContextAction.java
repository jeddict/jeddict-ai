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

import javax.swing.AbstractAction;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.JMenuItem;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.Presenter;

/**
 * A base class for context-aware actions that are presented as popup menu items.
 * This class provides common functionality for setting the action's name, icon,
 * and enabled state.In particular getPopupPresenter() is the key to get the icon
 * showed next to the action name (see https://netbeans.apache.org/wiki/main/netbeansdevelopperfaq/DevFaqAddIconToContextMenu).
 */
public abstract class BaseContextAction extends AbstractAction implements Presenter.Popup {

    @StaticResource
    private static final String ICON = "icons/logo16.png";

    /**
     * Constructs a new BaseContextAction.
     *
     * @param name the name of the action.
     * @param enable true to enable the action, false to disable it.
     */
    public BaseContextAction(final String name, final boolean enable) {
        super(name);
        setEnabled(enable);

        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(ICON, false));
        putValue("iconBase", ICON);
    }

    /**
     * Returns the popup presenter for this action.
     *
     * @return the JMenuItem that represents this action in a popup menu.
     */
    @Override
    public JMenuItem getPopupPresenter() {
        final JMenuItem item = new JMenuItem(this);
        item.setVisible(enabled);
        return item;
    }
}

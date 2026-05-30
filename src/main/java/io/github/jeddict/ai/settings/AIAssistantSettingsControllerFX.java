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
package io.github.jeddict.ai.settings;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.TopLevelRegistration(
        id = "JeddictAIAssistantFX",
        categoryName = "#OptionsCategory_Name_JeddictAIAssistantFX",
        iconBase = "icons/logo32.png",
        keywords = "#OptionsCategory_Keywords_JeddictAIAssistantFX",
        keywordsCategory = "JeddictAIAssistant"
)
public final class AIAssistantSettingsControllerFX extends OptionsPanelController {

    private final JeddictPreferences preferences = new JeddictPreferences();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final Logger LOG = Logger.getLogger(AIAssistantSettingsControllerFX.class.getName());

    @Override
    public void update() {
        LOG.info("option panel controller - update");
        preferences.refresh();
    }

    @Override
    public void applyChanges() {
        SwingUtilities.invokeLater(() -> {
            preferences.save();
        });
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        //
        // if preferences is null, the UI is not yet initialized
        //
        return (preferences.preferences != null) && preferences.preferences.isContainingChanges();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return preferences.getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}

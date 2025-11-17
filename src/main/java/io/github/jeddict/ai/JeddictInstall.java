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
package io.github.jeddict.ai;

import io.github.jeddict.ai.settings.FilePreferences;
import static io.github.jeddict.ai.settings.PreferencesManager.JEDDICT_CONFIG;
import static io.github.jeddict.ai.settings.ReportManager.DAILY_INPUT_TOKEN_STATS_KEY;
import static io.github.jeddict.ai.settings.ReportManager.DAILY_OUTPUT_TOKEN_STATS_KEY;
import static io.github.jeddict.ai.settings.ReportManager.JEDDICT_STATS;
import io.github.jeddict.ai.util.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.openide.modules.ModuleInstall;

/**
 * Jeddict Module installation class. This shall be set in manifest.mf with key
 * OpenIDE-Module-Install. <code>restore()</code> is called any time the module
 * is reloaded, either due to restarting NB or reloading the module.
 */
public class JeddictInstall extends ModuleInstall {

    private final Logger LOG = Logger.getLogger(getClass().getCanonicalName());

    @Override
    public void restored() {
        LOG.info(() -> "Restoring Jeddict AI Assistant");

        final Path configPath = FileUtil.getConfigPath();
        final Path configFile = configPath.resolve(JEDDICT_CONFIG);

        //
        // Old versions of Jeddict used to store the configuration in $HOME/jeddict.json,
        // therefore the new code shall migrate the old settings is found.
        //
        try {
            final Path oldConfigFile = Paths.get(System.getProperty("user.home")).resolve("jeddict.json");

            if (Files.exists(oldConfigFile) && !Files.exists(configFile)) {
                final Path statsFile = configPath.resolve(JEDDICT_STATS);
                LOG.info(() -> String.format(
                    "Migrating old config file from %s to %s and %s",
                    oldConfigFile, configFile, statsFile
                ));
                Files.createDirectories(configPath);
                Files.move(oldConfigFile, configFile);

                //
                // the old version of the settings contained also stats; the new
                // version does not and stats go in a separate file
                //
                final FilePreferences prefs = new FilePreferences(configFile);
                final FilePreferences stats = new FilePreferences(statsFile);

                JSONObject o = prefs.getChild(DAILY_INPUT_TOKEN_STATS_KEY);
                if (!o.isEmpty()) {
                    stats.setChild(DAILY_INPUT_TOKEN_STATS_KEY, o);
                }
                o = prefs.getChild(DAILY_OUTPUT_TOKEN_STATS_KEY);
                if (!o.isEmpty()) {
                    stats.setChild(DAILY_OUTPUT_TOKEN_STATS_KEY, o);
                }
                prefs.remove(DAILY_INPUT_TOKEN_STATS_KEY);
                prefs.remove(DAILY_OUTPUT_TOKEN_STATS_KEY);

                LOG.info("Successfully migrated old config file.");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to migrate old config file", e);
        }
    }
}

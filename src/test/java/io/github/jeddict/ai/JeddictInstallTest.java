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

import com.github.stefanbirkner.systemlambda.SystemLambda;
import io.github.jeddict.ai.settings.FilePreferences;
import static io.github.jeddict.ai.settings.PreferencesManager.JEDDICT_CONFIG;
import static io.github.jeddict.ai.settings.PreferencesManagerTest.LINUX;
import static io.github.jeddict.ai.settings.PreferencesManagerTest.MACOS;
import static io.github.jeddict.ai.settings.PreferencesManagerTest.USER;
import static io.github.jeddict.ai.settings.PreferencesManagerTest.WINDOWS;
import static io.github.jeddict.ai.settings.ReportManager.JEDDICT_STATS;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.util.FileUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class JeddictInstallTest extends TestBase {

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        Files.createDirectory(HOME.resolve(USER));
    }

    @Test
    public void migrates_old_config_file_from_home_directory_linux() throws Exception {
        final Path USERHOME = HOME.resolve(USER);
        // Simulate Linux for a predictable target path
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", LINUX);
            System.setProperty("user.name", USER);
            // Use the @TempDir HOME from TestBase as our user.home
            System.setProperty("user.home", USERHOME.toString());

            // 1. Setup: Create the old config file in the user's home
            Path oldConfigFile = USERHOME.resolve("jeddict.json");
            String fileContent = "{\"key\": \"value\"}";
            Files.writeString(oldConfigFile, fileContent);
            then(oldConfigFile).exists();

            // Define the expected new path
            Path newConfigFile = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);
            then(newConfigFile).doesNotExist();

            // 2. Execute the code under test
            new JeddictInstall().restored(); // This will trigger the migration

            // 3. Assert the results
            then(oldConfigFile).doesNotExist();
            then(newConfigFile).exists();
            then(Files.readString(newConfigFile)).isEqualTo(fileContent);
        });
    }

    @Test
    public void migrates_old_config_file_from_home_directory_windows_no_appdata() throws Exception {
        final Path USERHOME = HOME.resolve(USER);
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", WINDOWS);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", USERHOME.toString()); // Use @TempDir for user.home

            SystemLambda.withEnvironmentVariable("APPDATA", "")
            .execute(() -> {

                // 1. Setup: Create the old config file in the user's home
                Path oldConfigFile = USERHOME.resolve("jeddict.json");
                String fileContent = "{\"key\": \"value_win\"}";
                Files.writeString(oldConfigFile, fileContent);
                then(oldConfigFile).exists();

                // Define the expected new path (Windows fallback when APPDATA is not set)
                Path newConfigFile = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);
                then(newConfigFile).doesNotExist();

                // 2. Execute the code under test
                new JeddictInstall().restored(); // This will trigger the migration

                // 3. Assert the results
                then(oldConfigFile).doesNotExist();
                then(newConfigFile).exists();
                then(Files.readString(newConfigFile)).isEqualTo(fileContent);

            });
        });
    }

    @Test
    public void migrates_old_config_file_from_home_directory_windows_appdata() throws Exception {
        final Path USERHOME = HOME.resolve(USER);
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", WINDOWS);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", USERHOME.toString()); // Use @TempDir for user.home

            SystemLambda.withEnvironmentVariable("APPDATA", HOME.resolve("AppData").toString())
            .execute(() -> {

                // 1. Setup: Create the old config file in the user's home
                Path oldConfigFile = USERHOME.resolve("jeddict.json");
                String fileContent = "{\"key\": \"value_win\"}";
                Files.writeString(oldConfigFile, fileContent);
                then(oldConfigFile).exists();

                // Define the expected new path (Windows fallback when APPDATA is not set)
                Path newConfigFile = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);
                then(newConfigFile).doesNotExist();

                // 2. Execute the code under test
                new JeddictInstall().restored(); // This will trigger the migration

                // 3. Assert the results
                then(oldConfigFile).doesNotExist();
                then(newConfigFile).exists();
                then(Files.readString(newConfigFile)).isEqualTo(fileContent);

            });
        });
    }

    @Test
    public void migrates_old_config_file_from_home_directory_macos() throws Exception {
        final Path USERHOME = HOME.resolve(USER);
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", MACOS);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", USERHOME.toString()); // Use @TempDir for user.home

            // 1. Setup: Create the old config file in the user's home
            Path oldConfigFile = USERHOME.resolve("jeddict.json");
            String fileContent = "{\"key\": \"value_mac\"}";
            Files.writeString(oldConfigFile, fileContent);
            then(oldConfigFile).exists();

            // Define the expected new path
            Path newConfigFile = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);
            then(newConfigFile).doesNotExist();

            // 2. Execute the code under test
            new JeddictInstall().restored(); // This will trigger the migration

            // 3. Assert the results
            then(oldConfigFile).doesNotExist();
            then(newConfigFile).exists();
            then(Files.readString(newConfigFile)).isEqualTo(fileContent);
        });
    }

    @Test
    public void migrates_old_config_file_splits_config_and_stats() throws Exception {
        final Path USERHOME = HOME.resolve(USER);

        // Simulate Linux for a predictable target path
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", LINUX);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", USERHOME.toString());

            // 1. Setup: Create the old config file in the user's home
            Path oldConfigFile = USERHOME.resolve("jeddict.json");
            String fileContent = """
            {
                "dailyInputTokenStats": {
                    "20335": 81392
                },
                "key1":"value1",
                "dailyOutputTokenStats": {
                    "20355": 1206
                }
            }
            """;
            Files.writeString(oldConfigFile, fileContent);
            then(oldConfigFile).exists();

            // Define the expected new path
            Path newConfigFile = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);
            Path newStatsFile = FileUtil.getConfigPath().resolve(JEDDICT_STATS);
            then(newConfigFile).doesNotExist(); then(newStatsFile).doesNotExist();

            // 2. Execute the code under test
            new JeddictInstall().restored(); // This will trigger the migration

            // 3. Assert the results
            then(oldConfigFile).doesNotExist();
            then(newConfigFile).exists();
            then(newStatsFile).exists();

            FilePreferences newPrefs = new FilePreferences(newConfigFile);
            FilePreferences newStats = new FilePreferences(newStatsFile);

            then(newPrefs.get("key1", "")).isEqualTo("value1");
            then(newPrefs.getChild("dailyInputTokenStats").toString()).isEqualTo("{}");
            then(newPrefs.getChild("dailyOutputTokenStats").toString()).isEqualTo("{}");

            then(newStats.get("key1", "")).isEmpty();
            then(newStats.getChild("dailyInputTokenStats").toString()).isEqualTo("{\"20335\":81392}");
            then(newStats.getChild("dailyOutputTokenStats").toString()).isEqualTo("{\"20355\":1206}");
        });
    }
}

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

import com.github.stefanbirkner.systemlambda.SystemLambda;
import static io.github.jeddict.ai.settings.PreferencesManager.JEDDICT_CONFIG;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.util.FileUtil;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class PreferencesManagerTest extends TestBase {

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        // Reset the singleton instance before each test
        Field instance = PreferencesManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        Files.createDirectory(HOME.resolve(USER));
    }

    @Test
    public void constructor_without_given_path_linux() throws Exception {
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", LINUX);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", "/home/" + USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance();
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }

    @Test
    public void constructor_without_given_path_macos() throws Exception {
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", MACOS);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", "/home/" + USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance();
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }

    @Test
    public void constructor_without_given_path_windows() throws Exception {
        SystemLambda.restoreSystemProperties(() -> {
            System.setProperty("os.name", WINDOWS);
            System.setProperty("user.name", USER);
            System.setProperty("user.home", "C:\\Users\\" + USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance();
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }
}
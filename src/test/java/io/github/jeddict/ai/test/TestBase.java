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
package io.github.jeddict.ai.test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 */
public class TestBase {

    protected final Logger LOG = Logger.getAnonymousLogger();

    public final static String WINDOWS = "Windows 10";
    public final static String LINUX = "Linux";
    public final static String MACOS = "Mac OS X";
    public final static String USER = "user";

    protected String projectDir;
    protected DummyLogHandler logHandler;
    //
    // Settings are currently saved in a file in the user home (see
    // PreferencesManager and FilePreferences). To be able to manipulate them
    // without side effects, we set up a different user home
    //
    protected PreferencesManager preferences;

    @TempDir
    protected Path HOME;

    @BeforeEach
    public void beforeEach() throws Exception {
        projectDir = HOME.resolve("dummy-project").toString();

        Logger logger = Logger.getLogger("io.github.jeddict.ai");
        logger.setLevel(Level.ALL);
        logger.addHandler(logHandler = new DummyLogHandler());

        Path folder = Files.createDirectories(Paths.get(projectDir, "folder"));
        try (Writer w = new FileWriter(folder.resolve("testfile.txt").toFile())) {
            w.append("This is a test file content for real file testing.");
        }

        Files.copy(Paths.get(
        "src/test/resources/settings/jeddict.json"),
        HOME.resolve("jeddict.json"),
        StandardCopyOption.REPLACE_EXISTING
        );

        //
        // Making sure the singleton is initilazed with a testing configuration
        // file under a temporary directory
        //
        restoreSystemProperties(() -> {
            System.setProperty("user.home", HOME.toAbsolutePath().toString());

            preferences = PreferencesManager.getInstance();
        });
    }

    @AfterEach
    public void afterEach() {
        Logger.getLogger(getClass().getPackageName()).removeHandler(logHandler);
    }

    protected void thenPathsAreEqual(final Path p1, final Path p2) {
        then(p1.toUri().getPath()).isEqualTo(p2.toUri().getPath());
    }
}

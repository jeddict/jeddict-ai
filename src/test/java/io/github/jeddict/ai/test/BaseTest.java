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

import io.github.jeddict.ai.test.DummyLogHandler;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 */
public class BaseTest {
    protected String projectDir;
    protected DummyLogHandler logHandler;

    @TempDir
    protected Path HOME;

    @BeforeEach
    public void beforeEach() throws IOException {
        projectDir = HOME.resolve("dummy-project").toString();

        Logger logger = Logger.getLogger("io.github.jeddict.ai");
        logger.setLevel(Level.ALL);
        logger.addHandler(logHandler = new DummyLogHandler());

        Path folder = Files.createDirectories(Paths.get(projectDir, "folder"));
        try (Writer w = new FileWriter(folder.resolve("testfile.txt").toFile())) {
            w.append("This is a test file content for real file testing.");
        }
    }

    @AfterEach
    public void afterEach() {
        Logger.getLogger(getClass().getPackageName()).removeHandler(logHandler);
    }
}

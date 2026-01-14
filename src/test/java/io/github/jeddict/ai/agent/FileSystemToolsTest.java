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
package io.github.jeddict.ai.agent;

import dev.langchain4j.exception.ToolExecutionException;
import io.github.jeddict.ai.test.TestBase;
import static io.github.jeddict.ai.agent.AbstractTool.PROPERTY_MESSAGE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileSystemToolsTest extends TestBase {

    private final static String TESTFILE = "folder/testfile.txt";

    protected FileSystemTools tools = null;
    protected List<PropertyChangeEvent> events = null;

    @BeforeEach
    public void beforeEac() throws IOException {
        tools = new FileSystemTools(projectDir);
        events = new ArrayList();
        tools.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });
    }

    @Test
    public void searchInFile_with_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "test file";

        then(tools.searchInFile(path, pattern)).contains("Match at").contains("test file");
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_with_no_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "abc";

        then(tools.searchInFile(path, pattern)).isEqualTo("No matches found");
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String pattern = "abc";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(abs, pattern));

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + abs + "'");

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(rel, pattern));

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + rel + "'");
    }

    @Test
    public void createFile_with_and_without_existing_file() throws Exception {
        final String path = "folder/newfile.txt";
        final String content = "Sample content.";

        then(tools.createFile(path, content)).isEqualTo("File created");

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📄 Creating new file: " + path);

        events.clear();
        thenThrownBy(() -> tools.createFile(path, content))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + path + " already exists");

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📄 Creating new file: " + path);
        then(events.get(1).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(1).getNewValue()).isEqualTo("❌ " + path + " already exists");
    }

    @Test
    public void createFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String content = "Sample content.";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(abs, content));

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📄 Creating new file: " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(rel, content));

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📄 Creating new file: " + rel);
    }

    @Test
    public void deleteFile_success_and_not_found() throws Exception {
        final String path = TESTFILE;

        //
        // Relative path
        //
        final Path fileToDelete = Paths.get(projectDir, path);
        then(fileToDelete).exists(); // just to make sure...
        then(tools.deleteFile(path)).isEqualTo("File deleted");
        then(fileToDelete).doesNotExist();

        then(tools.deleteFile(path)).isEqualTo("File not found: " + path);

        //
        // TODO: logging
        //
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";

        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");

        then(tools.listFilesInDirectory(nonExistingDir)).isEqualTo("Directory not found: " + nonExistingDir);

        //
        // TODO: logging
        //
    }

    @Test
    public void readFile_success_and_failure() throws Exception {
        final String pathOK = TESTFILE;
        final Path fullPathOK = Paths.get(projectDir, pathOK).toAbsolutePath().toRealPath();
        final String expectedContent = FileUtils.readFileToString(fullPathOK.toFile(), "UTF8");

        //
        // success relative path (note we log in progress the relative path, which is user friendly)
        //
        then(tools.readFile(pathOK)).isEqualTo(expectedContent);
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + pathOK);

        //
        // success absolute path inside folder
        //
        events.clear();
        then(tools.readFile(fullPathOK.toString())).isEqualTo(expectedContent);
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + fullPathOK);

        //
        // failure (not we log absolute path to make troubleshooting easier)
        //
        final String pathKO = "nowhere.txt";
        final Path fullPathKO = Paths.get(projectDir, pathKO);
        events.clear();

        thenThrownBy( () ->
            tools.readFile(pathKO)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("failed to read file %s%s%s".formatted(projectDir, File.separator, pathKO));

        then(events).hasSize(2);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + pathKO);
        then(events.get(1).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(1).getNewValue()).isEqualTo("❌ Failed to read file " + fullPathKO);
    }

    @Test
    public void readFile_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("jeddict.json").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.readFile(abs.toString())
        );

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() ->
            tools.readFile(rel)
        );

        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + rel);
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        then(tools.createDirectory(path)).isEqualTo("Directory already exists: " + path);

        //
        // TODO: logging
        //
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = Paths.get(projectDir, path);

        Files.createDirectories(fullPath);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        then(tools.deleteDirectory(path)).isEqualTo("Directory not found: " + path);

        //
        // TODO: logging
        //
    }

    // --------------------------------------------------------- private methods

    private void thenTriedFileOutsideProjectFolder(final Runnable exec) {

        thenThrownBy(() -> exec.run())
        .isInstanceOf(ToolExecutionException.class)
        .hasMessage("trying to reach a file outside the project folder");
        then(events).anyMatch((e) -> {
            return (
                e.getPropertyName().equals(PROPERTY_MESSAGE) &&
                e.getNewValue().equals("❌ Trying to reach a file outside the project folder")
           );
        });

    }
}

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

import static io.github.jeddict.ai.agent.AbstractTool.PROPERTY_MESSAGE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.BDDAssertions;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class FileSystemToolsTest extends BaseTest {

    @AfterEach
    public void afterEach() {
    }

    @Test
    public void searchInFile_with_matches() throws Exception {
        final String path = "folder/testfile.txt";
        final String pattern = "test file";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });

        then(tools.searchInFile(path, pattern)).contains("Match at").contains("test file");
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void testSearchInFile_NoMatches() throws Exception {
        final String path = "folder/testfile.txt";
        final String pattern = "abc";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });

        then(tools.searchInFile(path, pattern)).isEqualTo("No matches found");
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void createFile_with_and_without_existing_file() throws Exception {
        final String path = "folder/newfile.txt";
        final String content = "Sample content.";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        then(tools.createFile(path, content)).isEqualTo("File created");
        then(tools.createFile(path, content)).isEqualTo("File already exists: " + path);

        //
        // TODO: logging
        //
    }

    @Test
    public void deleteFile_success_and_not_found() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String path = "folder/testfile.txt";

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

        final FileSystemTools tools = new FileSystemTools(projectDir);
        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");

        then(tools.listFilesInDirectory(nonExistingDir)).isEqualTo("Directory not found: " + nonExistingDir);

        //
        // TODO: logging
        //
    }

    @Test
    public void readFile_success_and_failure() throws Exception {
        final String pathOK = "folder/testfile.txt";
        final Path fullPathOK = Paths.get(projectDir, pathOK);
        final String expectedContent = FileUtils.readFileToString(fullPathOK.toFile(), "UTF8");

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });

        //
        // success
        //
        then(tools.readFile(pathOK)).isEqualTo(expectedContent);
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + pathOK);

        //
        // failure
        //
        final String pathKO = "nowhere.txt";
        final Path fullPathKO = Paths.get(projectDir, pathKO);
        events.clear();

        BDDAssertions.thenThrownBy( () ->
            tools.readFile(pathKO)
        );
        then(events).hasSize(2);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("📖 Reading file " + pathKO);
        then(events.get(1).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(1).getNewValue()).isEqualTo("❌ Failed to read file: " + fullPathKO);
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        final FileSystemTools tools = new FileSystemTools(projectDir);

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

        final FileSystemTools tools = new FileSystemTools(projectDir);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        then(tools.deleteDirectory(path)).isEqualTo("Directory not found: " + path);

        //
        // TODO: logging
        //
    }
}
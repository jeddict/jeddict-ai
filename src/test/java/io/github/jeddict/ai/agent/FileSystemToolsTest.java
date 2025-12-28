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

import io.github.jeddict.ai.test.TestBase;
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

public class FileSystemToolsTest extends TestBase {

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
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(events::add);

        then(tools.createFile(path, content)).isEqualTo("File created");
        then(tools.createFile(path, content)).isEqualTo("File already exists: " + path);

        // Logging assertions for progress messages
        int i = 0;
        then(events).hasSize(4);
        then(events.get(i).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(i++).getNewValue()).isEqualTo("📄 Creating new file: " + path);
        then(events.get(i).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(i++).getNewValue()).isEqualTo("✅ File created successfully: " + path);
        then(events.get(i).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(i++).getNewValue()).isEqualTo("📄 Creating new file: " + path);
        then(events.get(i).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(i++).getNewValue()).isEqualTo("⚠ File already exists: " + path);
    }

    @Test
    public void deleteFile_success_and_not_found() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String path = "folder/testfile.txt";

        final Path fileToDelete = Paths.get(projectDir, path);
        then(fileToDelete).exists(); // just to make sure...

        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(events::add);

        then(tools.deleteFile(path)).isEqualTo("File deleted");
        then(fileToDelete).doesNotExist();

        then(tools.deleteFile(path)).isEqualTo("File not found: " + path);

        // Logging assertions for progress messages
        then(events).extracting(PropertyChangeEvent::getNewValue).contains(
                "🗑 Attempting to delete file: " + path,
                "✅ File deleted successfully: " + path,
                "⚠ File not found: " + path
        );
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";

        final FileSystemTools tools = new FileSystemTools(projectDir);

        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(events::add);

        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");

        then(tools.listFilesInDirectory(nonExistingDir)).isEqualTo("Directory not found: " + nonExistingDir);

        // Logging assertions for progress messages
        then(events).extracting(PropertyChangeEvent::getNewValue).contains(
                "📂 Listing content of directory: " + existingDir,
                "❌ invalid directory: " + nonExistingDir
        );
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
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(events::add);

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        then(tools.createDirectory(path)).isEqualTo("Directory already exists: " + path);

        // Logging assertions for progress messages
        then(events).extracting(PropertyChangeEvent::getNewValue).contains(
                "📂 Creating new directory: " + path,
                "⚠ Directory already exists: " + path
        );
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = Paths.get(projectDir, path);

        Files.createDirectories(fullPath);

        final FileSystemTools tools = new FileSystemTools(projectDir);

        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(events::add);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        then(tools.deleteDirectory(path)).isEqualTo("Directory not found: " + path);

        // Logging assertions for progress messages
        then(events).extracting(PropertyChangeEvent::getNewValue).contains(
                "🗑 Attempting to delete directory: " + path,
                "✅ Directory deleted successfully: " + path,
                "🗑 Attempting to delete directory: " + path,
                "⚠ Directory not found: " + path
        );
    }

    @Test
    public void findFiles_finds_single_file() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String directory = ".";
        final String pattern = ".*testfile\\.txt";

        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(evt -> events.add(evt));

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).contains("folder/testfile.txt");
        then(events).extracting(PropertyChangeEvent::getPropertyName).contains(PROPERTY_MESSAGE);
    }

    @Test
    public void findFiles_finds_multiple_files() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);

        // Create another file
        Path extraFile = projectPath.resolve("folder/otherfile.txt");
        Files.writeString(extraFile, "content");

        final String directory = "folder";
        final String pattern = ".*\\.txt";

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).contains("folder/testfile.txt");
        then(result).contains("folder/otherfile.txt");
    }

    @Test
    public void findFiles_matches_folder_name() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);

        // Create a specific folder structure
        Path specialDir = projectPath.resolve("special_dir");
        Files.createDirectories(specialDir);
        Path specialFile = specialDir.resolve("content.data");
        Files.writeString(specialFile, "data");

        final String directory = ".";
        // Search for anything containing "special_dir" in the path
        final String pattern = ".*special_dir.*";

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).contains("special_dir/content.data");
    }

    @Test
    public void findFiles_no_matches() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String directory = ".";
        final String pattern = ".*missing\\.txt";

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).isEqualTo("");
    }

    @Test
    public void findFiles_empty_pattern_returns_all_files() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);

        // Create another file in a subfolder
        Path extraFile = projectPath.resolve("folder/otherfile.txt");
        Files.writeString(extraFile, "content");

        final String directory = ".";
        final String pattern = ""; // Empty pattern

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).contains("folder/testfile.txt");
        then(result).contains("folder/otherfile.txt");
    }

    @Test
    public void findFiles_directory_not_found() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String directory = "nonexistent";
        final String pattern = ".*";

        final List<PropertyChangeEvent> events = new ArrayList<>();
        tools.addPropertyChangeListener(evt -> events.add(evt));

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).isEqualTo("ERR: invalid directory " + directory);
        then(events).extracting(PropertyChangeEvent::getNewValue)
                .contains("❌ invalid directory: " + directory);
    }
}

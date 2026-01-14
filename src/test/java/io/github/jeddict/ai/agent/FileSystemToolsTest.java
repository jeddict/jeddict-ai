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
import io.github.jeddict.ai.lang.DummyJeddictBrainListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        then(tools.searchInFile(path, pattern)).contains("Match at").contains("test file");
        then(listener.collector).hasSize(1);
        then(listener.collector.get(0)).asString().isEqualTo("(onProgress,🔎 Looking for '" + pattern + "' inside '" + path + "')");
    }

    @Test
    public void testSearchInFile_NoMatches() throws Exception {
        final String path = "folder/testfile.txt";
        final String pattern = "abc";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

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

		then(listener.collector.get(0)).asString().isEqualTo("(onProgress,🔎 Looking for '" + pattern + "' inside '" + path + "')");
    }

    @Test
    public void createFile_with_and_without_existing_file() throws Exception {
        final String path = "folder/newfile.txt";
        final String content = "Sample content.";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

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

        // Logging assertions for progress messages
        int i = 0;
        then(listener.collector).hasSize(4);
        then(listener.collector.get(i++)).asString().isEqualTo("(onProgress,📄 Creating new file: " + path + ')');
        then(listener.collector.get(i++)).asString().isEqualTo("(onProgress,✅ File created successfully: " + path + ')');
        then(listener.collector.get(i++)).asString().isEqualTo("(onProgress,📄 Creating new file: " + path + ')');
        then(listener.collector.get(i++)).asString().isEqualTo("(onProgress,⚠ File already exists: " + path + ')');
    }

    @Test
    public void deleteFile_success_and_not_found() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String path = "folder/testfile.txt";

        final Path fileToDelete = Paths.get(projectDir, path);
        then(fileToDelete).exists(); // just to make sure...

        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        then(tools.deleteFile(path)).isEqualTo("File deleted");
        then(fileToDelete).doesNotExist();

        then(tools.deleteFile(path)).isEqualTo("File not found: " + path);

        // Logging assertions for progress messages
        then(listener.collector).contains(
            Pair.of("onProgress", "🗑 Attempting to delete file: " + path),
            Pair.of("onProgress", "✅ File deleted successfully: " + path),
            Pair.of("onProgress", "⚠ File not found: " + path)
        );
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";

        final FileSystemTools tools = new FileSystemTools(projectDir);

        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");

        then(tools.listFilesInDirectory(nonExistingDir)).isEqualTo("Directory not found: " + nonExistingDir);

        // Logging assertions for progress messages
        then(listener.collector).contains(
            Pair.of("onProgress", "📂 Listing content of directory: " + existingDir),
            Pair.of("onProgress", "❌ invalid directory: " + nonExistingDir)
        );
    }

    @Test
    public void readFile_success_and_failure() throws Exception {
        final String pathOK = "folder/testfile.txt";
        final Path fullPathOK = Paths.get(projectDir, pathOK);
        final String expectedContent = FileUtils.readFileToString(fullPathOK.toFile(), "UTF8");

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        //
        // success
        //
        then(tools.readFile(pathOK)).isEqualTo(expectedContent);
        then(listener.collector).hasSize(1);
        then(listener.collector.get(0)).asString().isEqualTo("(onProgress,📖 Reading file " + pathOK + ')');

        //
        // failure
        //
        final String pathKO = "nowhere.txt";
        final Path fullPathKO = Paths.get(projectDir, pathKO);
        listener.collector.clear();

        BDDAssertions.thenThrownBy( () ->
            tools.readFile(pathKO)
        );
        then(listener.collector).hasSize(2);
        then(listener.collector.get(0)).asString().isEqualTo("(onProgress,📖 Reading file " + pathKO + ')');
        then(listener.collector.get(1)).asString().isEqualTo("(onProgress,❌ Failed to read file: " + fullPathKO + ')');
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        final FileSystemTools tools = new FileSystemTools(projectDir);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        then(tools.createDirectory(path)).isEqualTo("Directory already exists: " + path);

        // Logging assertions for progress messages
        then(listener.collector).contains(
            Pair.of("onProgress", "📂 Creating new directory: " + path),
            Pair.of("onProgress", "⚠ Directory already exists: " + path)
        );
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = Paths.get(projectDir, path);

        Files.createDirectories(fullPath);

        final FileSystemTools tools = new FileSystemTools(projectDir);

        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        then(tools.deleteDirectory(path)).isEqualTo("Directory not found: " + path);

        // Logging assertions for progress messages
        then(listener.collector).contains(
            Pair.of("onProgress", "🗑 Attempting to delete directory: " + path),
            Pair.of("onProgress", "✅ Directory deleted successfully: " + path),
            Pair.of("onProgress", "🗑 Attempting to delete directory: " + path),
            Pair.of("onProgress", "⚠ Directory not found: " + path)
        );
    }

    @Test
    public void findFiles_finds_single_file() throws Exception {
        final FileSystemTools tools = new FileSystemTools(projectDir);
        final String directory = ".";
        final String pattern = ".*testfile\\.txt";

        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).contains("folder/testfile.txt");
        then(listener.collector).isNotEmpty();
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

    String result = tools.findFiles(directory, pattern);

    //
    // TODO: Back to then(result).isEmpty() once https://github.com/langchain4j/langchain4j/issues/4300
    //       will be fixed
    //
    then(result).isEqualTo("No matches found for " + pattern);
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

        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tools.addListener(listener);

        String result = tools.findFiles(directory, pattern).replace('\\', '/');

        then(result).isEqualTo("ERR: invalid directory " + directory);
        then(listener.collector).contains(
            Pair.of("onProgress", "❌ invalid directory: " + directory)
        );
    }
}

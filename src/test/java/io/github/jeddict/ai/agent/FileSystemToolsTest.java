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
import static io.github.jeddict.ai.agent.AbstractToolTest.TESTFILE;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.lang.DummyJeddictBrainListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.BDDAssertions;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileSystemToolsTest extends TestBase {

    protected FileSystemTools tools = null;
    protected DummyJeddictBrainListener listener = null;

    @BeforeEach
    public void beforeEac() throws IOException {
        tools = tools = new FileSystemTools(projectDir);
        listener = new DummyJeddictBrainListener();
        tools.addListener(listener);
    }

    @Test
    public void searchInFile_with_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "test file";

        then(tools.searchInFile(path, pattern)).contains("Match at").contains("test file");
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_with_no_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "abc";

        then(tools.searchInFile(path, pattern)).isEqualTo("No matches found");
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String pattern = "abc";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(abs, pattern));

        thenProgressContains(listener.collector.get(0), "🔎 Looking for '" + pattern + "' inside '" + abs + "'");

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(rel, pattern));
    }

    @Test
    public void createFile_with_and_without_existing_file() throws Exception {
        final String path = "folder/newfile.txt";
        final String content = "Sample content.";

        then(tools.createFile(path, content)).isEqualTo("File created");

        thenProgressContains(listener.collector.get(0), "📄 Creating file " + path);

        listener.collector.clear();
        thenThrownBy(() -> tools.createFile(path, content))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + path + " already exists");

        thenProgressContains(listener.collector.get(0), "📄 Creating file " + path);
        thenProgressContains(listener.collector.get(1), "❌ " + path + " already exists");
    }

    @Test
    public void createFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String content = "Sample content.";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(abs, content));

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(rel, content));

        thenProgressContains(listener.collector.get(0), "📄 Creating file " + rel);
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

        thenProgressContains(listener.collector.get(0), "🗑️ Deleting file " + path);

        listener.collector.clear();
        thenThrownBy(() -> tools.deleteFile(path))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(path + " does not exist");

        thenProgressContains(listener.collector.get(0), "🗑️ Deleting file " + path);
        thenProgressContains(listener.collector.get(1), "❌ " + path + " does not exist");
    }

    @Test
    public void deleteFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(abs));

        thenProgressContains(listener.collector.get(0), "🗑️ Deleting file " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(rel));
        thenProgressContains(listener.collector.get(0), "🗑️ Deleting file " + rel);
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";
        final String emptyDir = "newfolder";

        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");
        thenProgressContains(listener.collector.get(0), "📂 Listing contents of directory " + existingDir);

        listener.collector.clear();
        Files.createDirectory(projectPath.resolve(emptyDir));
        then(tools.listFilesInDirectory(emptyDir)).isEmpty();
        thenProgressContains(listener.collector.get(0), "📂 Listing contents of directory " + emptyDir);

        listener.collector.clear();
        thenThrownBy(() -> tools.listFilesInDirectory(nonExistingDir))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(nonExistingDir + " does not exist");
        thenProgressContains(listener.collector.get(0), "📂 Listing contents of directory " + nonExistingDir);
        thenProgressContains(listener.collector.get(1), "❌ " + nonExistingDir + " does not exist");
    }

    @Test
    public void listFilesInDirectory_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("folder").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(abs));
        thenProgressContains(listener.collector.get(0), "📂 Listing contents of directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside";

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(rel));
        thenProgressContains(listener.collector.get(0), "📂 Listing contents of directory " + rel);
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
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "📖 Reading file " + pathOK);

        //
        // success absolute path inside folder
        //
        listener.collector.clear();
        then(tools.readFile(fullPathOK.toString())).isEqualTo(expectedContent);
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "📖 Reading file " + fullPathOK);

        //
        // failure (not we log absolute path to make troubleshooting easier)
        //
        final String pathKO = "nowhere.txt";
        final Path fullPathKO = Paths.get(projectDir, pathKO);
        listener.collector.clear();

        BDDAssertions.thenThrownBy( () ->
            tools.readFile(pathKO)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessageContaining("failed to read file: java.nio.file.NoSuchFileException: ");

        then(listener.collector).hasSize(2);
        thenProgressContains(listener.collector.get(0), "📖 Reading file " + pathKO);
        thenProgressContains(listener.collector.get(1), "❌ Failed to read file:");
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

        thenProgressContains(listener.collector.get(0), "📖 Reading file " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() ->
            tools.readFile(rel)
        );

        thenProgressContains(listener.collector.get(0), "📖 Reading file " + rel);
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        thenProgressContains(listener.collector.get(0), "📂 Creating new directory " + path);
        thenProgressContains(listener.collector.get(1), "✅ Directory created");

        listener.collector.clear();
        thenThrownBy( () ->
            tools.createDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " already exists");

        thenProgressContains(listener.collector.get(0), "📂 Creating new directory " + path);
        thenProgressContains(listener.collector.get(1), "❌ " + path + " already exists");
    }

    @Test
    public void createDirectory_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("newfolder").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.createDirectory(abs.toString())
        );

        thenProgressContains(listener.collector.get(0), "📂 Creating new directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.createDirectory(rel)
        );

        thenProgressContains(listener.collector.get(0), "📂 Creating new directory " + rel);
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = projectPath.resolve(path);

        Files.createDirectories(fullPath);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        thenProgressContains(listener.collector.get(0), "🗑️ Deleting directory " + path);
        thenProgressContains(listener.collector.get(1), "✅ " + path + " deleted");

        listener.collector.clear();
        thenThrownBy( () ->
            tools.deleteDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " not found");
        thenProgressContains(listener.collector.get(0), "🗑️ Deleting directory " + path);
        thenProgressContains(listener.collector.get(1), "❌ " + path + " not found");

        final String notdir = projectPath.resolve(TESTFILE).toString();
        listener.collector.clear();
        thenThrownBy( () -> tools.deleteDirectory(notdir))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + notdir + " not a directory");
        thenProgressContains(listener.collector.get(0), "🗑️ Deleting directory " + notdir);
        thenProgressContains(listener.collector.get(1), "❌ " + notdir + " not a directory");
    }

    @Test
    public void deleteDirectory_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("newfolder").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.deleteDirectory(abs.toString())
        );

        thenProgressContains(listener.collector.get(0), "🗑️ Deleting directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.deleteDirectory(rel)
        );

        thenProgressContains(listener.collector.get(0), "🗑️ Deleting directory " + rel);
    }

    @Test
    public void replaceSnippetByRegexp_success_and_not_found() throws Exception {
        final Path fullPath = projectPath.resolve(TESTFILE).normalize().toRealPath();

        then(tools.replaceSnippetByRegex(TESTFILE, "for.*ing", "for testing"))
            .isEqualTo("Snippet replaced");
        then(fullPath).content().isEqualTo("This is a test file content for testing.");
        thenProgressContains(listener.collector.get(0), "🔄 Replacing text matching regex 'for.*ing' in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "✅ Snippet replaced");

        listener.collector.clear();
        then(
            tools.replaceSnippetByRegex(TESTFILE, "none", "do not change me")
        ).isEqualTo("No matches found for pattern");
        thenProgressContains(listener.collector.get(0), "🔄 Replacing text matching regex 'none' in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "❌ No matches found for regex 'none' in " + TESTFILE);

        listener.collector.clear();
        Path notExistingPath =  projectPath.resolve("notexisting.txt");
        thenThrownBy( () -> tools.replaceSnippetByRegex(
            notExistingPath.toString(), "text", "nothing"
        )).isInstanceOf(ToolExecutionException.class)
        .hasMessage("replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
        thenProgressContains(listener.collector.get(0), "🔄 Replacing text matching regex 'text' in " + projectPath.resolve("notexisting.txt"));
        thenProgressContains(listener.collector.get(1), "❌ Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
    }

    @Test
    public void replaceSnippetByRegexp_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("newfolder").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceSnippetByRegex(abs.toString(), ".*", "nothing")
        );

        thenProgressContains(listener.collector.get(0), "🔄 Replacing text matching regex '.*' in " + abs);

    //
        // relative path
    //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceSnippetByRegex(rel, ".*", "nothing")
        );

        thenProgressContains(listener.collector.get(0), "🔄 Replacing text matching regex '.*' in " + rel);
}

    @Test
    public void replaceFileContent_success_and_not_found() throws Exception {
        final Path fullPath = projectPath.resolve(TESTFILE).normalize().toRealPath();

        then(tools.replaceFileContent(TESTFILE, "new text"))
            .isEqualTo("File updated");
        then(fullPath).content().isEqualTo("new text");
        thenProgressContains(listener.collector.get(0), "🔄 Replacing content in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "✅ File content replaced");

        listener.collector.clear();
        Path notExistingPath =  projectPath.resolve("notexisting.txt");
        thenThrownBy( () -> tools.replaceFileContent(notExistingPath.toString(), "new text"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
        thenProgressContains(listener.collector.get(0), "🔄 Replacing content in " + projectPath.resolve("notexisting.txt"));
        thenProgressContains(listener.collector.get(1), "❌ Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
    }

    @Test
    public void replaceFileContent_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("newfolder").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceFileContent(abs.toString(), "nothing")
        );

        thenProgressContains(listener.collector.get(0), "🔄 Replacing content in " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceFileContent(rel, "nothing")
        );

        thenProgressContains(listener.collector.get(0), "🔄 Replacing content in " + rel);
    }

    // --------------------------------------------------------- private methods

    private void thenTriedFileOutsideProjectFolder(final Runnable exec) {
        thenThrownBy(() -> exec.run())
        .isInstanceOf(ToolExecutionException.class)
        .hasMessage("trying to reach a file outside the project folder");
        then(listener.collector).anyMatch((e) -> {
            return (
                e.getRight().equals("❌ Trying to reach a file outside the project folder")
           );
        });
}

    private void thenProgressContains(final Pair<String, Object> e, final String progress) {
        then(e).asString().contains(progress);
    }
}

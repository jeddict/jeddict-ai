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
        thenProgressContains(listener.collector.get(0), "\n🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_with_no_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "abc";

        then(tools.searchInFile(path, pattern)).isEqualTo("No matches found");
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "\n🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String pattern = "abc";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(abs, pattern));

        thenProgressContains(listener.collector.get(0), "\n🔎 Looking for '" + pattern + "' inside '" + abs + "'");

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

        thenProgressContains(listener.collector.get(0), "\n📄 Creating file " + path);

        listener.collector.clear();
        thenThrownBy(() -> tools.createFile(path, content))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + path + " already exists");

        thenProgressContains(listener.collector.get(0), "\n📄 Creating file " + path);
        thenProgressContains(listener.collector.get(1), "\n❌ " + path + " already exists");
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

        thenProgressContains(listener.collector.get(0), "\n📄 Creating file " + rel);
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

        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting file " + path);

        listener.collector.clear();
        thenThrownBy(() -> tools.deleteFile(path))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(path + " does not exist");

        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting file " + path);
        thenProgressContains(listener.collector.get(1), "\n❌ " + path + " does not exist");
    }

    @Test
    public void deleteFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(abs));

        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting file " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(rel));
        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting file " + rel);
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";
        final String emptyDir = "newfolder";

        then(tools.listFilesInDirectory(existingDir, 1)).contains("testfile.txt");
        thenProgressContains(listener.collector.get(0), "\n📂 Listing content of directory " + existingDir);

        listener.collector.clear();
        Files.createDirectory(projectPath.resolve(emptyDir));
        then(tools.listFilesInDirectory(emptyDir, 1)).isEqualTo("(empty)");
        thenProgressContains(listener.collector.get(0), "\n📂 Listing content of directory " + emptyDir);

        listener.collector.clear();
        thenThrownBy(() -> tools.listFilesInDirectory(nonExistingDir, 1))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(nonExistingDir + " does not exist");
        thenProgressContains(listener.collector.get(0), "\n📂 Listing content of directory " + nonExistingDir);
        thenProgressContains(listener.collector.get(1), "\n❌ " + nonExistingDir + " does not exist");
    }

    @Test
    public void listFilesInDirectory_tree_mode_unlimited_depth() throws Exception {
        // depth=0 → unlimited recursive tree (same behaviour as old fileTree("", 0))
        final String tree = tools.listFilesInDirectory("", 0);
        then(tree)
            .contains("pom.xml")
            .contains("folder/")
            .contains("testfile.txt");
    }

    @Test
    public void listFilesInDirectory_tree_mode_limited_depth() throws Exception {
        // depth=2 → two levels deep from the project root (direct children + their children)
        final String tree = tools.listFilesInDirectory("", 2);
        then(tree)
            .contains("src/")
            .contains("main/") // main/ is at level 2 (child of src/), so it IS included
            .doesNotContain("java/"); // java/ is at level 3, beyond depth 2
    }

    @Test
    public void listFilesInDirectory_tree_mode_sub_path() throws Exception {
        // depth=0 + sub-path → tree rooted at sub-directory
        final String tree = tools.listFilesInDirectory("folder", 0);
        then(tree)
            .contains("testfile.txt")
            .doesNotContain("pom.xml");
    }

    @Test
    public void listFilesInDirectory_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("folder").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(abs, 1));
        thenProgressContains(listener.collector.get(0), "\n📂 Listing content of directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside";

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(rel, 1));
        thenProgressContains(listener.collector.get(0), "\n📂 Listing content of directory " + rel);
    }

    @Test
    public void readFileLines_success_and_failure() throws Exception {
        //
        // create a multi-line file for testing
        //
        final String path = "folder/multiline.txt";
        final String content = "line one\nline two\nline three\nline four\nline five";
        tools.createFile(path, content);
        listener.collector.clear();

        //
        // success: read a range in the middle
        //
        then(tools.readFileLines(path, 2, 4)).isEqualTo("line two\nline three\nline four");
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + path + " lines 2 to 4");

        //
        // success: single line
        //
        listener.collector.clear();
        then(tools.readFileLines(path, 3, 3)).isEqualTo("line three");

        //
        // success: toLine beyond EOF is silently truncated to last line (LLM cannot know file length)
        //
        listener.collector.clear();
        then(tools.readFileLines(path, 4, 100)).isEqualTo("line four\nline five");
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + path + " lines 4 to 100");

        //
        // success: fromLine beyond end of file returns empty (valid params, file just too short)
        //
        listener.collector.clear();
        then(tools.readFileLines(path, 10, 20)).isEqualTo("");

        //
        // failure: fromLine < 1 (must not be silently clamped)
        //
        listener.collector.clear();
        thenThrownBy(() -> tools.readFileLines(path, 0, 2))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("fromLine must be >= 1, got: 0");

        //
        // failure: toLine < 1
        //
        listener.collector.clear();
        thenThrownBy(() -> tools.readFileLines(path, 1, 0))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("toLine must be >= 1, got: 0");

        //
        // failure: both fromLine and toLine below 1
        //
        listener.collector.clear();
        thenThrownBy(() -> tools.readFileLines(path, -5, -1))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("fromLine must be >= 1, got: -5");

        //
        // failure: fromLine > toLine (inverted range)
        //
        listener.collector.clear();
        thenThrownBy(() -> tools.readFileLines(path, 4, 2))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("fromLine (4) must be <= toLine (2)");

        //
        // failure: file does not exist
        //
        final String pathKO = "nowhere.txt";
        listener.collector.clear();
        thenThrownBy(() -> tools.readFileLines(pathKO, 1, 3))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("failed to read file: java.nio.file.NoSuchFileException: ");
        then(listener.collector).hasSize(2);
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + pathKO + " lines 1 to 3");
        thenProgressContains(listener.collector.get(1), "\n❌ Failed to read file:");
    }

    @Test
    public void readFileLines_fails_on_paths_outside_project_folder() throws Exception {
        final Path abs = HOME.resolve("jeddict.json").toAbsolutePath().normalize();

        //
        // absolute path
        //
        thenTriedFileOutsideProjectFolder(() ->
            tools.readFileLines(abs.toString(), 1, 5)
        );
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + abs + " lines 1 to 5");

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() ->
            tools.readFileLines(rel, 1, 5)
        );
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + rel + " lines 1 to 5");
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
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + pathOK);

        //
        // success absolute path inside folder
        //
        listener.collector.clear();
        then(tools.readFile(fullPathOK.toString())).isEqualTo(expectedContent);
        then(listener.collector).hasSize(1);
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + fullPathOK);

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
        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + pathKO);
        thenProgressContains(listener.collector.get(1), "\n❌ Failed to read file:");
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

        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() ->
            tools.readFile(rel)
        );

        thenProgressContains(listener.collector.get(0), "\n📖 Reading file " + rel);
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        thenProgressContains(listener.collector.get(0), "\n📂 Creating new directory " + path);
        thenProgressContains(listener.collector.get(1), "\n✅ Directory created");

        listener.collector.clear();
        thenThrownBy( () ->
            tools.createDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " already exists");

        thenProgressContains(listener.collector.get(0), "\n📂 Creating new directory " + path);
        thenProgressContains(listener.collector.get(1), "\n❌ " + path + " already exists");
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

        thenProgressContains(listener.collector.get(0), "\n📂 Creating new directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.createDirectory(rel)
        );

        thenProgressContains(listener.collector.get(0), "\n📂 Creating new directory " + rel);
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = projectPath.resolve(path);

        Files.createDirectories(fullPath);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting directory " + path);
        thenProgressContains(listener.collector.get(1), "\n✅ " + path + " deleted");

        listener.collector.clear();
        thenThrownBy( () ->
            tools.deleteDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " not found");
        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting directory " + path);
        thenProgressContains(listener.collector.get(1), "\n❌ " + path + " not found");

        final String notdir = projectPath.resolve(TESTFILE).toString();
        listener.collector.clear();
        thenThrownBy( () -> tools.deleteDirectory(notdir))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + notdir + " not a directory");
        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting directory " + notdir);
        thenProgressContains(listener.collector.get(1), "\n❌ " + notdir + " not a directory");
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

        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting directory " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.deleteDirectory(rel)
        );

        thenProgressContains(listener.collector.get(0), "\n🗑️ Deleting directory " + rel);
    }

    @Test
    public void replaceSnippetByRegexp_success_and_not_found() throws Exception {
        final Path fullPath = projectPath.resolve(TESTFILE).normalize().toRealPath();

        then(tools.replaceSnippetByRegex(TESTFILE, "for.*ing", "for testing"))
            .isEqualTo("Snippet replaced");
        then(fullPath).content().isEqualTo("This is a test file content for testing.");
        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing text matching regex 'for.*ing' in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "\n✅ Snippet replaced");

        listener.collector.clear();
        then(
            tools.replaceSnippetByRegex(TESTFILE, "none", "do not change me")
        ).isEqualTo("No matches found for pattern");
        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing text matching regex 'none' in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "\n❌ No matches found for regex 'none' in " + TESTFILE);

        listener.collector.clear();
        Path notExistingPath =  projectPath.resolve("notexisting.txt").normalize();
        thenThrownBy( () -> tools.replaceSnippetByRegex(
            notExistingPath.toString(), "text", "nothing"
        )).isInstanceOf(ToolExecutionException.class)
        .hasMessage("replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing text matching regex 'text' in " + projectPath.resolve("notexisting.txt"));
        thenProgressContains(listener.collector.get(1), "\n❌ Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
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

        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing text matching regex '.*' in " + abs);

    //
        // relative path
    //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceSnippetByRegex(rel, ".*", "nothing")
        );

        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing text matching regex '.*' in " + rel);
}

    @Test
    public void replaceFileContent_success_and_not_found() throws Exception {
        final Path fullPath = projectPath.resolve(TESTFILE).normalize().toRealPath();

        then(tools.replaceFileContent(TESTFILE, "new text"))
            .isEqualTo("File updated");
        then(fullPath).content().isEqualTo("new text");
        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing content in " + TESTFILE);
        thenProgressContains(listener.collector.get(1), "\n✅ File content replaced");

        listener.collector.clear();
        Path notExistingPath =  projectPath.resolve("notexisting.txt");
        thenThrownBy( () -> tools.replaceFileContent(notExistingPath.toString(), "new text"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing content in " + projectPath.resolve("notexisting.txt"));
        thenProgressContains(listener.collector.get(1), "\n❌ Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
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

        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing content in " + abs);

        //
        // relative path
        //
        listener.collector.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceFileContent(rel, "nothing")
        );

        thenProgressContains(listener.collector.get(0), "\n🔄 Replacing content in " + rel);
    }

    @Test
    public void fileTree_returns_full_tree_for_project_root() throws Exception {
        final String tree = tools.fileTree("", 0);
        then(tree)
            .contains("pom.xml")
            .contains("folder/")
            .contains("testfile.txt");
    }

    @Test
    public void fileTree_with_sub_path_returns_subtree() throws Exception {
        final String tree = tools.fileTree("folder", 0);
        then(tree)
            .contains("testfile.txt")
            .doesNotContain("pom.xml");
    }

    @Test
    public void fileTree_with_depth_limits_traversal() throws Exception {
        // depth=1: only direct children of project root are included
        final String tree = tools.fileTree("", 1);
        then(tree)
            .contains("pom.xml")
            .contains("src/")
            .doesNotContain("main/");
    }

    @Test
    public void dirTree_returns_only_directories() throws Exception {
        final String tree = tools.dirTree();
        then(tree)
            .contains("folder/")
            .doesNotContain("pom.xml")
            .doesNotContain("testfile.txt");
    }

    @Test
    public void getFileTree_static_returns_file_tree_for_project_root() throws Exception {
        final String tree = FileSystemTools.getFileTree(projectPath, null, 0);
        then(tree)
            .contains("pom.xml")
            .contains("folder/")
            .contains("testfile.txt");
    }

    @Test
    public void getFileTree_static_with_sub_path_returns_subtree() throws Exception {
        final String tree = FileSystemTools.getFileTree(projectPath, "folder", 0);
        then(tree)
            .contains("testfile.txt")
            .doesNotContain("pom.xml");
    }

    @Test
    public void getFileTree_static_with_depth_limits_traversal() throws Exception {
        final String tree = FileSystemTools.getFileTree(projectPath, null, 1);
        then(tree)
            .contains("pom.xml")
            .contains("src/")
            .doesNotContain("main/");
    }

    @Test
    public void getFileTree_static_with_path_outside_project_returns_error() throws Exception {
        final String result = FileSystemTools.getFileTree(projectPath, "../../../etc", 0);
        then(result).contains("outside the project");
    }

    @Test
    public void getFileTree_static_returns_exception_for_null_root() {
        thenThrownBy(() -> FileSystemTools.getFileTree(null, null, 0))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("project root is not set");
    }

    @Test
    public void getDirTree_static_returns_directory_hierarchy() throws Exception {
        final String tree = FileSystemTools.getDirTree(projectPath);
        then(tree)
            .contains("folder/")
            .doesNotContain("pom.xml")
            .doesNotContain("testfile.txt");
    }

    @Test
    public void getDirTree_static_returns_exception_for_null_root() {
        thenThrownBy(() -> FileSystemTools.getDirTree(null))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("project root is not set");
    }

    // --------------------------------------------------------- private methods

    private void thenTriedFileOutsideProjectFolder(final Runnable exec) {
        thenThrownBy(() -> exec.run())
        .isInstanceOf(ToolExecutionException.class)
        .hasMessage("trying to reach a file outside the project folder");
        then(listener.collector).anyMatch((e) -> {
            return (
                e.getRight().equals("\n❌ Trying to reach a file outside the project folder")
           );
        });
}

    private void thenProgressContains(final Pair<String, Object> e, final String progress) {
        then(e).asString().contains(progress);
    }
}

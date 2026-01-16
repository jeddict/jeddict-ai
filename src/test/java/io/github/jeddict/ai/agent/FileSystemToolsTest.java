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
        thenProgressMatches(events.get(0), "🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_with_no_matches() throws Exception {
        final String path = TESTFILE;
        final String pattern = "abc";

        then(tools.searchInFile(path, pattern)).isEqualTo("No matches found");
        then(events).hasSize(1);
        thenProgressMatches(events.get(0), "🔎 Looking for '" + pattern + "' inside '" + path + "'");
    }

    @Test
    public void searchInFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String pattern = "abc";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(abs, pattern));

        thenProgressMatches(events.get(0), "🔎 Looking for '" + pattern + "' inside '" + abs + "'");

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.searchInFile(rel, pattern));

        thenProgressMatches(events.get(0), "🔎 Looking for '" + pattern + "' inside '" + rel + "'");
    }

    @Test
    public void createFile_with_and_without_existing_file() throws Exception {
        final String path = "folder/newfile.txt";
        final String content = "Sample content.";

        then(tools.createFile(path, content)).isEqualTo("File created");

        thenProgressMatches(events.get(0), "📄 Creating file " + path);

        events.clear();
        thenThrownBy(() -> tools.createFile(path, content))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + path + " already exists");

        thenProgressMatches(events.get(0), "📄 Creating file " + path);
        thenProgressMatches(events.get(1), "❌ " + path + " already exists");
    }

    @Test
    public void createFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();
        final String content = "Sample content.";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(abs, content));

        thenProgressMatches(events.get(0), "📄 Creating file " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.createFile(rel, content));

        thenProgressMatches(events.get(0), "📄 Creating file " + rel);
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

        thenProgressMatches(events.get(0), "🗑️ Deleting file " + path);

        events.clear();
        thenThrownBy(() -> tools.deleteFile(path))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(path + " does not exist");

        thenProgressMatches(events.get(0), "🗑️ Deleting file " + path);
        thenProgressMatches(events.get(1), "❌ " + path + " does not exist");
    }

    @Test
    public void deleteFile_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("jeddict-config.json").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(abs));

        thenProgressMatches(events.get(0), "🗑️ Deleting file " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() -> tools.deleteFile(rel));
        thenProgressMatches(events.get(0), "🗑️ Deleting file " + rel);
    }

    @Test
    public void listFilesInDirectory_success_and_not_found() throws Exception {
        final String existingDir = "folder";
        final String nonExistingDir = "nonexistingdir";

        then(tools.listFilesInDirectory(existingDir)).contains("testfile.txt");
        thenProgressMatches(events.get(0), "📂 Listing contents of directory " + existingDir);

        events.clear();
        thenThrownBy(() -> tools.listFilesInDirectory(nonExistingDir))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage(nonExistingDir + " does not exist");
        thenProgressMatches(events.get(0), "📂 Listing contents of directory " + nonExistingDir);
        thenProgressMatches(events.get(1), "❌ " + nonExistingDir + " does not exist");
    }

    @Test
    public void listFilesInDirectory_outside_project_dir() {
        //
        // absolute path
        //
        final String abs = HOME.resolve("folder").toAbsolutePath().toString();

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(abs));
        thenProgressMatches(events.get(0), "📂 Listing contents of directory " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside";

        thenTriedFileOutsideProjectFolder(() -> tools.listFilesInDirectory(rel));
        thenProgressMatches(events.get(0), "📂 Listing contents of directory " + rel);
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
        thenProgressMatches(events.get(0), "📖 Reading file " + pathOK);

        //
        // success absolute path inside folder
        //
        events.clear();
        then(tools.readFile(fullPathOK.toString())).isEqualTo(expectedContent);
        then(events).hasSize(1);
        thenProgressMatches(events.get(0), "📖 Reading file " + fullPathOK);

        //
        // failure (not we log absolute path to make troubleshooting easier)
        //
        final String pathKO = "nowhere.txt";
        final Path fullPathKO = Paths.get(projectDir, pathKO);
        events.clear();

        thenThrownBy( () ->
            tools.readFile(pathKO)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessageContaining("failed to read file: java.nio.file.NoSuchFileException: ");

        then(events).hasSize(2);
        thenProgressMatches(events.get(0), "📖 Reading file " + pathKO);
        thenProgressMatches(events.get(1), "❌ Failed to read file: .*");
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

        thenProgressMatches(events.get(0), "📖 Reading file " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outside.txt";

        thenTriedFileOutsideProjectFolder(() ->
            tools.readFile(rel)
        );

        thenProgressMatches(events.get(0), "📖 Reading file " + rel);
    }

    @Test
    public void createDirectory_success_and_exists() throws Exception {
        final String path = "newdir";

        then(tools.createDirectory(path)).isEqualTo("Directory created");
        thenProgressMatches(events.get(0), "📂 Creating new directory " + path);
        thenProgressMatches(events.get(1), "✅ Directory created successfully");

        events.clear();
        thenThrownBy( () ->
            tools.createDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " already exists");

        thenProgressMatches(events.get(0), "📂 Creating new directory " + path);
        thenProgressMatches(events.get(1), "❌ " + path + " already exists");
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

        thenProgressMatches(events.get(0), "📂 Creating new directory " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.createDirectory(rel)
        );

        thenProgressMatches(events.get(0), "📂 Creating new directory " + rel);
    }

    @Test
    public void deleteDirectory_success_and_not_found() throws Exception {
        final String path = "newdir";
        final Path fullPath = projectPath.resolve(path);

        Files.createDirectories(fullPath);

        then(tools.deleteDirectory(path)).isEqualTo("Directory deleted");
        thenProgressMatches(events.get(0), "🗑️ Deleting directory " + path);
        thenProgressMatches(events.get(1), "✅ " + path + " deleted successfully");

        events.clear();
        thenThrownBy( () ->
            tools.deleteDirectory(path)
        ).isInstanceOf(ToolExecutionException.class)
        .hasMessage("❌ " + path + " not found");
        thenProgressMatches(events.get(0), "🗑️ Deleting directory " + path);
        thenProgressMatches(events.get(1), "❌ " + path + " not found");

        final String notdir = projectPath.resolve(TESTFILE).toString();
        events.clear();
        thenThrownBy( () -> tools.deleteDirectory(notdir))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessage("❌ " + notdir + " not a directory");
        thenProgressMatches(events.get(0), "🗑️ Deleting directory " + notdir);
        thenProgressMatches(events.get(1), "❌ " + notdir + " not a directory");
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

        thenProgressMatches(events.get(0), "🗑️ Deleting directory " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.deleteDirectory(rel)
        );

        thenProgressMatches(events.get(0), "🗑️ Deleting directory " + rel);
    }

    @Test
    public void replaceSnippetByRegexp_success_and_not_found() throws Exception {
        final Path fullPath = projectPath.resolve(TESTFILE).normalize().toRealPath();

        then(tools.replaceSnippetByRegex(TESTFILE, "for.*ing", "for testing"))
            .isEqualTo("Snippet replaced successfully");
        then(fullPath).content().isEqualTo("This is a test file content for testing.");
        thenProgressMatches(events.get(0), "🔄 Replacing text matching regex 'for.*ing' in file " + TESTFILE);
        thenProgressMatches(events.get(1), "✅ Snippet replaced successfully");

        events.clear();
        then(
            tools.replaceSnippetByRegex(TESTFILE, "none", "do not change me")
        ).isEqualTo("No matches found for pattern");
        thenProgressMatches(events.get(0), "🔄 Replacing text matching regex 'none' in file " + TESTFILE);
        thenProgressMatches(events.get(1), "❌ No matches found for regex 'none' in file " + TESTFILE);

        events.clear();
        Path notExistingPath =  projectPath.resolve("notexisting.txt");
        thenThrownBy( () -> tools.replaceSnippetByRegex(
            notExistingPath.toString(), "text", "nothing"
        )).isInstanceOf(ToolExecutionException.class)
        .hasMessage("Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
        thenProgressMatches(events.get(0), "🔄 Replacing text matching regex 'text' in file " + projectPath.resolve("notexisting.txt"));
        thenProgressMatches(events.get(1), "❌ Replacement failed: java.nio.file.NoSuchFileException: " + notExistingPath);
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

        thenProgressMatches(events.get(0), "🔄 Replacing text matching regex '.*' in file " + abs);

        //
        // relative path
        //
        events.clear();

        final String rel = projectDir + File.separator + "../outsidedir";

        thenTriedFileOutsideProjectFolder(() ->
            tools.replaceSnippetByRegex(rel, ".*", "nothing")
        );

        thenProgressMatches(events.get(0), "🔄 Replacing text matching regex '.*' in file " + rel);
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

    private void thenProgressMatches(final PropertyChangeEvent e, final String progressRegex) {
        then(e.getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(e.getNewValue()).matches((s) -> ((String)s).matches(progressRegex));
    }
}

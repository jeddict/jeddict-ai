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

package io.github.jeddict.ai.components.diff;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.DummyProject;
import io.github.jeddict.ai.test.TestBase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileUtil;

/**
 *
 */
@CacioTest
public class DiffPaneControllerTest extends TestBase {

    final private static String F = "folder/testfile.txt";
    final private static String C = "new content";

    private DummyProject P;

    @BeforeEach
    public void before() throws Exception {
        P = new DummyProject(projectPath);
    }
    
    @Test
    public void creation() throws Exception {
        DiffPaneController ctrl = new DiffPaneController(P, F, C);
        
        then(ctrl.project).isSameAs(P);
        
        //
        // Updating a file
        //
        then(ctrl.path).isEqualTo(Paths.get(F).toString());
        then(ctrl.fullPath())
            .isEqualTo(Paths.get(P.getProjectDirectory().getPath()).toRealPath().resolve(F).toAbsolutePath());
        then(ctrl.original).isNotNull();
        then(ctrl.modified).isNotNull();
        then(ctrl.isNewFile).isFalse();
        
        //
        // Creating a new file
        //
        ctrl = new DiffPaneController(P, "newfile.txt", C);
        then(ctrl.path).isEqualTo("newfile.txt");
        then(ctrl.fullPath().toString()).isEqualTo(new File(P.getProjectDirectory().getPath(), "newfile.txt").getAbsolutePath());
        then(ctrl.original).isNull();
        then(ctrl.modified).isNotNull();
        then(ctrl.isNewFile).isTrue();
    }

    @Test
    public void creation_throws_exception_for_outside_project_dir() throws Exception {
        final String PATH = "../outside.txt";

        assertThatThrownBy(() -> new DiffPaneController(P, PATH, C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("file path '" + new File(projectDir, PATH).getCanonicalPath() + "' must be within the project directory");
    }

    @Test
    public void creation_with_absolute_path_resolves_into_a_path_inside_project() throws Exception {
        final Path abs = FileUtil.toPath(P.getProjectDirectory()).resolve(F).toAbsolutePath();

        final DiffPaneController ctrl = new DiffPaneController(P, abs.toString(), C);

        then(ctrl.fullPath().toString()).isEqualTo(abs.toString());
    }

    @Test
    public void project_cannot_be_null() {
        thenThrownBy(() -> new DiffPaneController(null, F, C))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("project cannot be null");
    }

    @Test
    public void action_cannot_be_null_nor_invalid() throws Exception {
        thenThrownBy(() -> new DiffPaneController(P, null, C))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("path cannot be null");
        
        //
        // Make the project dir invalid by deleteing it
        //
        P.getProjectDirectory().delete();
        thenThrownBy(() -> new DiffPaneController(P, F, C))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid project directory")
            .hasMessageContaining(Paths.get(P.getProjectDirectory().getPath()).toString()); // keep Windows into account
    }

    @Test
    public void return_current_content() throws Exception {
        final Path path = Paths.get(P.getProjectDirectory().getPath()).resolve(F);
        final String fileText = Files.readString(path); 
        
        //
        // modified is the modified text of the file
        //
        final DiffPaneController ctrl = new DiffPaneController(P, F, C);
        then(ctrl.modified()).isEqualTo(fileText);

        //
        // if the file modified changes, the new text is returned
        //
        Files.writeString(path, "new line", StandardOpenOption.APPEND);

        then(ctrl.modified()).endsWith("new line").isEqualTo(Files.readString(path));

        //
        // If for any reasons the file does not exist any more, return null
        //
        Files.delete(path);
        then(ctrl.modified()).isNull();
    }
    
    @Test
    public void save_saves_modified_to_original() throws Exception {
        //
        // Update existing file
        //
        DiffPaneController ctrl = new DiffPaneController(P, F, C + "\nnew line");
        
        ctrl.save(C + "\nnew line");
        then(ctrl.fullPath()).hasContent(C + "\nnew line");
        
        ctrl.save("hello");
        then(ctrl.fullPath()).hasContent("hello");
        
        //
        // Absolute path
        //
        ctrl = new DiffPaneController(
            P, 
            FileUtil.toPath(P.getProjectDirectory().getFileObject(F)).toAbsolutePath().toString(),
            C
        );
        ctrl.save("absolute path");
        then(ctrl.fullPath()).hasContent("absolute path");
    }
    
    @Test
    public void save_creates_a_new_file() throws Exception {
        final Path newFilePath = Paths.get("newfolder/newfile.txt");
        
        //
        // Create a new file
        //
        DiffPaneController ctrl = new DiffPaneController(
            P, "cities.txt", ""
        );
        
        ctrl.save("""
                  1. Paris, France
                  2. New York City, USA
                  3. New Delhi, India
                  """);
        Path expectedPath = Paths.get(P.realProjectDirectory).resolve("cities.txt");
        then(expectedPath).exists().content().contains("New York");
        
        
        
        ctrl = new DiffPaneController(P, newFilePath.toString(), "");
        ctrl.save("hello");
        expectedPath = Paths.get(P.realProjectDirectory).resolve(newFilePath);
        then(expectedPath).exists().content().isEqualTo("hello");
        
        //
        // Absolute path
        //
        ctrl = new DiffPaneController(
            P, 
            FileUtil.toPath(P.getProjectDirectory().getFileObject("newfile.txt", false)).toAbsolutePath().toString(),
            C
        );
        ctrl.save("absolute path");
        then(ctrl.fullPath()).hasContent("absolute path");
    }
}

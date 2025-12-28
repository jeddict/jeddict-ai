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

import java.io.IOException;
import io.github.jeddict.ai.test.DummyProject;
import io.github.jeddict.ai.test.TestBase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;

/**
 *
 */
public class DiffPaneControllerTest extends TestBase {

    final private static String F = "folder/testfile.txt";
    final private static String C = "new content";

    private Project P;


    @BeforeEach
    public void before() throws IOException {
        P = new DummyProject(projectPath);
    }

    @Test
    public void creation() {

        DiffPaneController ctrl = new DiffPaneController(P, F, C);
        then(ctrl.project).isSameAs(P);
        then(ctrl.path).isSameAs(F);
        then(ctrl.fullPath).isEqualTo(new File(P.getProjectDirectory().getPath(), F).getAbsolutePath());
    }

    @Test
    public void creation_throws_exception_for_outside_project_dir() {
        final String PATH = "../outside.txt";

        assertThatThrownBy(() -> new DiffPaneController(P, PATH, C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("file path '" + Paths.get(projectDir, PATH).toAbsolutePath().normalize() + "' must be within the project directory");
    }

    @Test
    public void project_cannot_be_null() {
        assertThatThrownBy(() -> new DiffPaneController(null, F, C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project cannot be null");
    }

    @Test
    public void action_cannot_be_null() {
        assertThatThrownBy(() -> new DiffPaneController(P, null, C))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path cannot be null");
    }

    @Test
    public void return_actual_content() throws Exception {
        //
        // The actual content is the content currently saved in the modified
        // text (the original, modified with some or all changes of the diff
        // and saved to the original file
        //
        final DiffPaneController ctrl = new DiffPaneController(P, F, C);
        final Path path = Paths.get(P.getProjectDirectory().getPath()).resolve(F);

        then(ctrl.content()).isEqualTo(Files.readString(path));

        //
        // Simulating applying changes by writing to the file
        //
        Files.writeString(path, "new line", StandardOpenOption.APPEND);

        then(ctrl.content()).contains("new line").isEqualTo(Files.readString(path));

        //
        // If for any reasons the file does not exist any more, return null
        //
        Files.delete(path);
        then(ctrl.content()).isNull();
    }
}

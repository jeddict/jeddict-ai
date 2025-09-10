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

package io.github.jeddict.ai.components.actions;

import java.io.IOException;
import io.github.jeddict.ai.agent.FileAction;
import io.github.jeddict.ai.test.DummyProject;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/**
 *
 */
public class ActionPaneControllerTest {

    private FileSystem FS;
    private FileObject projectDir;
    private Project P;

    @Before
    public void setup() throws IOException {
        FS = FileUtil.createMemoryFileSystem();
        projectDir = FS.getRoot().createFolder("dummy-project");
        P = new DummyProject(projectDir);
    }

    @Test
    public void creation() {
        final FileAction A = new FileAction("create", "src/main/java/SayHello.java", "hello world");
        ActionPaneController ctrl = new ActionPaneController(P, A);
        then(ctrl.project).isSameAs(P);
        then(ctrl.action).isSameAs(A);
        then(ctrl.fullActionPath).isEqualTo("/dummy-project/src/main/java/SayHello.java");
    }

    @Test
    public void creation_throws_exception_for_outside_project_dir() {
        final String PATH = "../outside.txt";
        final FileAction action = new FileAction("create", PATH, null);

        assertThatThrownBy(() -> new ActionPaneController(P, action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("file path '/outside.txt' must be within the project directory");
    }

    @Test
    public void project_cannot_be_null() {
        final FileAction A = new FileAction("create", "/dummy-project/src/main/java/SayHello.java", "hello world");
        assertThatThrownBy(() -> new ActionPaneController(null, A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project cannot be null");
    }

    @Test
    public void action_cannot_be_null() {
        assertThatThrownBy(() -> new ActionPaneController(P, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("action cannot be null");
    }

    @Test
    public void create_file_in_project() throws IOException {
        String filePath = "src/main/java/NewFile.java";
        String fileContent = "public class NewFile {}";
        final FileAction A = new FileAction("create", filePath, fileContent);
        ActionPaneController ctrl = new ActionPaneController(P, A);
        ctrl.createFile();

        FileObject newFile = projectDir.getFileObject(filePath);
        then(newFile).isNotNull();
        then(newFile.asText()).isEqualTo(fileContent);
    }

    @Test
    public void delete_file_from_project() throws IOException {
        String filePath = "src/main/java/FileToDelete.java";
        String fileContent = "public class FileToDelete {}";
        final FileAction createAction = new FileAction("create", filePath, fileContent);
        ActionPaneController createCtrl = new ActionPaneController(P, createAction);
        createCtrl.createFile();

        FileObject fileToDelete = projectDir.getFileObject(filePath);
        then(fileToDelete).isNotNull();

        final FileAction deleteAction = new FileAction("delete", filePath, null);
        ActionPaneController deleteCtrl = new ActionPaneController(P, deleteAction);
        deleteCtrl.deleteFile();

        then(projectDir.getFileObject(filePath)).isNull();
    }
}

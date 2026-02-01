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
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;

/**
 *
 */
@CacioTest
public class DiffPaneControllerTest extends TestBase {
    
    protected FrameFixture window;
    protected JFrame frame;
    protected Container content;
    

    final private static String F = "folder/testfile.txt";
    final private static String C = "new content";

    private Project P;

    @BeforeEach
    public void before() throws Exception {
        P = new DummyProject(projectPath);
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle(("Test ArgumentPane"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content = frame.getContentPane();
        content.setPreferredSize(new Dimension(300, 300));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        window = new FrameFixture(frame);
        window.show();
    }
    
    @AfterEach
    public void afterEach() {
        super.afterEach();
        window.cleanUp();
    }

    @Test
    public void creation() {
        DiffPaneController ctrl = new DiffPaneController(P, F, C);
        
        then(ctrl.project).isSameAs(P);
        
        //
        // Updating a file
        //
        then(ctrl.path).isEqualTo(F);
        then(ctrl.fullPath()).isEqualTo(new File(P.getProjectDirectory().getPath(), F).getAbsolutePath());
        then(ctrl.original).isNotNull();
        then(ctrl.modified).isNotNull();
        then(ctrl.isNewFile()).isFalse();
        
        //
        // Creating a new file
        //
        ctrl = new DiffPaneController(P, "newfile.txt", C);
        then(ctrl.path).isEqualTo("newfile.txt");
        then(ctrl.fullPath()).isEqualTo(new File(P.getProjectDirectory().getPath(), "newfile.txt").getAbsolutePath());
        then(ctrl.original).isNotNull();
        then(ctrl.modified).isNotNull();
        then(ctrl.isNewFile()).isTrue();
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

        then(ctrl.fullPath()).isEqualTo(abs.toString());
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
    public void return_current_content() throws Exception {
        final Path path = Paths.get(P.getProjectDirectory().getPath()).resolve(F);
        final String fileText = Files.readString(path); 
        
        //
        // original is the original text of the file
        //
        final DiffPaneController ctrl = new DiffPaneController(P, F, C);
        then(ctrl.original()).isEqualTo(fileText);

        //
        // if the file original changes, the new text is returned
        //
        Files.writeString(path, "new line", StandardOpenOption.APPEND);

        then(ctrl.original()).endsWith("new line").isEqualTo(Files.readString(path));

        //
        // If for any reasons the file does not exist any more, return null
        //
        Files.delete(path);
        then(ctrl.original()).isNull();
    }
    
    @Test
    public void save_saves_modified_to_original() throws Exception {
        //
        // Update existing file
        //
        DiffPaneController ctrl = new DiffPaneController(P, F, C + "\nnew line");
        
        ctrl.save(C + "\nnew line");
        then(new File(ctrl.fullPath())).hasContent(C + "\nnew line");
        
        ctrl.save("hello");
        then(new File(ctrl.fullPath())).hasContent("hello");
        
        //
        // Absolute path
        //
        ctrl = new DiffPaneController(
            P, 
            FileUtil.toPath(P.getProjectDirectory().getFileObject(F)).toAbsolutePath().toString(),
            C
        );
        ctrl.save("absolute path");
        then(new File(ctrl.fullPath())).hasContent("absolute path");
    }
    
    @Test
    public void save_creates_a_new_file() throws Exception {
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
        then(Paths.get(P.getProjectDirectory().getPath()).resolve("cities.txt")).exists();
        then(ctrl.original.asText()).contains("New York");
        
        ctrl = new DiffPaneController(
            P, "newfolder/newfile.txt", ""
        );
        ctrl.save("hello");
        then(ctrl.original.asText()).isEqualTo("hello");
        
        //
        // Absolute path
        //
        ctrl = new DiffPaneController(
            P, 
            FileUtil.toPath(P.getProjectDirectory().getFileObject("newfile.txt", false)).toAbsolutePath().toString(),
            C
        );
        ctrl.save("absolute path");
        then(new File(ctrl.fullPath())).hasContent("absolute path");
    }
}

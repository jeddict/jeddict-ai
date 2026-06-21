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
package io.github.jeddict.ai.test;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import static io.github.jeddict.ai.test.DummyProjectSources.SOURCES_TYPE_JAVA;
import static io.github.jeddict.ai.test.DummyProjectSources.SOURCES_TYPE_TEST;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.Test;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.InstanceContent;

@CacioTest
public class DummyProjectTest extends TestBase {

    @Test
    public void constructors() {
        DummyProject p = new DummyProject(projectDir);
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        p = new DummyProject(new File(projectDir));
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        p = new DummyProject(new File(projectDir).toPath());
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        thenThrownBy(() -> {
            new DummyProject((File)null);
        }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("projectDir can not be null");
    }

    @Test
    public void get_project_directory_returns_correct_file_object() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject projectDir = fs.getRoot().createFolder("test-project");
        DummyProject project = new DummyProject((FileObject) projectDir);
        then(project.getProjectDirectory()).isSameAs(projectDir);
    }

    @Test
    public void constructor_throws_exception_for_null_directory() {
        thenThrownBy(() -> new DummyProject((FileObject) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir can not be null");
    }

    @Test
    public void constructor_throws_exception_for_non_existent_file() {
        java.io.File nonExistentFile = new java.io.File("nonexistent-project-dir");
        thenThrownBy(() -> new DummyProject(nonExistentFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project directory can not be null or invalid");
    }

    @Test
    public void lookup_returns_provided_instances() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject projectDir = fs.getRoot().createFolder("test-project");
        final DummyProject project = new DummyProject(projectDir);

        then(project.instances).isInstanceOf(InstanceContent.class).isNotNull();
    }

    @Test
    public void name() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject folder = fs.getRoot().createFolder("test-project");

        final DummyProject project = new DummyProject(folder);

        project.name("hello world");
        then(project.name()).isEqualTo("hello world");

        project.name("test project");
        then(project.name()).isEqualTo("test project");
    }

    @Test
    public void real_project_dir() throws IOException {
        final DummyProject project = new DummyProject(projectDir);

        then(project.realProjectDirectory)
            .isEqualTo(Paths.get(project.getProjectDirectory().getPath()).toRealPath().toString());
    }

    @Test
    public void type() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject folder = fs.getRoot().createFolder("test-project");

        final DummyProject project = new DummyProject(folder);

        project.type("ant");
        then(project.type()).isEqualTo("ant");

        /*
        project.name("test project");
        then(project.name()).isEqualTo("test project");
*/
    }
    
    @Test
    public void main_and_test_sources() throws Exception {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject root = fs.getRoot().createFolder("test-project");

        // 1. Arrange: Create mock structure in memory
        FileObject src = root.createFolder("src");
        FileObject mainSrc = src.createFolder("main").createFolder("java");
        FileObject testSrc = src.createFolder("test").createFolder("java");

        Project project = new DummyProject(root);

        // 2. Act: Fetch Java Sources
        SourceGroup[] javaGroups = ProjectUtils.getSources(project)
                .getSourceGroups(SOURCES_TYPE_JAVA);

        // 3. Assert: Verify main sources match
        then(javaGroups).hasSize(1);
        then(javaGroups[0].getRootFolder()).isEqualTo(mainSrc);

        // 4. Act: Fetch Test Sources
        SourceGroup[] testGroups = ProjectUtils.getSources(project)
                .getSourceGroups(SOURCES_TYPE_TEST);

        // 5. Assert: Verify test sources match
        then(testGroups).hasSize(1);
        then(testGroups[0].getRootFolder()).isEqualTo(testSrc);
    }

    @Test
    public void main_and_test_sources_are_empty_if_missing_dirs() throws Exception {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject folder = fs.getRoot().createFolder("test-project");

        Project project = new DummyProject(folder);

        SourceGroup[] javaGroups = ProjectUtils.getSources(project)
                .getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        SourceGroup[] testGroups = ProjectUtils.getSources(project)
                .getSourceGroups("test");

        then(javaGroups).hasSize(0);
        then(testGroups).hasSize(0);
    }
}

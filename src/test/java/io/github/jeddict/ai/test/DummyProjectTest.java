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

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.assertj.core.api.BDDAssertions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.InstanceContent;

public class DummyProjectTest extends TestBase {

    @Test
    public void constructors() {
        DummyProject p = new DummyProject(projectDir);
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        p = new DummyProject(new File(projectDir));
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        p = new DummyProject(new File(projectDir).toPath());
        then(new File(p.getProjectDirectory().getPath())).exists().isDirectory();

        BDDAssertions.thenThrownBy(() -> {
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
        assertThatThrownBy(() -> new DummyProject((FileObject) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null");
    }

    @Test
    public void constructor_throws_exception_for_non_existent_file() {
        java.io.File nonExistentFile = new java.io.File("nonexistent-project-dir");
        assertThatThrownBy(() -> new DummyProject(nonExistentFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project directory cannot be null or invalid");
    }

    @Test
    public void lookup_returns_provided_instances() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject projectDir = fs.getRoot().createFolder("test-project");
        final DummyProject project = new DummyProject((FileObject) projectDir);

        then(project.instances).isInstanceOf(InstanceContent.class).isNotNull();
    }
}
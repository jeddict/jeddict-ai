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

import io.github.jeddict.ai.agent.BaseTest;
import java.io.File;
import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class DummyProjectTest extends BaseTest {

    @Test
    public void get_project_directory_returns_correct_file_object() throws IOException {
        FileObject projectFile = FileUtil.toFileObject(new File(projectDir));
        DummyProject project = new DummyProject(projectFile);
        then(project.getProjectDirectory()).isSameAs(projectFile);
    }

    @Test
    public void constructor_throws_exception_for_null_directory() {
        assertThatThrownBy(() -> new DummyProject((FileObject) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null");

        assertThatThrownBy(() -> new DummyProject((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null");

        assertThatThrownBy(() -> new DummyProject((File) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null");
    }

    @Test
    public void constructor_throws_exception_for_non_existent_file() {
        java.io.File nonExistentFile = new java.io.File("nonexistent-project-dir");
        assertThatThrownBy(() -> new DummyProject(nonExistentFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null or invalid");

        assertThatThrownBy(() -> new DummyProject(nonExistentFile.getAbsolutePath()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("projectDir cannot be null or invalid");
    }
}
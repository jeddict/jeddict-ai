package io.github.jeddict.ai.test;

import java.io.IOException;
import org.junit.Test;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

public class DummyProjectTest {

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
}

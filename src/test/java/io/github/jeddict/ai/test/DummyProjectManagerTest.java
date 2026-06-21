package io.github.jeddict.ai.test;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import java.io.IOException;
import java.util.Set;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Mutex;

@CacioTest
public class DummyProjectManagerTest {

    @Test
    public void findProject_returns_dummy_project_for_maven_root() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject projectDir = fs.getRoot().createFolder("maven-project");
        projectDir.createData("pom.xml");

        DummyProjectManager manager = new DummyProjectManager();

        Project project = manager.findProject(projectDir);
        then(project).isInstanceOf(DummyProject.class);
        then(project.getProjectDirectory()).isSameAs(projectDir);
        then(manager.isProject(projectDir)).isNotNull();
    }

    @Test
    public void findProject_returns_null_when_pom_is_missing() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject projectDir = fs.getRoot().createFolder("plain-folder");

        DummyProjectManager manager = new DummyProjectManager();

        then(manager.findProject(projectDir)).isNull();
        then(manager.isProject(projectDir)).isNull();
    }

    @Test
    public void findProject_detects_maven_project_from_child_file() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject projectDir = fs.getRoot().createFolder("maven-project");
        projectDir.createData("pom.xml");
        FileObject src = projectDir.createFolder("src");
        FileObject main = src.createFolder("main");
        FileObject java = main.createFolder("java");
        FileObject child = java.createData("Example.java");

        DummyProjectManager manager = new DummyProjectManager();

        then(manager.findProject(child)).isInstanceOf(DummyProject.class);
        then(manager.isProject(child)).isNotNull();
    }

    @Test
    public void utility_methods_return_expected_defaults() throws IOException {
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject projectDir = fs.getRoot().createFolder("maven-project");
        projectDir.createData("pom.xml");

        DummyProjectManager manager = new DummyProjectManager();
        DummyProject project = new DummyProject(projectDir);

        then(manager.getMutex()).isSameAs(Mutex.EVENT);
        then(manager.getMutex(true, project, new Project[0])).isSameAs(Mutex.EVENT);
        then(manager.getModifiedProjects()).isEqualTo(Set.of());
        then(manager.isModified(project)).isFalse();
        then(manager.isValid(project)).isTrue();
        then(manager.isValid(null)).isFalse();

        manager.clearNonProjectCache();
        manager.saveProject(project);
        manager.saveAllProjects();
    }
}

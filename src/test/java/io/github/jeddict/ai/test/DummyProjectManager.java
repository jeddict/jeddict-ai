package io.github.jeddict.ai.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.swing.Icon;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectManagerImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Mutex;

/**
 * Minimal test-only project manager that recognizes Maven projects.
 */
public class DummyProjectManager implements ProjectManagerImplementation {

    private static final String PROJECT_TYPE = "maven";
    private static final Icon PROJECT_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/maven/resources/Maven2Icon.gif", true);

    @Override
    public void init(@NonNull ProjectManagerCallBack pmcb) {
        // no-op for tests
    }

    @Override
    public @NonNull Mutex getMutex() {
        return Mutex.EVENT;
    }

    @Override
    public @NonNull Mutex getMutex(boolean writeAccess, @NonNull Project project, @NonNull Project[] projects) {
        return Mutex.EVENT;
    }

    @Override
    public @CheckForNull Project findProject(@NonNull FileObject fo) throws IOException, IllegalArgumentException {
        final FileObject projectDir = locateMavenProjectDirectory(fo);
        return projectDir != null ? new DummyProject(projectDir) : null;
    }

    @Override
    public @CheckForNull ProjectManager.Result isProject(@NonNull FileObject fo) throws IllegalArgumentException {
        final FileObject projectDir = locateMavenProjectDirectory(fo);
        if (projectDir == null) {
            return null;
        }
        return new ProjectManager.Result(projectDir.getNameExt(), PROJECT_TYPE, PROJECT_ICON);
    }

    @Override
    public void clearNonProjectCache() {
        // no-op
    }

    @Override
    public @NonNull Set<Project> getModifiedProjects() {
        return Collections.emptySet();
    }

    @Override
    public boolean isModified(@NonNull Project project) {
        return false;
    }

    @Override
    public boolean isValid(@NonNull Project project) {
        return project != null && project.getProjectDirectory() != null;
    }

    @Override
    public void saveProject(@NonNull Project project) throws IOException {
        // no-op
    }

    @Override
    public void saveAllProjects() throws IOException {
        // no-op
    }

    private static FileObject locateMavenProjectDirectory(FileObject fo) {
        FileObject current = fo;
        while (current != null) {
            if (current.getFileObject("pom.xml") != null) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}

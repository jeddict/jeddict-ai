package io.github.jeddict.ai.test;

import static io.github.jeddict.ai.util.ProjectUtil.isGradleProject;
import static io.github.jeddict.ai.util.ProjectUtil.isMavenProject;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectManagerImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;

/**
 * A native test SPI Project Manager capable of accurately matching
 * and routing both Maven and Gradle workspaces based on folder contents.
 */
public class DummyProjectManager implements ProjectManagerImplementation {

    @Override
    public void init(@NonNull ProjectManagerCallBack pmcb) {}

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
        if (isGradleProject(fo) || isMavenProject(fo)) {
            return new DummyProject(fo);
        }
        return null;
    }

    @Override
    public @CheckForNull ProjectManager.Result isProject(@NonNull FileObject fo) throws IllegalArgumentException {
        if (isGradleProject(fo)) {
            return new ProjectManager.Result(fo.getNameExt(), "gradle", null);
        } else if (isMavenProject(fo)) {
            return new ProjectManager.Result(fo.getNameExt(), "maven", null);
        }
        return null;
    }

    @Override
    public void clearNonProjectCache() {}

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
        return (project != null) && (project.getProjectDirectory() != null);
    }

    @Override
    public void saveProject(@NonNull Project project) throws IOException {}

    @Override
    public void saveAllProjects() throws IOException {}
}

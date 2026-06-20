package io.github.jeddict.ai.test;

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

public class DummyProjectManager implements ProjectManagerImplementation {

    @Override
    public void init(@NonNull ProjectManagerCallBack pmcb) {
    }

    @Override
    public @NonNull Mutex getMutex() {
        return Mutex.EVENT;
    }

    @Override
    public @NonNull Mutex getMutex(boolean bln, @NonNull Project prjct, @NonNull Project[] prjcts) {
        return Mutex.EVENT;
    }

    @Override
    public @CheckForNull Project findProject(@NonNull FileObject fo) throws IOException, IllegalArgumentException {
        return null;
    }

    @Override
    public @CheckForNull ProjectManager.Result isProject(@NonNull FileObject fo) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void clearNonProjectCache() {
    }

    @Override
    public @NonNull Set<Project> getModifiedProjects() {
        return Collections.emptySet();
    }

    @Override
    public boolean isModified(@NonNull Project prjct) {
        return false;
    }

    @Override
    public boolean isValid(@NonNull Project prjct) {
        return true;
    }

    @Override
    public void saveProject(@NonNull Project prjct) throws IOException {
    }

    @Override
    public void saveAllProjects() throws IOException {
    }
}

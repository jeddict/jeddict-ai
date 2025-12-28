package io.github.jeddict.ai.components.diff;

import java.io.File;
import org.netbeans.api.project.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 * {@code ActionPaneController} is a controller class that handles file-related
 * operations (create, delete) within a NetBeans project. It performs path
 * validation and interacts with the project's file system.
 */
public class DiffPaneController {

    public enum UserAction {
        ACCEPT, REJECT
    }

    public final Project project;
    public final String path;
    public final String fullPath;
    public final FileObject fileObject;

    public String content = "";
    protected Consumer<UserAction> onDone = null;

    /**
     * Constructs a new {@code ActionPaneController}.
     *
     * @param project The NetBeans project associated with the action.
     * @param path The {@code FileAction} to be performed.
     * @param content The proposed content
     *
     * @throws IllegalArgumentException if the project or action is null, or if
     * the file path specified in the action is invalid or outside the project
     * directory.
     */
    public DiffPaneController(
        final Project project, final String path, final String content
    ) throws IllegalArgumentException {
        if (project == null) {
            throw new IllegalArgumentException("project cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        this.project = project;
        this.path = path;
        this.fullPath = getValidatedFullPath(path);
        this.content = content;

        this.fileObject = FileUtil.toFileObject(Paths.get(fullPath));
    }

    public String content() {
        try {
            return Files.readString(Paths.get(fullPath));
        } catch (IOException x) {
            Exceptions.printStackTrace(x);
            return null;
        }
    }

    /**
     * Save the content of the base file. This method is public to allow classes
     * using it to save the content from outside the DiffView.
     */
    public void save() {
        try {
            if (fileObject != null) {
                //
                // the file already existed, the diff editor has the latest
                // version that can be saved with its cookie.
                //

                //
                // find should not return null, it throws a DataObjectNotFoundException
                // if the object is not found
                //
                final SaveCookie saveCookie = DataObject.find(fileObject).getLookup().lookup(SaveCookie.class);

                // If there are unsaved changes, save the file
                if (saveCookie != null) {
                    saveCookie.save();
                }
            } else {
                //
                // it's a new file, content contains the text that is saved
                // drirectly.
                //
                FileUtils.writeStringToFile(new File(fullPath), content, "UTF8");
            }
        } catch (IOException x) {
            Exceptions.printStackTrace(x);
        }
    }

    /**
     * Validates the path path and returns its canonical full path.
     * This method ensures that the file path is within the project directory.
     *
     * @param path the relative path to validate
     *
     * @return The canonical full path of the file.
     *
     * @throws IllegalArgumentException if the file path is invalid or outside
     * the project directory.
     */
    private String getValidatedFullPath(final String path) {
        //
        // NOTE: we use File here to easily manipulate the file, but bear in
        // mind not use use the File object directly. They may not be the same
        // as the files in the project, depending on the FileSystem used
        //
        try {
            final String projectFile = new File(project.getProjectDirectory().toURI().getPath()).getCanonicalPath();
            final String fullPath = new File(projectFile, path).getCanonicalPath();
            if (!fullPath.startsWith(projectFile)) {
                throw new IllegalArgumentException(
                    "file path '" + fullPath + "' must be within the project directory"
                );
            }
            return fullPath;
        } catch (IOException x) {
            throw new IllegalArgumentException("invalid path '" +
                path + "' in '" +
                project.getProjectDirectory().toURI() + ": " +
                x.getMessage()
            );
        }
    }
}
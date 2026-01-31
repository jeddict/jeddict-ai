package io.github.jeddict.ai.components.diff;

import org.netbeans.api.project.Project;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 * {@code ActionPaneController} is a controller class that handles file-related
 * operations (create, delete) within a NetBeans project. It performs path
 * validation and interacts with the project's file system.
 */
public class DiffPaneController {
    
    private final Logger LOG = Logger.getLogger(this.getClass().getName());

    public enum UserAction {
        ACCEPT, REJECT
    }

    public final Project project;
    public final String path;
    public final String fullPath;
    public final FileObject original;  // original version
    public final FileObject modified;  // new version

    protected final boolean isNewFile;
    
    protected Consumer<UserAction> onDone = null;
    

    /**
     * Constructs a new {@code DiffPaneController}.
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
 
        final FileObject original = FileUtil.toFileObject(Paths.get(fullPath));
        isNewFile = (original == null);
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject modified = null;
        try {
            modified = FileUtil.createData(fs.getRoot(), path);
        
            try (Writer w = new OutputStreamWriter(modified.getOutputStream())) {
                w.write(content); w.close();
            }
        }  catch (IOException x) {
            LOG.severe(() -> "error creating the updated version: %s".formatted(String.valueOf(x)));
        }
        this.modified = modified;
        this.original = (isNewFile) 
                      ? project.getProjectDirectory().getFileObject(path, false)
                      : original;
    }
    
    public String original() {
        try {
            return original.asText();
        } catch (IOException x) {
            LOG.severe("unexpected error retrieving the content: " + x);
            Exceptions.printStackTrace(x);
        }
        
        return null;
    }
    
    /**
     * Save the content of the base file. This method is public to allow classes
     * using it to save the content from outside the DiffView.
     */
    public void save(final String text) {
        try {
            LOG.finest(
                () -> "saving to %s %s with content:\n%s".formatted(
                    fullPath, (isNewFile) ? "new" : "existing", StringUtils.abbreviateMiddle(text, "...", 80)
                )
            );
            
            try (final Writer w = new OutputStreamWriter(original.getOutputStream())) {
                w.write(text);
            }
        } catch (IOException x) {
            Exceptions.printStackTrace(x);
        }
    }
    
    public boolean isNewFile() {
        return isNewFile;
    }

    // --------------------------------------------------------- Private methods
    
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

        final Path projectPath = FileUtil.toPath(project.getProjectDirectory()).toAbsolutePath().normalize();
        final Path filePath = Paths.get(path);
        final Path absolutePath = filePath.isAbsolute()
                            ? filePath.normalize()
                            : projectPath.resolve(filePath).normalize();
        if (!absolutePath.startsWith(projectPath)) {
            throw new IllegalArgumentException(
                "file path '" + absolutePath + "' must be within the project directory"
            );
        }
        return absolutePath.toString();
    }
}
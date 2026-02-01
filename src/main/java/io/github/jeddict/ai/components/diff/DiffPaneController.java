package io.github.jeddict.ai.components.diff;

import org.netbeans.api.project.Project;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
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
    public final String realProjectDir;
    public final String path;
    public final FileObject original;  // modified version
    public final FileObject modified;  // new version
    public final boolean isNewFile;
    
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
        this.path = getValidatedPath(path);
        
        try {
            this.realProjectDir = Paths.get(project.getProjectDirectory().getPath())
                .toRealPath().toString();
        } catch (IOException x) {
            throw new IllegalArgumentException("invalid project directory " + x.getMessage());
        }
        
        final Path fullPath = fullPath();
        isNewFile = !Files.exists(fullPath);
 
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        LOG.finest(()-> "in memory path: " + this.path);
        FileObject modified = null;
        try {
            //
            // The memory fily system uses '/' as path separator, let's make sure 
            // we use the proper name
            //
            modified = FileUtil.createData(fs.getRoot(), this.path.replace('\\', '/'));
        
            try (Writer w = new OutputStreamWriter(modified.getOutputStream())) {
                w.write(content); w.close();
            }
        }  catch (IOException x) {
            LOG.severe(() -> "error creating the updated version: %s".formatted(String.valueOf(x)));
            x.printStackTrace();
        }
        
        this.original = FileUtil.toFileObject(fullPath()); // null if it does not exist
        this.modified = modified;
    }
    
    public String modified() {
        try {
            return Files.readString(fullPath());
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
                () -> "saving to %s (%s) with content:\n%s".formatted(
                    String.valueOf(fullPath()), (isNewFile) ? "new" : "existing", StringUtils.abbreviateMiddle(text, "...", 80)
                )
            );
            
            final FileObject destination = isNewFile
//            ? FileUtil.createData(realProjectDir, path)
                ? FileUtil.createData(project.getProjectDirectory(), path.replace('\\', '/'))
                : original;
            
            try (final Writer w = new OutputStreamWriter(destination.getOutputStream())) {
                w.write(text);
            }
        } catch (IOException x) {
            LOG.severe("error saving the file: " + x);
            Exceptions.printStackTrace(x);
        }
    }
    
    public Path fullPath() {
        try {
            return Paths.get(project.getProjectDirectory().getPath())
                .toRealPath() // the project directory must exist
                .resolve(path).normalize() // the new path
                .toAbsolutePath(); // make it absolute
        } catch (IOException x) {
            //
            // This is not supposed to happen...
            //
            LOG.severe("ups... this is not supposed to happen: " + x);
            Exceptions.printStackTrace(x);
        }
        return null;
    }

    // --------------------------------------------------------- Private methods
    
    /**
     * Validates the path path and returns its canonical full path.
     * This method ensures that the file path is within the project directory.
     *
     * @param path the relative path to validate
     *
     * @return The canonical path of the file relative to the project directory
     *
     * @throws IllegalArgumentException if the file path is invalid or outside
     * the project directory.
     */
    private String getValidatedPath(final String path) {
        //
        // NOTE: we use Path here to easily manipulate the file, but bear in
        // mind not use use the Path object directly. They may not be the same
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
        return projectPath.relativize(absolutePath).toString();
    }
}
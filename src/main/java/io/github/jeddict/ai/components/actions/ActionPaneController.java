package io.github.jeddict.ai.components.actions;

import io.github.jeddict.ai.agent.FileAction;
import io.github.jeddict.ai.agent.FileSystemTools;
import java.io.File;
import org.netbeans.api.project.Project;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * {@code ActionPaneController} is a controller class that handles file-related
 * operations (create, delete) within a NetBeans project. It performs path
 * validation and interacts with the project's file system.
 */
public class ActionPaneController {

    public final Project project;
    public final FileAction action;
    public final String fullActionPath;

    private final FileSystemTools tools;

    /**
     * Constructs a new {@code ActionPaneController}.
     *
     * @param project The NetBeans project associated with the action.
     * @param action The {@code FileAction} to be performed.
     * @throws IllegalArgumentException if the project or action is null, or if
     * the file path specified in the action is invalid or outside the project
     * directory.
     */
    public ActionPaneController(Project project, FileAction action) throws IllegalArgumentException {
        if (project == null) {
            throw new IllegalArgumentException("project cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        this.project = project;
        this.action = action;

        this.fullActionPath = getValidatedFullPath();

        this.tools = new FileSystemTools(
            Paths.get(project.getProjectDirectory().getPath()).normalize().toAbsolutePath().toString()
        );
    }

    /**
     * Creates a file in the project directory based on the {@code FileAction}.
     * The file path is taken from {@code action.path()} and content from
     * {@code action.content()}.
     *
     * @throws IOException if an I/O error occurs during file creation.
     */
    public void executeAction() throws IOException {
        final String filePath = action.path();

        try {
            switch (action.action()) {
                case "delete" -> {
                    tools.deleteFile(filePath);
                }

            }
        } catch (Exception x) {
            //
            // TODO: if it is already a IOException, just rethrow it
            throw new IOException(x);
        }
    }

    /**
     * Validates the file path and returns its canonical full path.
     * This method ensures that the file path is within the project directory.
     *
     * @return The canonical full path of the file.
     * @throws IllegalArgumentException if the file path is invalid or outside
     * the project directory.
     */
    private String getValidatedFullPath() {
        //
        // NOTE: we use File here to easily manipulate the file, but bear in
        // mind not use use the File object directly. They may not be the same
        // as the files in the project, depending on the FileSystem used
        //
        try {
            final String projectFile = new File(project.getProjectDirectory().toURI().getPath()).getCanonicalPath();
            final String fullPath = new File(projectFile, action.path()).getCanonicalPath();
            if (!fullPath.startsWith(projectFile)) {
                throw new IllegalArgumentException(
                    "file path '" + fullPath + "' must be within the project directory"
                );
            }
            return fullPath;
        } catch (IOException x) {
            throw new IllegalArgumentException("invalid path '" +
                action.path() + "' in '" +
                project.getProjectDirectory().toURI() + ": " +
                x.getMessage()
            );
        }
    }
}
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
package io.github.jeddict.ai.agent;

import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.lang.JeddictStreamHandler;
import io.github.jeddict.ai.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Element;

import org.netbeans.api.project.Project;

/**
 * Collection of tools that expose file system and editor operations
 * inside Apache NetBeans to an AI assistant. 
 * <p>
 * These tools allow language models to read, modify, and manage project files
 * in a controlled and safe way.
 */
public class FileSystemTools {

    private final Project project;
    private final JeddictStreamHandler handler;

    /**
     * Creates a new {@code FileSystemTools } instance bound to the given project.
     *
     * @param project the NetBeans project in which these tools operate
     * @param handler an optional stream handler for reporting actions
     */
    public FileSystemTools(Project project, JeddictStreamHandler handler) {
        this.project = project;
        this.handler = handler;
    }

    /**
     * Reads the raw content of a file on disk.
     *
     * @param path the file path relative to the project
     * @return the file content, or an error message if it could not be read
     */
    @Tool("Read the content of a file by path")
    public String readFile(String path) {
        log("Reading", path);
        try {
            return FileUtil.readContent(project, path);
        } catch (IOException e) {
            return "Could not read file: " + e.getMessage();
        }
    }

    /**
     * Searches for a regular expression inside a file.
     *
     * @param path    the file path relative to the project
     * @param pattern the regex pattern to search for
     * @return all matches with their offsets, or a message if none were found
     */
    @Tool("Search for a regex pattern in a file by path")
    public String searchInFile(String path, String pattern) {
        log("Searching", path);
        try {
            String content = FileUtil.readContent(project, path);
            Matcher m = Pattern.compile(pattern).matcher(content);
            StringBuilder result = new StringBuilder();
            while (m.find()) {
                result.append("Match at ").append(m.start())
                        .append(": ").append(m.group()).append("\n");
            }
            return result.length() > 0 ? result.toString() : "No matches found.";
        } catch (IOException e) {
            return "Search failed: " + e.getMessage();
        }
    }

    /**
     * Reads the content of a file using the NetBeans editor API.
     *
     * @param path the file path relative to the project
     * @return the document content, or an error message
     */
    @Tool("Read the text content of a file in NetBeans by path")
    public String readFileContent(String path) {
        log("Opening", path);
        return FileUtil.withDocument(project, path, doc -> doc.getText(0, doc.getLength()), false);
    }

    /**
     * Replaces the full content of a file with the given text.
     *
     * @param path       the file path relative to the project
     * @param newContent the new content to write
     * @return a status message
     */
    @Tool("Replace the full content of a file by path with new text")
    public String replaceFileContent(String path, String newContent) {
        log("Updating", path);
        return FileUtil.withDocument(project, path, doc -> {
            try {
                doc.remove(0, doc.getLength());
                doc.insertString(0, newContent, null);
                return "File updated";
            } catch (Exception e) {
                return "Update failed: " + e.getMessage();
            }
        }, true);
    }

    /**
     * Inserts text into a file at the specified offset.
     *
     * @param path    the file path relative to the project
     * @param offset  the character offset where text should be inserted
     * @param newText the text to insert
     * @return a status message
     */
    @Tool("Insert text at a given offset in a file by path")
    public String insertTextInFile(String path, int offset, String newText) {
        log("Inserting text into", path);
        return FileUtil.withDocument(project, path, doc -> {
            try {
                doc.insertString(offset, newText, null);
                return "Inserted text at " + offset;
            } catch (Exception e) {
                return "Insert failed: " + e.getMessage();
            }
        }, true);
    }

    /**
     * Inserts a line at the given line number (0-based).
     *
     * @param path       the file path relative to the project
     * @param lineNumber the line number (0-based)
     * @param lineText   the text of the new line
     * @return a status message
     */
    @Tool("Insert a line of code at a given line number (0-based) in a file by path")
    public String insertLineInFile(String path, int lineNumber, String lineText) {
        log("Adding line to", path);
        return FileUtil.withDocument(project, path, doc -> {
            try {
                Element root = doc.getDefaultRootElement();
                if (lineNumber < 0 || lineNumber > root.getElementCount()) {
                    return "Invalid line number: " + lineNumber;
                }

                int offset = (lineNumber == root.getElementCount())
                        ? doc.getLength()
                        : root.getElement(lineNumber).getStartOffset();

                doc.insertString(offset, lineText + System.lineSeparator(), null);
                return "Inserted line at " + lineNumber;
            } catch (Exception e) {
                return "Line insert failed: " + e.getMessage();
            }
        }, true);
    }

    /**
     * Counts the number of lines in a file.
     *
     * @param path the file path relative to the project
     * @return the number of lines, or an error message
     */
    @Tool("Get the number of lines in a file by path")
    public String countLinesInFile(String path) {
        log("📄 Counting lines in", path);
        return FileUtil.withDocument(project, path, doc -> "File has "
                + doc.getDefaultRootElement().getElementCount() + " lines.", false);
    }

    /**
     * Creates a new file at the given path.
     *
     * @param path    the file path relative to the project
     * @param content optional content to write into the file
     * @return a status message
     */
    @Tool("Create a new file at the given path with optional content")
    public String createFile(String path, String content) {
        log("Creating", path);
        try {
            Path filePath = FileUtil.resolvePath(project, path);
            if (Files.exists(filePath)) {
                return "File already exists: " + path;
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content != null ? content : "");
            return "File created";
        } catch (IOException e) {
            return "File creation failed: " + e.getMessage();
        }
    }

    /**
     * Deletes a file.
     *
     * @param path the file path relative to the project
     * @return a status message
     */
    @Tool("Delete a file at the given path")
    public String deleteFile(String path) {
        log("🗑️ Deleting", path);
        try {
            Path filePath = FileUtil.resolvePath(project, path);
            if (!Files.exists(filePath)) {
                return "File not found: " + path;
            }

            Files.delete(filePath);
            return "File deleted";
        } catch (IOException e) {
            return "File delete failed: " + e.getMessage();
        }
    }

    /**
     * Lists all files and directories in a directory.
     *
     * @param path the directory path relative to the project
     * @return a list of files and directories, or an error message
     */
    @Tool("List all files and directories inside a given directory path")
    public String listFilesInDirectory(String path) {
        log("📂 Listing", path);
        try {
            Path dirPath = FileUtil.resolvePath(project, path);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return "Directory not found: " + path;
            }

            StringBuilder result = new StringBuilder(dirPath.getFileName() + ":\n");
            Files.list(dirPath).forEach(p -> {
                result.append(" - ").append(p.getFileName())
                        .append(Files.isDirectory(p) ? "/" : "")
                        .append("\n");
            });
            return result.toString();
        } catch (IOException e) {
            return "Could not list directory: " + e.getMessage();
        }
    }

    /**
     * Creates a new directory.
     *
     * @param path the directory path relative to the project
     * @return a status message
     */
    @Tool("Create a new directory at the given path")
    public String createDirectory(String path) {
        log("Creating", path);
        try {
            Path dirPath = FileUtil.resolvePath(project, path);
            if (Files.exists(dirPath)) {
                return "Directory already exists: " + path;
            }

            Files.createDirectories(dirPath);
            return "Directory created";
        } catch (IOException e) {
            return "Directory creation failed: " + e.getMessage();
        }
    }

    /**
     * Deletes a directory (must be empty).
     *
     * @param path the directory path relative to the project
     * @return a status message
     */
    @Tool("Delete a directory at the given path (must be empty)")
    public String deleteDirectory(String path) {
        log("🗑️ Deleting", path);
        try {
            Path dirPath = FileUtil.resolvePath(project, path);
            if (!Files.exists(dirPath)) {
                return "Directory not found: " + path;
            }
            if (!Files.isDirectory(dirPath)) {
                return "Not a directory: " + path;
            }

            Files.delete(dirPath);
            return "Directory deleted";
        } catch (IOException e) {
            return "Directory delete failed: " + e.getMessage();
        }
    }

    /**
     * Logs the current action to the {@link JeddictStreamHandler}, if
     * available.
     *
     * @param action the action being performed (e.g., "Reading", "Updating")
     * @param path the file path on which the action is performed
     */
    private void log(String action, String path) {
        if (handler != null && path != null) {
            String fileName = java.nio.file.Paths.get(path).getFileName().toString();
            handler.onToolingResponse(action + " " + fileName + "\n");
        }
    }
}


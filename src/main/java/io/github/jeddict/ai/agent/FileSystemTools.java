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
import dev.langchain4j.exception.ToolExecutionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Collection of tools that expose file system and editor operations inside
 * Apache NetBeans to an AI assistant.
 * <p>
 * These tools allow language models to read, modify, and manage project files
 * in a controlled and safe way.
 */
public class FileSystemTools extends AbstractCodeTool {

    public FileSystemTools(final String basedir) {
        super(basedir);
    }

    /**
     * Reads the raw content of a file on disk.
     *
     * @param path the file path relative to the project
     * @return the file content, or an error message if it could not be read
     */
    @Tool("Read the content of a file by path")
    public String readFile(final String path) throws Exception {
        progress("📖 Reading file " + path);

        //
        // If a path is absolute it must be within the project folder
        //
        if (path.startsWith(File.separator)) {
            Path absolutePath = Paths.get(path).toAbsolutePath().normalize();
            if (!absolutePath.startsWith(basepath)) {
                progress("❌ Trying to read a file outside the base path");
                throw new ToolExecutionException("trying to read a file outside the base path");
            }

        }
        try {
            final Path fullPath = fullPath(path);
            return Files.readString(fullPath, Charset.defaultCharset());
        } catch (IOException e) {
            progress("❌ Failed to read file " + e.getMessage());
            throw new ToolExecutionException("failed to read file " + e.getMessage());
        }
    }

    /**
     * Searches for a regular expression inside a file.
     *
     * @param path the file path relative to the project
     * @param pattern the regex pattern to search for
     * @return all matches with their offsets, or a message if none were found
     */
    @Tool("Search for a regex pattern in a file by path")
    public String searchInFile(String path, String pattern) throws Exception {
        progress("🔎 Looking for '" + pattern + "' inside '" + path + "'");
        String content = Files.readString(fullPath(path), Charset.defaultCharset());
        Matcher m = Pattern.compile(pattern).matcher(content);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            result.append("Match at ").append(m.start())
                  .append(": ").append(m.group()).append("\n");
        }
        return result.length() > 0 ? result.toString() : "No matches found";
    }

    /**
     * Replaces parts of a file using a literal string match instead of regex.
     * Escapes the literal string to a regex pattern internally.
     *
     * @param path the file path relative to the project
     * @param literalText the literal string to search and replace
     * @param replacement the replacement text
     * @return a status message
     */
    @Tool("Replace parts of a file content matching a literal string with replacement text. Special regex characters are escaped automatically")
    public String replaceSnippetByLiteral(String path, String literalText, String replacement)
            throws Exception {
        String escapedPattern = Pattern.quote(literalText);
        return replaceSnippetByRegex(path, escapedPattern, replacement);
    }

    /**
     * Replaces parts of a file content matching a regex pattern with
     * replacement text.
     *
     * @param path the file path relative to the project
     * @param regexPattern the regex pattern to search for
     * @param replacement the replacement text
     * @return a status message
     */
    @Tool("Replace parts of a file content matching a regex pattern with replacement text")
    public String replaceSnippetByRegex(String path, String regexPattern, String replacement)
            throws Exception {
        progress("🔄 Replacing text matching regex '" + regexPattern + "' in file: " + path);
        try {
            Path filePath = fullPath(path);
            String original = Files.readString(filePath);
            String modified = original.replaceAll(regexPattern, replacement);

            if (original.equals(modified)) {
                progress("⚠️ No matches found for regex '" + regexPattern + "' in file: " + path);
                return "No matches found for pattern.";
            }

            Files.writeString(filePath, modified, StandardOpenOption.TRUNCATE_EXISTING);
            progress("✅ Replacement completed in file: " + path);
            return "File snippet replaced successfully.";
        } catch (Exception e) {
            progress("❌ Replacement failed " + e.getMessage() + " in file: " + path);
            throw e;
        }
    }

    /**
     * Replaces the full content of a file with the given text.
     *
     * @param path the file path relative to the project
     * @param newContent the new content to write
     * @return a status message
     */
    @Tool("Replace the full content of a file by path with new text")
    public String replaceFileContent(String path, String newContent) throws Exception {
        progress("📝 Replacing entire content of file: " + path);
        try {
            Files.writeString(fullPath(path), newContent, StandardOpenOption.TRUNCATE_EXISTING);
            progress("✅ File content replaced successfully: " + path);
            return "File updated";
        } catch (Exception e) {
            progress("❌ Failed to replace content " + e.getMessage() + " in file: " + path);
            throw e;
        }
    }

    /**
     * Creates a new file at the given path.
     *
     * @param path the file path relative to the project
     * @param content optional content to write into the file
     * @return a status message
     */
    @Tool("Create a new file at the given path with optional content")
    public String createFile(String path, String content) throws Exception {
        progress("📄 Creating new file: " + path);
        try {
            Path filePath = fullPath(path);

            if (Files.exists(filePath)) {
                progress("⚠️ File already exists: " + path);
                return "File already exists: " + path;
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content != null ? content : "");

            progress("✅ File created successfully: " + path);
            return "File created";
        } catch (IOException e) {
            progress("❌ File creation failed: " + e.getMessage() + " in file: " + path);
            throw e;
        }
    }

    /**
     * Deletes a file.
     *
     * @param path the file path relative to the project
     * @return a status message
     */
    @Tool("Delete a file at the given path")
    public String deleteFile(String path) throws Exception {
        progress("🗑️ Attempting to delete file: " + path);
        try {
            Path filePath = fullPath(path);
            if (!Files.exists(filePath)) {
                progress("⚠️ File not found: " + path);
                return "File not found: " + path;
            }

            Files.delete(filePath);
            progress("✅ File deleted successfully: " + path);
            return "File deleted";
        } catch (IOException e) {
            progress("❌ File deletion failed: " + e.getMessage() + " in file: " + path);
            throw e;
        }
    }

    /**
     * Lists all files and directories in a directory.
     *
     * @param path the directory path relative to the project
     * @return a list of files and directories, or an error message
     */
    @Tool("""
        List all files and directories inside a given path. If the path does not
        exist or is not a directory, it returns a "directory not found" message.
        If path is an existing directory, it returns a list of pathnames,
        one on each line. If the path is a directory, the pathname will end with
        a slash ('/').
    """)
    public String listFilesInDirectory(String path) throws Exception {
        progress("📂 Listing contents of directory: " + path);
        Path dirPath = fullPath(path);

        if (!Files.isDirectory(dirPath)) {
            progress("⚠️ Directory not found: " + path);
            return "Directory not found: " + path;
        }

        String result = Files.list(dirPath)
                .map(p -> " - " + p.getFileName() + (Files.isDirectory(p) ? "/" : ""))
                .collect(Collectors.joining("\n"));

        return dirPath.getFileName() + ":\n" + result;
    }

    /**
     * Creates a new directory.
     *
     * @param path the directory path relative to the project
     * @return a status message
     */
    @Tool("Create a new directory at the given path")
    public String createDirectory(String path) throws Exception {
        progress("📂 Creating new directory: " + path);
        try {
            Path dirPath = fullPath(path);
            if (Files.exists(dirPath)) {
                progress("⚠️ Directory already exists: " + path);
                return "Directory already exists: " + path;
            }

            Files.createDirectories(dirPath);
            progress("✅ Directory created successfully: " + path);
            return "Directory created";
        } catch (IOException e) {
            progress("❌ Directory creation failed: " + e.getMessage() + " in " + path);
            throw e;
        }
    }

    /**
     * Deletes a directory (must be empty).
     *
     * @param path the directory path relative to the project
     * @return a status message
     */
    @Tool("Delete a directory at the given path (must be empty)")
    public String deleteDirectory(String path) throws Exception {
        progress("🗑️ Attempting to delete directory: " + path);
        try {
            Path dirPath = fullPath(path);
            if (!Files.exists(dirPath)) {
                progress("⚠️ Directory not found: " + path);
                return "Directory not found: " + path;
            }
            if (!Files.isDirectory(dirPath)) {
                progress("⚠️ Not a directory: " + path);
                return "Not a directory: " + path;
            }

            Files.delete(dirPath);
            progress("✅ Directory deleted successfully: " + path);
            return "Directory deleted";
        } catch (IOException e) {
            progress("❌ Directory deletion failed: " + e.getMessage() + " in " + path);
            throw e;
        }
    }
}

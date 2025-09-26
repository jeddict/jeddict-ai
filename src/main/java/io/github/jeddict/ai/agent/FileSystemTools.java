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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Element;
import org.apache.commons.io.file.PathUtils;

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
    public String readFile(String path) throws Exception {
        progress("📖 Reading file " + path);
        try {
            String content = PathUtils.readString(fullPath(path), Charset.defaultCharset());
            return content;
        } catch (IOException e) {
            progress("❌ Failed to read file: " + e.getMessage());
            throw e;
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
        String content = PathUtils.readString(Paths.get(basedir, path), Charset.defaultCharset());
        Matcher m = Pattern.compile(pattern).matcher(content);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            result.append("Match at ").append(m.start())
                    .append(": ").append(m.group()).append("\n");
        }
        return result.length() > 0 ? result.toString() : "No matches found";
    }

    /**
     * Find a line number to insert code after the end of a Java method or
     * constructor. This method heuristically scans the file content lines to
     * find the closing brace of the method. It accounts for Javadoc and nested
     * braces for accurate placement.
     *
     * @param fileContent the full Java source code as a String
     * @param methodName the method or constructor name to find
     * @return the line number after the method ends, or -1 if not found
     */
    private int findInsertionLineAfterMethod(String fileContent, String methodName) {
        String[] lines = fileContent.split("\r?\n");
        int methodStartLine = -1;
        int braceDepth = 0;
        boolean inMethod = false;

        Pattern methodPattern = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(.*\\)\\s*\\{\\s*$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!inMethod) {
                // Detect method declaration line
                if (methodPattern.matcher(line).find()) {
                    inMethod = true;
                    methodStartLine = i;
                    // Count opening brace
                    braceDepth = 1;
                }
            } else {
                // Inside method, track braces to find method end
                braceDepth += countChar(line, '{');
                braceDepth -= countChar(line, '}');
                if (braceDepth == 0) {
                    // Method ends here
                    return i + 1; // return line after method end
                }
            }
        }
        return -1;
    }

    /**
     * Inserts a line after the end of a specified Java method or constructor in
     * the file. Uses findInsertionLineAfterMethod to get a robust insertion
     * point.
     *
     * @param path the file path relative to the project
     * @param methodName the method or constructor name where to insert after
     * @param lineText the text to insert as a new line
     * @return status message
     */
    @Tool("Insert a line of code at a given line number (0-based) in a file by path")
    public String insertLineAfterMethod(String path, String methodName, String lineText)
    throws Exception {
        progress("✏️ Inserting line after method '" + methodName + "' in file: " + path);
        String content = withDocument(path, doc -> doc.getText(0, doc.getLength()), false);
        if (content.startsWith("Could not")) {
            progress("❌ Failed to read file: " + path);
            return "Failed to read file: " + content;
        }
        int insertLine = findInsertionLineAfterMethod(content, methodName);
        if (insertLine < 0) {
            progress("⚠️ Method not found: " + methodName + " in file " + path);
            return "Method not found: " + methodName;
        }
        progress("✅ Inserting text at line " + insertLine + " in file: " + path);
        return insertLineInFile(path, insertLine, lineText);
    }

    /**
     * Counts occurrences of a character in a string.
     *
     * @param line the string to search
     * @param ch the character to count
     * @return count of characters found
     */
    private int countChar(String line, char ch) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ch) {
                count++;
            }
        }
        return count;
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

        return withDocument(path, doc -> {
            try {
                String original = doc.getText(0, doc.getLength());
                String modified = original.replaceAll(regexPattern, replacement);

                if (original.equals(modified)) {
                    progress("⚠️ No matches found for regex '" + regexPattern + "' in file: " + path);
                    return "No matches found for pattern.";
                }

                doc.remove(0, doc.getLength());
                doc.insertString(0, modified, null);

                progress("✅ Replacement completed in file: " + path);
                return "File snippet replaced successfully.";
            } catch (Exception e) {
                progress("❌ Replacement failed " + e.getMessage() + " in file: " + path);
                throw e;
            }
        }, true);
    }

    /**
     * Replaces the full content of a file with the given text.
     *
     * @param path the file path relative to the project
     * @param newContent the new content to write
     * @return a status message
     */
    @Tool("Replace the full content of a file by path with new text")
    public String replaceFileContent(String path, String newContent)
    throws Exception {
        progress("📝 Replacing entire content of file: " + path);

        return withDocument(path, doc -> {
            try {
                doc.remove(0, doc.getLength());
                doc.insertString(0, newContent, null);

                progress("✅ File content replaced successfully: " + path);
                return "File updated";
            } catch (Exception e) {
                progress("❌ Failed to replace content " + e.getMessage() + " in file: " + path);
                throw e;
            }
        }, true);
    }

    /**
     * Inserts a line at the given line number (0-based).
     *
     * @param path the file path relative to the project
     * @param lineNumber the line number (0-based)
     * @param lineText the text of the new line
     * @return a status message
     */
    @Tool("Insert a line of code at a given line number (0-based) in a file by path")
    public String insertLineInFile(String path, int lineNumber, String lineText)
    throws Exception {
        progress("✏️ Inserting line at " + lineNumber + " in file: " + path);

        return withDocument(path, doc -> {
            try {
                Element root = doc.getDefaultRootElement();
                if (lineNumber < 0 || lineNumber > root.getElementCount()) {
                    progress("⚠️ Invalid line number " + lineNumber + " for file: " + path);
                    return "Invalid line number: " + lineNumber;
                }

                int offset = (lineNumber == root.getElementCount())
                        ? doc.getLength()
                        : root.getElement(lineNumber).getStartOffset();

                doc.insertString(offset, lineText + System.lineSeparator(), null);

                progress("✅ Inserted line at " + lineNumber + " in file: " + path);
                return "Inserted line at " + lineNumber;
            } catch (Exception e) {
                progress("❌ Line insert failed: " + e.getMessage() + " in file: " + path);
                throw e;
            }
        }, true);
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
            Path filePath = Paths.get(basedir, path);
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
    @Tool("List all files and directories inside a given directory path")
    public String listFilesInDirectory(String path) throws Exception {
        progress("📂 Listing contents of directory: " + path);
        try {
            Path dirPath = fullPath(path);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                progress("⚠️ Directory not found: " + path);
                return "Directory not found: " + path;
            }

            StringBuilder result = new StringBuilder(dirPath.getFileName() + ":\n");
            Files.list(dirPath).forEach(p -> {
                result.append(" - ").append(p.getFileName())
                        .append(Files.isDirectory(p) ? "/" : "")
                        .append("\n");
            });

            progress("✅ Directory listed successfully: " + path);
            return result.toString();
        } catch (IOException e) {
            progress("❌ Failed to list directory: " + e.getMessage() + " in " + path);
            throw e;
        }
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
    public String deleteDirectory(String path)
    throws Exception {
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
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
import io.github.jeddict.ai.settings.PreferencesManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import org.apache.commons.lang3.StringUtils;

/**
 * Collection of tools that expose file system and editor operations inside
 * Apache NetBeans to an AI assistant.
 * <p>
 * These tools allow language models to read, modify, and manage project files
 * in a controlled and safe way.
 */
public class FileSystemTools extends AbstractCodeTool {

    public FileSystemTools(final String basedir) throws IOException {
        super(basedir);
    }

    /**
     * Returns the full file tree of the project (or a sub-directory) as a
     * formatted, indented string. Files and directories excluded by
     * {@code PreferencesManager} (exclude-dirs and allowed extensions) are
     * automatically filtered out.
     *
     * @param path  sub-directory path relative to the project root to use as
     *              the tree root (e.g. {@code "src/main/java"}); leave blank
     *              to show the full project tree
     * @param depth maximum number of directory levels to descend;
     *              {@code 0} means unlimited depth
     * @return indented file tree string
     */
    @Tool(
        name = "fileTree",
        value = "Return the file tree structure of the project or a sub-directory. "
            + "Use 'path' to restrict the tree to a sub-directory (e.g. 'src/main/java'); "
            + "leave blank to show the full project. "
            + "Use 'depth' to limit traversal depth (e.g. 3); 0 means unlimited."
    )
    @ToolPolicy(READONLY)
    public String fileTree(String path, int depth) {
        progress("📂 Gathering file tree" + (path != null && !path.isBlank() ? " for " + path : ""));
        return getFileTree(basepath, path, depth);
    }

    /**
     * Returns the directory hierarchy of the project as a compact, indented
     * string. Only directories are listed — individual files are omitted —
     * giving a quick overview of the package/module structure. Excluded
     * directories (as configured in {@code PreferencesManager}) are filtered
     * out automatically.
     *
     * @return indented directory tree string
     */
    @Tool(
        name = "dirTree",
        value = "Return the directory hierarchy of the project, showing only the folder "
            + "structure without individual files"
    )
    @ToolPolicy(READONLY)
    public String dirTree() {
        progress("📂 Gathering directory tree");
        return getDirTree(basepath);
    }

    /**
     *
     * @param path the file path relative to the project
     * @return the file content, or an error message if it could not be read
     */
    @Tool("Read the content of a file by path")
    @ToolPolicy(READONLY)
    public String readFile(final String path) throws ToolExecutionException {
        progress("📖 Reading file " + path);

        checkPath(path);

        try {
            final Path fullPath = fullPath(path);
            return Files.readString(fullPath, Charset.defaultCharset());
        } catch (IOException e) {
            progress("❌ Failed to read file: " + e);
            throw new ToolExecutionException("failed to read file: " + e);
        }
    }

    /**
     * Searches for files in the file system given a directory and a regex pattern.
     * The tool scans all folders and subfolders of the given directory.
     *
     * @param path the directory path relative to the project to start the search from
     * @param regexPattern the regex pattern to match against the absolute path of the files
     * @return a list of matching file paths, or an empty string if none were found
     */
    @Tool("""
    Recursively find files in a given root folder whose file name matches a regex
    pattern. The pattern is matched against the full pathname. It returns a
    newline-separated list of relative pathnames, or an empty string if no matches
    are found. It returns an error message starting with "ERR:" if the starting
    path does not exist. If the pattern is empty, it matches all files.
    """)
    @ToolPolicy(READONLY)
    public String findFiles(String path, String regexPattern) throws Exception {
        progress("🔎 Searching for files matching '" + regexPattern + "' in directory '" + path + "'");
        Path startDir = fullPath(path);

        if (!Files.exists(startDir) || !Files.isDirectory(startDir)) {
             progress("❌ invalid directory: " + path);
             return "ERR: invalid directory " + path;
        }

        final Pattern pattern = ((regexPattern == null) || regexPattern.isBlank())
                              ? null : Pattern.compile(regexPattern);

        try (Stream<Path> stream = Files.walk(startDir)) {
            Stream<Path> fileStream = stream.filter(p -> !Files.isDirectory(p));

            if (pattern != null) {
                fileStream = fileStream.filter(p -> pattern.matcher(p.toAbsolutePath().toString()).find());
            }

            List<String> matches = fileStream
                    .map(p -> basepath.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());

            if (matches.isEmpty()) {
                progress("⚠️ No matches found for '" + regexPattern + "' in: " + path);
                //
                // TODO: back to return "" once https://github.com/langchain4j/langchain4j/issues/4300
                //       will be fixed
                //
                return "No matches found for " + regexPattern;
            }

            String result = String.join("\n", matches);
            progress("✅ Found " + matches.size() + " matching files.");
            return result;
        } catch (IOException e) {
            progress("❌ Error searching files: " + e.getMessage());
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
    @ToolPolicy(READONLY)
    public String searchInFile(String path, String pattern) throws ToolExecutionException {
        progress("🔎 Looking for '" + pattern + "' inside '" + path + "'");

        checkPath(path);

        try {
        String content = Files.readString(fullPath(path), Charset.defaultCharset());
        Matcher m = Pattern.compile(pattern).matcher(content);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            result.append("Match at ").append(m.start())
                  .append(": ").append(m.group()).append("\n");
        }
        return result.length() > 0 ? result.toString() : "No matches found";
        } catch (IOException x) {
            throw new ToolExecutionException(x);
        }
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
    @Tool(
    """
    Replace parts of a file content matching a literal string with replacement text
    with no user interaction. Special regex characters are escaped.
    """)
    @ToolPolicy(READWRITE)
    public String replaceSnippetByLiteral(String path, String literalText, String replacement)
            throws Exception {
        return replaceSnippetByRegex(path, Pattern.quote(literalText), replacement);
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
    @Tool("Replace parts of a file content matching a regex pattern with replacement text  with no user interaction")
    @ToolPolicy(READWRITE)
    public String replaceSnippetByRegex(
        final String path, final String regexPattern, final String replacement
    )
    throws ToolExecutionException {
        progress("🔄 Replacing text matching regex '" + regexPattern + "' in " + path);

        checkPath(path);

        try {
            final Path filePath = fullPath(path).toRealPath();

            String original = Files.readString(filePath);
            String modified = original.replaceAll(regexPattern, replacement);

            if (original.equals(modified)) {
                progress("❌ No matches found for regex '" + regexPattern + "' in " + path);
                return "No matches found for pattern";
            }

            Files.writeString(filePath, modified, StandardOpenOption.TRUNCATE_EXISTING);
            progress("✅ Snippet replaced");
            return "Snippet replaced";
        } catch (IOException e) {
            progress("❌ Replacement failed: " + e);
            throw new ToolExecutionException("replacement failed: " + e);
        }
    }

    /**
     * Replaces the full content of a file with the given text.
     *
     * @param path the file path relative to the project
     * @param newContent the new content to write
     * @return a status message
     */
    @Tool("Replace the full content of a file by path with new text with no user interaction")
    @ToolPolicy(READWRITE)
    public String replaceFileContent(final String path, final String newContent)
    throws ToolExecutionException {
        progress("🔄 Replacing content in " + path);

        checkPath(path);

        try {
            Files.writeString(fullPath(path), newContent, StandardOpenOption.TRUNCATE_EXISTING);
            progress("✅ File content replaced");
            return "File updated";
        } catch (IOException e) {
            progress("❌ Replacement failed: " + e);
            throw new ToolExecutionException("replacement failed: " + e);
        }
    }

    /**
     * Creates a new file at the given path.
     *
     * @param path the file path relative to the project
     * @param content optional content to write into the file
     * @return a status message
     */
    @Tool("Create a new file at the given path with optional content with no user interaction")
    @ToolPolicy(READWRITE)
    public String createFile(String path, String content) throws ToolExecutionException {
        progress("📄 Creating file " + path);

        checkPath(path);

        try {
            final Path filePath = fullPath(path);

            if (Files.exists(filePath)) {
                progress("❌ " + path + " already exists");
                throw new ToolExecutionException("❌ " + path + " already exists");
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content != null ? content : "");

            progress("✅ File created: " + path);
            return "File created";
        } catch (IOException e) {
            progress("❌ File creation failed: " + e.getMessage() + " in " + path);
            throw new ToolExecutionException(e);
        }
    }

    /**
     * Deletes a file.
     *
     * @param path the file path relative to the project
     * @return a status message
     */
    @Tool("Delete a file at the given path")
    @ToolPolicy(READWRITE)
    public String deleteFile(String path) throws ToolExecutionException {
        progress("🗑️ Deleting file " + path);

        checkPath(path);

        try {
            final Path filePath = fullPath(path);
            if (!Files.exists(filePath)) {
                progress("❌ " + path + " does not exist");
                throw new ToolExecutionException(path + " does not exist");
            }

            Files.delete(filePath);
            progress("✅ " + path + " deleted");
            return "File deleted";
        } catch (IOException e) {
            progress("❌ File deletion failed: " + e.getMessage() + " in " + path);
            throw new ToolExecutionException(e);
        }
    }

    /**
     * Lists all files and directories in a directory.
     *
     * @param path the directory path relative to the project
     * @return a list of files and directories, or an error message
     */
    @Tool(
        """
        List all files and directories inside a given path one on each line.
        If an element of the list is a directory, the pathname will end with
        a slash ('/')
        If the path does not exist or is not a directory, it returns
        "<directory> does not exist".
        If the directory is empty (empty) is returned.
        """
    )
    @ToolPolicy(READONLY)
    public String listFilesInDirectory(final String path) throws ToolExecutionException {
        progress("📂 Listing content of directory " + path);

        checkPath(path);

        final Path dirPath = fullPath(path);

        if (!Files.isDirectory(dirPath)) {
            progress("❌ " + path + " does not exist");
            throw new ToolExecutionException(path + " does not exist");
        }

        //
        // currently langchain4j does not allow a tool to return an empty or null ù
        // result. See https://github.com/langchain4j/langchain4j/issues/4300
        // Until the fix it, let's return (empty)
        //
        try {
            return StringUtils.defaultIfBlank(Files.list(dirPath)
                .map(p -> " - " + p.getFileName() + (Files.isDirectory(p) ? "/" : ""))
                .collect(Collectors.joining("\n")), "(empty)");
        } catch (IOException e) {
            progress("❌ error listing " + path);
            throw new ToolExecutionException(e);
        }
    }

    /**
     * Creates a new directory.
     *
     * @param path the directory path relative to the project
     *
     * @return a status message
     */
    @Tool("Create a new directory at the given path")
    @ToolPolicy(READWRITE)
    public String createDirectory(String path) throws ToolExecutionException {
        progress("📂 Creating new directory " + path);

        checkPath(path);

        try {
            final Path dirPath = fullPath(path);
            if (Files.exists(dirPath)) {
                progress("❌ " + path + " already exists");
                throw new ToolExecutionException("❌ " + path + " already exists");
            }

            Files.createDirectories(dirPath);

            progress("✅ Directory created");
            return "Directory created";
        } catch (IOException e) {
            progress("❌ Directory creation failed: " + e.getMessage() + " in " + path);
            throw new ToolExecutionException(e);
        }
    }

    /**
     * Deletes a directory (must be empty).
     *
     * @param path the directory path relative to the project
     * @return a status message
     */
    @Tool("Delete a directory at the given path (must be empty)")
    @ToolPolicy(READWRITE)
    public String deleteDirectory(final String path) throws ToolExecutionException {
        progress("🗑️ Deleting directory " + path);

        checkPath(path);

        try {
            final Path dirPath = fullPath(path);
            if (!Files.exists(dirPath)) {
                progress("❌ " + path + " not found");
                throw new ToolExecutionException("❌ " + path + " not found");
            }
            if (!Files.isDirectory(dirPath)) {
                progress("❌ " + path + " not a directory");
                throw new ToolExecutionException("❌ " + path + " not a directory");
            }

            Files.delete(dirPath);
            progress("✅ " + path + " deleted");

            return "Directory deleted";
        } catch (IOException e) {
            progress("❌ Directory deletion failed: " + e.getMessage() + " in " + path);
            throw new ToolExecutionException(e);
        }
    }

    // --------------------------------------------------------- static tree builders

    /**
     * Returns the file tree structure rooted at the given base directory,
     * limited to the specified depth. Directories configured in
     * {@link PreferencesManager#getExcludeDirs()}, hidden entries, and files
     * whose extension is not in
     * {@link PreferencesManager#getFileExtensionListToInclude()} are excluded.
     *
     * @param projectRoot the absolute path of the project root (used as the
     *                    security boundary for path-traversal checks)
     * @param subPath     a path relative to {@code projectRoot} to use as the
     *                    tree root; {@code null} or blank means the whole tree
     * @param maxDepth    the maximum number of directory levels to descend;
     *                    values {@code <= 0} mean unlimited depth
     * @return the file tree as an indented string, or an empty string when the
     *         tree cannot be read
     */
    public static String getFileTree(Path projectRoot, String subPath, int maxDepth) {
        if (projectRoot == null) {
            return "";
        }
        try {
            final Path root;
            if (subPath != null && !subPath.isBlank()) {
                root = projectRoot.resolve(subPath).normalize();
                if (!root.startsWith(projectRoot)) {
                    // Prevent path traversal outside the project
                    return "Path is outside the project directory: " + subPath;
                }
                if (!Files.isDirectory(root)) {
                    return "Not a directory: " + subPath;
                }
            } else {
                root = projectRoot;
            }
            final Set<String> allowedExtensions = new HashSet<>(
                    PreferencesManager.getInstance().getFileExtensionListToInclude());
            final StringBuilder sb = new StringBuilder();
            final Stream<Path> stream = (maxDepth > 0)
                    ? Files.walk(root, maxDepth)
                    : Files.walk(root);
            try (stream) {
                stream
                    .filter(path -> !isExcluded(projectRoot, path))
                    .filter(path -> Files.isDirectory(path)
                            || allowedExtensions.contains(getExtension(path)))
                    .sorted()
                    .forEach(path -> {
                        if (path.equals(root)) {
                            return;
                        }
                        final Path relative = root.relativize(path);
                        final int depth = relative.getNameCount() - 1;
                        sb.append("  ".repeat(depth))
                          .append(path.getFileName())
                          .append(Files.isDirectory(path) ? "/" : "")
                          .append('\n');
                    });
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Returns the directory hierarchy rooted at the given base path as a
     * formatted string. Only directories are included — individual files and
     * classes are omitted — giving a compact overview of the package structure.
     * Hidden directories and directories configured in
     * {@link PreferencesManager#getExcludeDirs()} are excluded.
     *
     * @param root the absolute path to use as the tree root
     * @return the directory hierarchy as an indented string, or an empty
     *         string if the root is null or the tree cannot be read
     */
    public static String getDirTree(Path root) {
        if (root == null) {
            return "";
        }
        try {
            final StringBuilder sb = new StringBuilder();
            try (Stream<Path> stream = Files.walk(root)) {
                stream
                    .filter(Files::isDirectory)
                    .filter(path -> !isExcluded(root, path))
                    .sorted()
                    .forEach(path -> {
                        if (path.equals(root)) {
                            return;
                        }
                        final Path relative = root.relativize(path);
                        final int depth = relative.getNameCount() - 1;
                        sb.append("  ".repeat(depth))
                          .append(path.getFileName())
                          .append("/")
                          .append('\n');
                    });
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    /** Returns the file extension (without the dot) for the given path, or an empty string. */
    private static String getExtension(Path path) {
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    /**
     * Returns {@code true} when the given {@code path} should be excluded from
     * the file/directory tree.
     *
     * <p>A path is excluded when any of its components starts with {@code .}
     * (hidden files) <em>or</em> when its relative representation starts with
     * any of the directory/file prefixes returned by
     * {@link PreferencesManager#getExcludeDirs()}.  The comparison uses
     * forward slashes so that it is consistent with the Unix-style paths
     * stored in the settings regardless of the host OS.</p>
     */
    private static boolean isExcluded(final Path root, final Path path) {
        if (path.equals(root)) {
            return false;
        }
        // Always exclude hidden entries (any path component starting with '.')
        for (final Path component : root.relativize(path)) {
            if (component.toString().startsWith(".")) {
                return true;
            }
        }
        // Apply the user-configured exclude list (prefix match on the
        // forward-slash relative path, same logic as ProjectUtil.collectFiles)
        final String relativePath = root.relativize(path).toString()
                .replace(File.separatorChar, '/');
        return PreferencesManager.getInstance().getExcludeDirs().stream()
                .filter(s -> !s.isBlank())
                .anyMatch(relativePath::startsWith);
    }
}

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
package io.github.jeddict.ai.scanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import io.github.jeddict.ai.settings.PreferencesManager;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author Gaurav Gupta
 *
 * TODO: add more info about the project like:
 *       - base directory
 *       - build system (maven, ant, gradle)
 *       - project name
 */
public class ProjectMetadataInfo {

    private static final Logger LOG = Logger.getLogger(ProjectMetadataInfo.class.getName());

    private static final Map<Project, CachedResult> cache = new HashMap<>();

    public static String get(Project project) {
        if (project == null) {
            return "";
        }

        final CachedResult cachedResult = getCachedResult(project);

        // Check if cachedResult is null and handle accordingly
        if (cachedResult == null) {
            return "Project Metadata: Unable to retrieve metadata for the specified project.";
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("- name: ").append(StringUtils.defaultString(cachedResult.name)).append('\n')
          .append("- folder: ").append(StringUtils.defaultString(cachedResult.folder)).append('\n')
          .append("- type: ").append(StringUtils.defaultString(cachedResult.type)).append('\n')
        ;

        // Append EE Version with appropriate label if importPrefix is "jakarta
        if (cachedResult.eeVersion() != null) {
            if ("- jakarta".equals(cachedResult.importPrefix())) {
                sb.append("- EE Version: ").append(cachedResult.eeVersion()).append("\n");
            } else {
                sb.append("- EE Version: ").append(cachedResult.eeVersion()).append("\n");
            }
        }
        if (cachedResult.importPrefix() != null) {
            sb.append("- EE Import Prefix: ").append(cachedResult.importPrefix()).append("\n");
        }
        if (cachedResult.jdkVersion() != null) {
            sb.append("- Java Version: ").append(cachedResult.jdkVersion()).append("\n");
        }

        final String srcDir = getSrcDir(project);
        if (!srcDir.isBlank()) {
            sb.append("- Source Directory: ").append(srcDir).append("\n");
        }

        final String testDir = getTestDir(project);
        if (!testDir.isBlank()) {
            sb.append("- Test Source Directory: ").append(testDir).append("\n");
        }

        return sb.toString().trim();
    }

    public static CachedResult getCachedResult(Project project) {
        try {
            // Check if the project is cached
            CachedResult cachedResult = cache.get(project);

            final FileObject projectDirectory = project.getProjectDirectory();
            final String folder = FilenameUtils.separatorsToSystem(projectDirectory.getPath());

            // Use the build file modification time for cache invalidation; fall
            // back to the project directory mtime for non-standard projects.
            final FileObject buildFile = firstPresent(projectDirectory,
                    "pom.xml", "build.gradle", "build.gradle.kts", "build.xml");
            final long lastModified = buildFile != null
                    ? new File(buildFile.getPath()).lastModified()
                    : new File(folder).lastModified();

            if (cachedResult != null && cachedResult.timestamp >= lastModified) {
                return cachedResult;
            }

            // Project name: read from the build file when available so we don't
            // depend on the NB global Lookup (which is not initialized in unit tests).
            // For Maven projects, use the <name> element (or artifactId as fallback).
            // For other project types, use the project directory name as a sensible default.
            String name = projectDirectory.getName();

            // Detect build system from well-known build file names
            String type = detectProjectType(projectDirectory);

            String eeVersion = null;
            String jdkVersion = null;
            String importPrefix = null;

            // Parse pom.xml directly (MavenXpp3Reader, no NbMavenProject)
            if (buildFile != null && "pom.xml".equals(buildFile.getNameExt())) {
                final MavenXpp3Reader reader = new MavenXpp3Reader();
                try (final InputStream in = buildFile.getInputStream()) {
                    final Model model = reader.read(in);
                    if (model.getName() != null && !model.getName().isBlank()) {
                        name = model.getName();
                    }
                    eeVersion = getEEVersionFromDependencies(model.getDependencies());
                    jdkVersion = getJdkVersionFromModel(model);
                    if (eeVersion != null) {
                        if (eeVersion.startsWith("jakarta")) {
                            importPrefix = eeVersion.equals("jakarta-8.0.0") ? "javax" : "jakarta";
                        } else if (eeVersion.startsWith("javax")) {
                            importPrefix = "javax";
                        }
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Failed to parse pom.xml for project metadata in: " + folder, ex);
                }
            }

            // Cache the result
            final CachedResult result = new CachedResult(
                name, folder, type,
                importPrefix, eeVersion, jdkVersion, lastModified
            );
            cache.put(project, result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Returns the first of the given filenames that exists under {@code dir}. */
    private static FileObject firstPresent(FileObject dir, String... names) {
        for (final String name : names) {
            final FileObject fo = dir.getFileObject(name);
            if (fo != null) {
                return fo;
            }
        }
        return null;
    }

    /** Detects the build system from well-known build files in the project directory. */
    private static String detectProjectType(FileObject dir) {
        if (dir.getFileObject("pom.xml") != null) return "maven";
        if (dir.getFileObject("build.gradle") != null
                || dir.getFileObject("build.gradle.kts") != null) return "gradle";
        if (dir.getFileObject("build.xml") != null) return "ant";
        return null;
    }

    private static String getEEVersionFromDependencies(List<org.apache.maven.model.Dependency> dependencies) {
        // Look for Jakarta EE or Java EE dependencies
        for (org.apache.maven.model.Dependency dependency : dependencies) {
            if (dependency.getGroupId().equals("jakarta.platform")) {
                if (dependency.getVersion() != null && dependency.getVersion().startsWith("8.0")) {
                    return "jakarta-8.0.0"; // Special case for Jakarta EE 8
                }
                return "jakarta"; // Other versions of Jakarta EE
            }
            if (dependency.getGroupId().startsWith("javax.")) {
                return "javax"; // Java EE dependencies
            }
        }
        return null; // Return null if no matching dependencies are found
    }

    private static String getJdkVersionFromModel(Model model) {
        // Check for JDK version in Maven properties
        String source = model.getProperties().getProperty("maven.compiler.source");
        String target = model.getProperties().getProperty("maven.compiler.target");

        // Return source version if available; fallback to target
        if (source != null) {
            return source;
        }
        if (target != null) {
            return target;
        }
        if (model.getBuild() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {
                if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    Object configuration = plugin.getConfiguration();
                    if (configuration instanceof Xpp3Dom dom) {
                        Xpp3Dom sourceNode = dom.getChild("source");
                        if (sourceNode != null) {
                            return sourceNode.getValue();
                        }
                    }
                }
            }
        }

        // Return null if no JDK version is found
        return null;
    }

    /**
     * Returns the main Java sources directory path relative to the project
     * root (e.g. {@code src/main/java}).
     *
     * <p>Uses the NetBeans {@link Sources} API, which is project-type-agnostic
     * and works for Maven, Gradle, Ant, and any other project that registers
     * Java source groups. Returns an empty string when no non-test Java source
     * root is registered or the project is {@code null}.</p>
     *
     * @param project the project to query
     * @return the relative path, or an empty string if not determinable
     */
    public static String getSrcDir(Project project) {
        if (project == null) {
            return "";
        }
        final SourceGroup[] groups = ProjectUtils.getSources(project)
                .getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        final Set<FileObject> testRoots = findTestRoots(groups);
        for (final SourceGroup sg : groups) {
            if (!testRoots.contains(sg.getRootFolder())) {
                return relativize(project, sg.getRootFolder().getPath());
            }
        }
        return "";
    }

    /**
     * Returns the main resources directory path relative to the project root
     * (e.g. {@code src/main/resources}).
     *
     * <p>Uses the NetBeans {@link Sources} API with the {@code "resources"}
     * source group type. Falls back to deriving the path from the main Java
     * source directory by convention (replacing the {@code java} segment with
     * {@code resources}) when no resource source group is registered.</p>
     *
     * @param project the project to query
     * @return the relative path, or an empty string if not determinable
     */
    public static String getSrcResourceDir(Project project) {
        if (project == null) {
            return "";
        }
        final SourceGroup[] groups = ProjectUtils.getSources(project)
                .getSourceGroups("resources");
        if (groups.length > 0) {
            final Set<FileObject> testRoots = findTestRoots(groups);
            for (final SourceGroup sg : groups) {
                if (!testRoots.contains(sg.getRootFolder())) {
                    return relativize(project, sg.getRootFolder().getPath());
                }
            }
        }
        // Fallback: derive from the main Java source directory by convention
        final String srcJavaDir = getSrcDir(project);
        if (!srcJavaDir.isBlank()) {
            final String candidate = replaceJavaSegment(srcJavaDir, "resources");
            if (candidate != null) {
                final Path resourcePath = Paths.get(project.getProjectDirectory().getPath())
                        .resolve(candidate);
                if (Files.isDirectory(resourcePath)) {
                    return candidate;
                }
            }
        }
        return "";
    }

    /**
     * Returns the test Java sources directory path relative to the project
     * root (e.g. {@code src/test/java}).
     *
     * <p>Uses the NetBeans {@link Sources} API, which is project-type-agnostic
     * and works for Maven, Gradle, Ant, and any other project that registers
     * Java source groups. Returns an empty string when no test Java source
     * root is registered or the project is {@code null}.</p>
     *
     * @param project the project to query
     * @return the relative path, or an empty string if not determinable
     */
    public static String getTestDir(Project project) {
        if (project == null) {
            return "";
        }
        final SourceGroup[] groups = ProjectUtils.getSources(project)
                .getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        final Set<FileObject> testRoots = findTestRoots(groups);
        for (final SourceGroup sg : groups) {
            if (testRoots.contains(sg.getRootFolder())) {
                return relativize(project, sg.getRootFolder().getPath());
            }
        }
        return "";
    }

    /**
     * Returns the test resources directory path relative to the project root
     * (e.g. {@code src/test/resources}).
     *
     * <p>Uses the NetBeans {@link Sources} API with the {@code "resources"}
     * source group type. Falls back to deriving the path from the test Java
     * source directory by convention (replacing the {@code java} segment with
     * {@code resources}) when no test resource source group is registered.</p>
     *
     * @param project the project to query
     * @return the relative path, or an empty string if not determinable
     */
    public static String getTestResourceDir(Project project) {
        if (project == null) {
            return "";
        }
        final SourceGroup[] groups = ProjectUtils.getSources(project)
                .getSourceGroups("resources");
        if (groups.length > 0) {
            final Set<FileObject> testRoots = findTestRoots(groups);
            for (final SourceGroup sg : groups) {
                if (testRoots.contains(sg.getRootFolder())) {
                    return relativize(project, sg.getRootFolder().getPath());
                }
            }
        }
        // Fallback: derive from the test Java source directory by convention
        final String testJavaDir = getTestDir(project);
        if (!testJavaDir.isBlank()) {
            final String candidate = replaceJavaSegment(testJavaDir, "resources");
            if (candidate != null) {
                final Path resourcePath = Paths.get(project.getProjectDirectory().getPath())
                        .resolve(candidate);
                if (Files.isDirectory(resourcePath)) {
                    return candidate;
                }
            }
        }
        return "";
    }

    // Matches path segments that represent test source roots: "test", "tests",
    // "test-java", "test-resources" etc. — but not unrelated words like "latest".
    private static final Pattern TEST_SEGMENT_PATTERN =
            Pattern.compile("^tests?(?:[^a-zA-Z].*)?$", Pattern.CASE_INSENSITIVE);

    /**
     * Identifies which roots among {@code groups} are test roots.
     *
     * <p>First tries {@link UnitTestForSourceQuery}, which is the canonical
     * NB API. If that query is unavailable (e.g. because the NB module system
     * is not running in the current test environment), falls back to a
     * path-based heuristic: the common ancestor of all source roots is found,
     * and any root whose path relative to that ancestor begins with a segment
     * that contains {@code "test"} (case-insensitive) is treated as a test
     * root. Using the common ancestor avoids false positives caused by
     * {@code "test"} appearing in parent directories of the project.</p>
     */
    private static Set<FileObject> findTestRoots(final SourceGroup[] groups) {
        final Map<FileObject, Boolean> folderIndex = new HashMap<>();
        for (final SourceGroup sg : groups) {
            folderIndex.put(sg.getRootFolder(), Boolean.FALSE);
        }
        final Set<FileObject> testRoots = new HashSet<>();
        boolean queryAvailable = true;
        for (final SourceGroup sg : groups) {
            if (!queryAvailable) {
                break;
            }
            try {
                for (final URL url : UnitTestForSourceQuery.findUnitTests(sg.getRootFolder())) {
                    final FileObject fo = URLMapper.findFileObject(url);
                    if (fo != null && folderIndex.containsKey(fo)) {
                        testRoots.add(fo);
                    }
                }
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                // NB module system not fully initialized (e.g. in unit tests).
                // Fall back to common-ancestor path detection below.
                LOG.log(Level.FINE, "UnitTestForSourceQuery unavailable; using path-based test-root detection", e);
                queryAvailable = false;
            }
        }
        if (!queryAvailable && testRoots.isEmpty() && !folderIndex.isEmpty()) {
            // Find the common ancestor of all source roots so we can compute
            // paths relative to the project, avoiding false "/test/" hits in
            // parent directories of the project itself.
            Path commonAncestor = null;
            for (final FileObject fo : folderIndex.keySet()) {
                final Path p = Paths.get(fo.getPath());
                if (commonAncestor == null) {
                    commonAncestor = p.getParent();
                } else {
                    while (commonAncestor != null && !p.startsWith(commonAncestor)) {
                        commonAncestor = commonAncestor.getParent();
                    }
                }
            }
            if (commonAncestor != null) {
                for (final FileObject fo : folderIndex.keySet()) {
                    final Path relative = commonAncestor.relativize(Paths.get(fo.getPath()));
                    if (relative.getNameCount() > 0
                            && TEST_SEGMENT_PATTERN.matcher(relative.getName(0).toString()).matches()) {
                        testRoots.add(fo);
                    }
                }
            }
        }
        return testRoots;
    }

    /**
     * Replaces the last path segment that is exactly {@code "java"} with
     * {@code replacement}. Returns {@code null} when no such segment exists,
     * preventing unintended substring substitutions (e.g. in
     * {@code "javascript/main/java"}).
     */
    private static String replaceJavaSegment(String relPath, String replacement) {
        final String sep = relPath.contains("/") ? "/" : File.separator;
        final String[] parts = relPath.split(Pattern.quote(sep), -1);
        for (int i = parts.length - 1; i >= 0; i--) {
            if ("java".equals(parts[i])) {
                parts[i] = replacement;
                return String.join(sep, parts);
            }
        }
        return null;
    }

    private static String relativize(Project project, String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return "";
        }
        try {
            final Path projectRoot = Paths.get(project.getProjectDirectory().getPath());
            final Path absolute = Paths.get(absolutePath);
            if (absolute.startsWith(projectRoot)) {
                return projectRoot.relativize(absolute).toString();
            }
            return absolutePath;
        } catch (final IllegalArgumentException e) {
            return absolutePath;
        }
    }

    /**
     * Returns the file tree structure of the project directory as a formatted
     * string. Directories configured in {@link PreferencesManager#getExcludeDirs()},
     * hidden entries (those whose path component starts with {@code .}), and
     * files whose extension is not in {@link PreferencesManager#getFileExtensionListToInclude()}
     * are excluded.
     *
     * @param project the project whose file tree to return
     * @return the file tree as an indented string, or an empty string if the
     *         project is null or the tree cannot be read
     */
    public static String getFileTree(Project project) {
        if (project == null) {
            return "";
        }
        try {
            final Path root = Paths.get(project.getProjectDirectory().getPath());
            final Set<String> allowedExtensions = new HashSet<>(
                    PreferencesManager.getInstance().getFileExtensionListToInclude());
            final StringBuilder sb = new StringBuilder();
            try (Stream<Path> stream = Files.walk(root)) {
                stream
                    .filter(path -> !isExcluded(root, path))
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

    /** Returns the file extension (without the dot) for the given path, or an empty string. */
    private static String getExtension(Path path) {
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    /**
     * Returns the minimal directory hierarchy of the project as a formatted
     * string. Only directories are included — individual files and classes are
     * omitted — giving a compact overview of the package structure.
     * Hidden directories and directories configured in
     * {@link PreferencesManager#getExcludeDirs()} are excluded.
     *
     * @param project the project whose directory hierarchy to return
     * @return the directory hierarchy as an indented string, or an empty
     *         string if the project is null or the tree cannot be read
     */
    public static String getMinimalTree(Project project) {
        if (project == null) {
            return "";
        }
        try {
            final Path root = Paths.get(project.getProjectDirectory().getPath());
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

    private record CachedResult(
        String name, String folder, String type,
        String importPrefix, String eeVersion, String jdkVersion,
        long timestamp
    ) {}
}

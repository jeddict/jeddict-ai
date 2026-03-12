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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import io.github.jeddict.ai.agent.FileSystemTools;
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

    /**
     * Optional hook that project-type-specific tools (e.g. MavenProjectTools,
     * GradleProjectTools) implement so that {@link ProjectMetadataInfo} can
     * obtain metadata without parsing build files directly.
     *
     * <p>This interface lives here (scanner package) so that the agent package
     * can depend on scanner — not the other way round.</p>
     */
    public interface BuildMetadataResolver {
        /** Returns the project display name, or {@code null} to keep the default. */
        String getProjectName();

        /**
         * Returns the build-system type label for this project (e.g. "maven",
         * "gradle", "ant", "nodejs", "python"). {@code null} means unknown.
         */
        String getProjectType();

        /**
         * Returns the name of the primary build file relative to the project
         * root (e.g. "pom.xml", "build.gradle", "build.xml"). Used for
         * cache-invalidation: when the file changes the cached metadata is
         * discarded. {@code null} means use the project directory mtime.
         */
        String getBuildFileName();

        /**
         * Returns an ordered map of project metadata key-value pairs to include
         * in the project info output (e.g. "Java Version" → "11").
         * Implementations should return an empty map when no metadata is
         * available; returning {@code null} is treated the same as an empty map.
         */
        Map<String, String> getProjectMetadata();
    }

    public static String get(Project project) {
        return get(project, null);
    }

    public static String get(Project project, BuildMetadataResolver resolver) {
        if (project == null) {
            return "";
        }

        final CachedResult cachedResult = getCachedResult(project, resolver);

        // Check if cachedResult is null and handle accordingly
        if (cachedResult == null) {
            return "Project Metadata: Unable to retrieve metadata for the specified project.";
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("- name: ").append(StringUtils.defaultString(cachedResult.name)).append('\n')
          .append("- folder: ").append(StringUtils.defaultString(cachedResult.folder)).append('\n')
        ;

        if (cachedResult.type() != null) {
            sb.append("- type: ").append(cachedResult.type()).append('\n');
        }

        for (final Map.Entry<String, String> entry : cachedResult.metadata().entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
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
        return getCachedResult(project, null);
    }

    /**
     * Returns (and caches) a {@link CachedResult} for the given project.
     *
     * <p>When a {@link BuildMetadataResolver} is supplied, the project type,
     * build file name, extra metadata key-value pairs, and project name are
     * all obtained from it. When {@code resolver} is {@code null} type and
     * metadata are absent — the generic path does not attempt to parse any
     * build file.</p>
     */
    public static CachedResult getCachedResult(Project project, BuildMetadataResolver resolver) {
        try {
            // Check if the project is cached
            CachedResult cachedResult = cache.get(project);

            final FileObject projectDirectory = project.getProjectDirectory();
            final String folder = FilenameUtils.separatorsToSystem(projectDirectory.getPath());

            // Use the build file modification time for cache invalidation; fall
            // back to the project directory mtime when no resolver (or no build
            // file name) is provided.
            final String buildFileName = (resolver != null) ? resolver.getBuildFileName() : null;
            final FileObject buildFile = (buildFileName != null)
                    ? projectDirectory.getFileObject(buildFileName)
                    : null;
            final long lastModified = buildFile != null
                    ? new File(buildFile.getPath()).lastModified()
                    : new File(folder).lastModified();

            if (cachedResult != null && cachedResult.timestamp >= lastModified) {
                return cachedResult;
            }

            // Default project name: directory name.  The resolver may override this.
            String name = projectDirectory.getName();

            // Project type and metadata come entirely from the resolver.
            // When no resolver is provided these remain null/empty.
            String type = null;
            Map<String, String> metadata = Collections.emptyMap();

            if (resolver != null) {
                if (resolver.getProjectName() != null && !resolver.getProjectName().isBlank()) {
                    name = resolver.getProjectName();
                }
                type = resolver.getProjectType();
                final Map<String, String> resolved = resolver.getProjectMetadata();
                if (resolved != null) {
                    metadata = resolved;
                }
            }

            // Cache the result
            final CachedResult result = new CachedResult(name, folder, type, metadata, lastModified);
            cache.put(project, result);
            return result;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to read project metadata", e);
            return null;
        }
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
        return getFileTree(project, null, 0);
    }

    /**
     * Returns the file tree structure rooted at the given sub-path inside the
     * project, limited to the specified depth. Directories configured in
     * {@link PreferencesManager#getExcludeDirs()}, hidden entries, and files
     * whose extension is not in
     * {@link PreferencesManager#getFileExtensionListToInclude()} are excluded.
     *
     * @param project  the project whose file tree to return
     * @param subPath  a path relative to the project root to use as the tree
     *                 root (e.g. {@code "src/main/java"}); {@code null} or blank
     *                 means the entire project directory
     * @param maxDepth the maximum number of directory levels to descend;
     *                 values {@code <= 0} mean unlimited depth
     * @return the file tree as an indented string, or an empty string if the
     *         project is null or the tree cannot be read
     */
    public static String getFileTree(Project project, String subPath, int maxDepth) {
        if (project == null) {
            return "";
        }
        final Path projectRoot = Paths.get(project.getProjectDirectory().getPath());
        return FileSystemTools.getFileTree(projectRoot, subPath, maxDepth);
    }

    /**
     * Returns the directory hierarchy of the project as a formatted string.
     * Only directories are included — individual files and classes are omitted
     * — giving a compact overview of the package structure. Hidden directories
     * and directories configured in {@link io.github.jeddict.ai.settings.PreferencesManager#getExcludeDirs()}
     * are excluded.
     *
     * @param project the project whose directory hierarchy to return
     * @return the directory hierarchy as an indented string, or an empty
     *         string if the project is null or the tree cannot be read
     */
    public static String getDirTree(Project project) {
        if (project == null) {
            return "";
        }
        final Path root = Paths.get(project.getProjectDirectory().getPath());
        return FileSystemTools.getDirTree(root);
    }

    private record CachedResult(
        String name, String folder, String type,
        Map<String, String> metadata,
        long timestamp
    ) {}
}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.maven.api.NbMavenProject;
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

            // Get project modification timestamp
            NbMavenProject nbMavenProject = project.getLookup().lookup(NbMavenProject.class);
            if (nbMavenProject == null) {
                return null; // Not a Maven project
            }

            // Get the pom.xml FileObject
            final File pomFile = nbMavenProject.getMavenProject().getFile();
            final long lastModified = pomFile.lastModified();

            // Invalidate cache if timestamp has changed
            if (cachedResult == null || cachedResult.timestamp < lastModified) {
                // Get the MavenProject
                MavenProject mavenProject = nbMavenProject.getMavenProject();

                // Determine Jakarta EE version
                String eeVersion = getEEVersionFromDependencies(mavenProject);

                // Determine JDK version
                String jdkVersion = getJdkVersionFromPom(mavenProject);

                // Determine the import prefix
                String importPrefix = null;
                if (eeVersion != null) {
                    if (eeVersion.startsWith("jakarta")) {
                        if (eeVersion.equals("jakarta-8.0.0")) {
                            importPrefix = "javax"; // Special case for Jakarta EE 8
                        } else {
                            importPrefix = "jakarta";
                        }
                    } else if (eeVersion.startsWith("javax")) {
                        importPrefix = "javax";
                    }
                }

                // Cache the result
                final CachedResult result = new CachedResult(
                    mavenProject.getName(), 
                    FilenameUtils.separatorsToSystem(project.getProjectDirectory().getPath()), 
                    "maven",
                    importPrefix, eeVersion, jdkVersion, lastModified
                );
                cache.put(project, result);

                return result;
            }

            // Return cached result
            return cachedResult;

        } catch (Exception e) {
            e.printStackTrace();
            return null; // In case of errors
        }
    }

    private static String getEEVersionFromDependencies(MavenProject mavenProject) {
        // Look for Jakarta EE or Java EE dependencies in the pom.xml
        for (org.apache.maven.model.Dependency dependency : mavenProject.getDependencies()) {
            if (dependency.getGroupId().equals("jakarta.platform")) {
                if (dependency.getVersion().startsWith("8.0")) {
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

    private static String getJdkVersionFromPom(MavenProject mavenProject) {
        // Check for JDK version in Maven properties
        String source = mavenProject.getProperties().getProperty("maven.compiler.source");
        String target = mavenProject.getProperties().getProperty("maven.compiler.target");

        // Return source version if available; fallback to target
        if (source != null) {
            return source;
        }
        if (target != null) {
            return target;
        }
        for (Plugin plugin : mavenProject.getBuildPlugins()) {
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

        // Return null if no JDK version is found
        return null;
    }

    private static final Set<String> IGNORED_NAMES = Set.of("target", "node_modules", "build");

    /**
     * Returns the main Java sources directory path relative to the project
     * root (e.g. {@code src/main/java}).
     *
     * <p>Uses the NetBeans {@link Sources} API first so that any project type
     * (Gradle, Ant, …) is handled uniformly. Falls back to the Maven
     * {@code NbMavenProject} API when the generic API returns no groups (e.g.
     * when the Maven NetBeans module is not fully initialised). Returns an
     * empty string for non-Java and non-Maven projects.</p>
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
        if (groups.length > 0) {
            final Set<FileObject> testRoots = findTestRoots(groups);
            for (final SourceGroup sg : groups) {
                if (!testRoots.contains(sg.getRootFolder())) {
                    return relativize(project, sg.getRootFolder().getPath());
                }
            }
        }
        final NbMavenProject nbMaven = project.getLookup().lookup(NbMavenProject.class);
        if (nbMaven != null) {
            return relativize(project, nbMaven.getMavenProject().getBuild().getSourceDirectory());
        }
        return "";
    }

    /**
     * Returns the main resources directory path relative to the project root
     * (e.g. {@code src/main/resources}).
     *
     * <p>Uses the NetBeans {@link Sources} API first so that any project type
     * (Gradle, Ant, …) is handled uniformly. Falls back to the Maven
     * {@code NbMavenProject} API when no resource source groups are registered.
     * Returns an empty string for projects with no resource roots.</p>
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
        final NbMavenProject nbMaven = project.getLookup().lookup(NbMavenProject.class);
        if (nbMaven != null) {
            final var resources = nbMaven.getMavenProject().getBuild().getResources();
            if (!resources.isEmpty()) {
                return relativize(project, resources.get(0).getDirectory());
            }
        }
        return "";
    }

    /**
     * Returns the test Java sources directory path relative to the project
     * root (e.g. {@code src/test/java}).
     *
     * <p>Uses the NetBeans {@link Sources} API first so that any project type
     * (Gradle, Ant, …) is handled uniformly. Falls back to the Maven
     * {@code NbMavenProject} API when the generic API returns no groups.
     * Returns an empty string for non-Java and non-Maven projects.</p>
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
        if (groups.length > 0) {
            final Set<FileObject> testRoots = findTestRoots(groups);
            for (final SourceGroup sg : groups) {
                if (testRoots.contains(sg.getRootFolder())) {
                    return relativize(project, sg.getRootFolder().getPath());
                }
            }
        }
        final NbMavenProject nbMaven = project.getLookup().lookup(NbMavenProject.class);
        if (nbMaven != null) {
            return relativize(project, nbMaven.getMavenProject().getBuild().getTestSourceDirectory());
        }
        return "";
    }

    /**
     * Returns the test resources directory path relative to the project root
     * (e.g. {@code src/test/resources}).
     *
     * <p>Uses the NetBeans {@link Sources} API first so that any project type
     * (Gradle, Ant, …) is handled uniformly. Falls back to the Maven
     * {@code NbMavenProject} API when no test resource source groups are
     * registered. Returns an empty string for projects with no test resource
     * roots.</p>
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
        final NbMavenProject nbMaven = project.getLookup().lookup(NbMavenProject.class);
        if (nbMaven != null) {
            final var resources = nbMaven.getMavenProject().getBuild().getTestResources();
            if (!resources.isEmpty()) {
                return relativize(project, resources.get(0).getDirectory());
            }
        }
        return "";
    }

    /**
     * Identifies which roots among {@code groups} are test roots.
     *
     * <p>For each source group the NetBeans {@link UnitTestForSourceQuery} is
     * asked for its associated unit-test roots. Any root that appears as a
     * test-target of another group in the same set is considered a test root.
     * This mirrors the logic in {@code ProjectUtil.getTestSourceGroups} but
     * operates purely on a pre-fetched array and returns {@link FileObject}
     * roots for O(1) lookup.</p>
     */
    private static Set<FileObject> findTestRoots(final SourceGroup[] groups) {
        final Map<FileObject, Boolean> folderIndex = new HashMap<>();
        for (final SourceGroup sg : groups) {
            folderIndex.put(sg.getRootFolder(), Boolean.FALSE);
        }
        final Set<FileObject> testRoots = new HashSet<>();
        for (final SourceGroup sg : groups) {
            for (final URL url : UnitTestForSourceQuery.findUnitTests(sg.getRootFolder())) {
                final FileObject fo = URLMapper.findFileObject(url);
                if (fo != null && folderIndex.containsKey(fo)) {
                    testRoots.add(fo);
                }
            }
        }
        return testRoots;
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
     * string. Hidden files/directories and common build output directories
     * (target, node_modules, build) are excluded.
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
            final StringBuilder sb = new StringBuilder();
            try (Stream<Path> stream = Files.walk(root)) {
                stream
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
     * Returns the minimal directory hierarchy of the project as a formatted
     * string. Only directories are included — individual files and classes are
     * omitted — giving a compact overview of the package structure.
     * Hidden directories and common build output directories
     * (target, node_modules, build) are excluded.
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

    private static boolean isExcluded(final Path root, final Path path) {
        if (path.equals(root)) {
            return false;
        }
        for (final Path component : root.relativize(path)) {
            final String name = component.toString();
            if (name.startsWith(".") || IGNORED_NAMES.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private record CachedResult(
        String name, String folder, String type,
        String importPrefix, String eeVersion, String jdkVersion,
        long timestamp
    ) {}
}

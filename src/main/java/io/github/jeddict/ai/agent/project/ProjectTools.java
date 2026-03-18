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
package io.github.jeddict.ai.agent.project;

import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.ToolPolicy;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;

/**
 * Tool to return information about the project: jdk version, j2ee version,
 * and file tree structure.
 *
 * <p>This is the generic base class. Use {@link #forProject(Project)} to
 * obtain the most specific subclass for the project's build system.</p>
 */
public class ProjectTools extends AbstractTool {

    private static final Logger LOG = Logger.getLogger(ProjectTools.class.getName());

    private final Project project;

    public ProjectTools(final Project project) throws IOException  {
        super(project.getProjectDirectory().getPath());
        this.project = project;
    }

    /**
     * Factory method that returns the most specific {@link ProjectTools}
     * subclass for the given project's build system.
     *
     * <ul>
     *   <li>Maven ({@code pom.xml} present) → {@link MavenProjectTools}</li>
     *   <li>Gradle ({@code build.gradle} or {@code build.gradle.kts}) → {@link GradleProjectTools}</li>
     *   <li>NetBeans Module ({@code nbproject/project.xml}) → {@link NetBeansModuleProjectTools}</li>
     *   <li>Ant ({@code build.xml}) → {@link AntProjectTools}</li>
     *   <li>Frontend ({@code package.json} + framework config) → {@link FrontendProjectTools}</li>
     *   <li>Node.js ({@code package.json}) → {@link NodeJsProjectTools}</li>
     *   <li>Python ({@code pyproject.toml}, {@code setup.py}, or {@code requirements.txt}) → {@link PythonProjectTools}</li>
     *   <li>PHP ({@code composer.json}) → {@link PhpProjectTools}</li>
     *   <li>CMake/Make ({@code CMakeLists.txt}, {@code Makefile}, or {@code makefile}) → {@link MakefileProjectTools}</li>
     *   <li>Rust ({@code Cargo.toml}) → {@link RustProjectTools}</li>
     *   <li>Go ({@code go.mod}) → {@link GoProjectTools}</li>
     *   <li>Docker ({@code Dockerfile} or {@code docker-compose.yml}) → {@link DockerProjectTools}</li>
     *   <li>Otherwise → {@link ProjectTools} (generic)</li>
     * </ul>
     */
    public static ProjectTools forProject(final Project project) throws IOException {
        final FileObject dir = project.getProjectDirectory();
        if (dir.getFileObject("pom.xml") != null) {
            try {
                final String pomContent = dir.getFileObject("pom.xml").asText();
                if (pomContent.contains("payara-micro-maven-plugin")) {
                    return new PayaraMicroMavenProjectTools(project);
                }
                if (pomContent.contains("payara-server-maven-plugin")) {
                    return new PayaraServerMavenProjectTools(project);
                }
            } catch (final IOException ignored) {
                // fall through to the generic MavenProjectTools
            }
            return new MavenProjectTools(project);
        }
        if (dir.getFileObject("build.gradle") != null
                || dir.getFileObject("build.gradle.kts") != null) {
            return new GradleProjectTools(project);
        }
        // NetBeans module projects often also have build.xml; check nbproject/ first
        final FileObject nbproject = dir.getFileObject("nbproject");
        if (nbproject != null && nbproject.getFileObject("project.xml") != null) {
            return new NetBeansModuleProjectTools(project);
        }
        if (dir.getFileObject("build.xml") != null) {
            return new AntProjectTools(project);
        }
        // Check for a frontend framework config before plain package.json
        if (dir.getFileObject("package.json") != null && hasFrontendFrameworkConfig(dir)) {
            return new FrontendProjectTools(project);
        }
        if (dir.getFileObject("package.json") != null) {
            return new NodeJsProjectTools(project);
        }
        if (dir.getFileObject("pyproject.toml") != null
                || dir.getFileObject("setup.py") != null
                || dir.getFileObject("requirements.txt") != null) {
            return new PythonProjectTools(project);
        }
        if (dir.getFileObject("composer.json") != null) {
            return new PhpProjectTools(project);
        }
        if (dir.getFileObject("CMakeLists.txt") != null
                || dir.getFileObject("Makefile") != null
                || dir.getFileObject("makefile") != null) {
            return new MakefileProjectTools(project);
        }
        if (dir.getFileObject("Cargo.toml") != null) {
            return new RustProjectTools(project);
        }
        if (dir.getFileObject("go.mod") != null) {
            return new GoProjectTools(project);
        }
        if (dir.getFileObject("Dockerfile") != null
                || dir.getFileObject("dockerfile") != null
                || dir.getFileObject("docker-compose.yml") != null
                || dir.getFileObject("docker-compose.yaml") != null) {
            return new DockerProjectTools(project);
        }
        return new ProjectTools(project);
    }

    /** Returns the NB {@link Project} this tool is bound to. */
    protected Project project() {
        return project;
    }

    @Tool(
        name = "projectInfo",
        value = "Return information about the project: jdk version, j2ee version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo()
    throws Exception {
        progress("Gathering project info: " + project());
        return appendSourceDirs(ProjectMetadataInfo.get(project()));
    }

    @Tool(
        name = "projectSrcDir",
        value = "Return the path of the main Java sources directory (e.g. src/main/java)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcDir()
    throws Exception {
        progress("Gathering project source directory: " + project());
        return getSrcDir(project());
    }

    @Tool(
        name = "projectSrcResourceDir",
        value = "Return the path of the main resources directory (e.g. src/main/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcResourceDir()
    throws Exception {
        progress("Gathering project source resources directory: " + project());
        return getSrcResourceDir(project());
    }

    @Tool(
        name = "projectTestDir",
        value = "Return the path of the test Java sources directory (e.g. src/test/java)"
    )
    @ToolPolicy(READONLY)
    public String projectTestDir()
    throws Exception {
        progress("Gathering project test directory: " + project());
        return getTestDir(project());
    }

    @Tool(
        name = "projectTestResourceDir",
        value = "Return the path of the test resources directory (e.g. src/test/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectTestResourceDir()
    throws Exception {
        progress("Gathering project test resources directory: " + project());
        return getTestResourceDir(project());
    }

    @Tool(
        name = "projectDependencies",
        value = "Return the list of dependencies declared in the project's build file"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies()
    throws Exception {
        progress("Gathering project dependencies: " + project());
        return "No dependency information available for this project type";
    }

    // -----------------------------------------------------------------------
    // Source / test directory helpers
    // -----------------------------------------------------------------------

    /**
     * Appends the main source directory and test source directory lines to the
     * given project-info string (which is typically produced by
     * {@link ProjectMetadataInfo#get}).
     *
     * @param info the base project info string
     * @return info extended with source/test directory lines
     */
    protected final String appendSourceDirs(final String info) {
        final StringBuilder sb = new StringBuilder(info);
        final String srcDir = getSrcDir(project());
        if (!srcDir.isBlank()) {
            sb.append('\n').append("- Source Directory: ").append(srcDir);
        }
        final String testDir = getTestDir(project());
        if (!testDir.isBlank()) {
            sb.append('\n').append("- Test Source Directory: ").append(testDir);
        }
        return sb.toString();
    }

    /**
     * Returns the main Java sources directory path relative to the project
     * root (e.g. {@code src/main/java}).
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

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when the given project directory contains at least
     * one well-known frontend framework configuration file (Angular, Next.js,
     * Vite, or Webpack).
     */
    private static boolean hasFrontendFrameworkConfig(final FileObject dir) {
        return dir.getFileObject("angular.json") != null
                || dir.getFileObject("next.config.js") != null
                || dir.getFileObject("next.config.mjs") != null
                || dir.getFileObject("next.config.ts") != null
                || dir.getFileObject("vite.config.js") != null
                || dir.getFileObject("vite.config.ts") != null
                || dir.getFileObject("vite.config.mjs") != null
                || dir.getFileObject("webpack.config.js") != null
                || dir.getFileObject("webpack.config.ts") != null;
    }

    // Matches path segments that represent test source roots: "test", "tests",
    // "test-java", "test-resources" etc. — but not unrelated words like "latest".
    private static final Pattern TEST_SEGMENT_PATTERN =
            Pattern.compile("^tests?(?:[^a-zA-Z].*)?$", Pattern.CASE_INSENSITIVE);

    /**
     * Identifies which roots among {@code groups} are test roots.
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
                LOG.log(Level.FINE, "UnitTestForSourceQuery unavailable; using path-based test-root detection", e);
                queryAvailable = false;
            }
        }
        if (!queryAvailable && testRoots.isEmpty() && !folderIndex.isEmpty()) {
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
     * {@code replacement}.
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
                return projectRoot.relativize(absolute).toString().replace(File.separatorChar, '/');
            }
            return absolutePath;
        } catch (final IllegalArgumentException e) {
            return absolutePath;
        }
    }
}
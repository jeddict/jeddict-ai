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
import org.netbeans.api.project.Project;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import java.io.IOException;
import org.openide.filesystems.FileObject;

/**
 * Tool to return information about the project: jdk version, j2ee version,
 * and file tree structure.
 *
 * <p>This is the generic base class. Use {@link #forProject(Project)} to
 * obtain the most specific subclass for the project's build system.</p>
 */
public class ProjectTools extends AbstractTool {

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
        return ProjectMetadataInfo.get(project());
    }

    @Tool(
        name = "projectSrcDir",
        value = "Return the path of the main Java sources directory (e.g. src/main/java)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcDir()
    throws Exception {
        progress("Gathering project source directory: " + project());
        return ProjectMetadataInfo.getSrcDir(project());
    }

    @Tool(
        name = "projectSrcResourceDir",
        value = "Return the path of the main resources directory (e.g. src/main/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcResourceDir()
    throws Exception {
        progress("Gathering project source resources directory: " + project());
        return ProjectMetadataInfo.getSrcResourceDir(project());
    }

    @Tool(
        name = "projectTestDir",
        value = "Return the path of the test Java sources directory (e.g. src/test/java)"
    )
    @ToolPolicy(READONLY)
    public String projectTestDir()
    throws Exception {
        progress("Gathering project test directory: " + project());
        return ProjectMetadataInfo.getTestDir(project());
    }

    @Tool(
        name = "projectTestResourceDir",
        value = "Return the path of the test resources directory (e.g. src/test/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectTestResourceDir()
    throws Exception {
        progress("Gathering project test resources directory: " + project());
        return ProjectMetadataInfo.getTestResourceDir(project());
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
}
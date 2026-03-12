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
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo.BuildMetadataResolver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.netbeans.api.project.Project;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Maven projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Maven-specific metadata
 * (EE version, JDK/compiler version) by parsing {@code pom.xml} through the
 * {@link MavenXpp3Reader}.  It also implements {@link BuildMetadataResolver}
 * so that {@link ProjectMetadataInfo} can obtain this data without parsing
 * build files itself.</p>
 */
public class MavenProjectTools extends JvmProjectTools implements BuildMetadataResolver {

    private final FileObject pomFile;

    /** Lazily-parsed model; {@code null} until first access. */
    private Model cachedModel;
    private boolean modelParsed = false;

    public MavenProjectTools(final Project project) throws IOException {
        super(project);
        this.pomFile = project.getProjectDirectory().getFileObject("pom.xml");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: jdk version, j2ee version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Maven project info: " + project());
        return appendSourceDirs(ProjectMetadataInfo.get(project(), this));
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        final Model model = model();
        if (model == null) {
            return null;
        }
        final String name = model.getName();
        return (name != null && !name.isBlank()) ? name : null;
    }

    @Override
    public String getProjectType() {
        return "maven";
    }

    @Override
    public String getBuildFileName() {
        return "pom.xml";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final Model model = model();
        if (model == null) {
            return metadata;
        }
        final String framework = parseFramework(model.getDependencies());
        if (framework != null) {
            if (framework.startsWith("jakarta") || framework.startsWith("javax")) {
                // Keep EE-specific keys for backward compatibility
                metadata.put("EE Version", framework);
                final String importPrefix;
                if (framework.startsWith("jakarta")) {
                    importPrefix = framework.equals("jakarta-8.0.0") ? "javax" : "jakarta";
                } else {
                    importPrefix = "javax";
                }
                metadata.put("EE Import Prefix", importPrefix);
            } else {
                metadata.put("Framework", framework);
            }
        }
        final String jdkVersion = parseJdkVersion(model);
        if (jdkVersion != null) {
            metadata.put("Java Version", jdkVersion);
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Tool(
        name = "frameworkVersion",
        value = "Return the primary framework or platform used by this Maven project (e.g. 'jakarta', 'javax', 'spring-boot', 'quarkus', 'micronaut', 'helidon')"
    )
    @ToolPolicy(READONLY)
    public String frameworkVersion() throws Exception {
        progress("Reading framework/platform from pom.xml");
        final Model model = model();
        final String v = model != null ? parseFramework(model.getDependencies()) : null;
        return v != null ? v : "No known framework dependency found in pom.xml";
    }

    @Override
    @Tool(
        name = "jdkVersion",
        value = "Return the Java compiler source version configured in this Maven project's pom.xml"
    )
    @ToolPolicy(READONLY)
    public String jdkVersion() throws Exception {
        progress("Reading JDK version from pom.xml");
        final Model model = model();
        final String v = model != null ? parseJdkVersion(model) : null;
        return v != null ? v : "No compiler version found in pom.xml";
    }

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of Maven dependencies declared in pom.xml, one per line as groupId:artifactId:version (scope)"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from pom.xml");
        final Model model = model();
        if (model == null) {
            return "Unable to read pom.xml";
        }
        final List<Dependency> deps = model.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return "No dependencies declared in pom.xml";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Dependency dep : deps) {
            sb.append(dep.getGroupId()).append(':')
              .append(dep.getArtifactId()).append(':')
              .append(dep.getVersion() != null ? dep.getVersion() : "(managed)");
            if (dep.getScope() != null) {
                sb.append(" (").append(dep.getScope()).append(')');
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    @Override
    @Tool(
        name = "runJavaClass",
        value = "Run a Java class by its fully qualified class name using the Maven exec plugin "
            + "and return the full output"
    )
    @ToolPolicy(READWRITE)
    public String runJavaClass(final String mainClass) {
        return runCommand(resolveRunCommand(mainClass), "Running " + mainClass);
    }

    /**
     * Builds the Maven command to run {@code mainClass}, preferring the Maven
     * wrapper ({@code mvnw} / {@code mvnw.cmd}) when it is present in the
     * project directory.
     *
     * @param mainClass the fully qualified name of the class to run
     * @return the shell command string (never {@code null})
     */
    String resolveRunCommand(final String mainClass) {
        return resolveWrapper() + " exec:java -Dexec.mainClass=" + mainClass;
    }

    @Override
    @Tool(
        name = "buildProject",
        value = "Build the Maven project using 'mvn clean install' (or the Maven wrapper) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public String buildProject() {
        return runCommand(resolveBuildCommand(), "Building");
    }

    /**
     * Returns the Maven command to build the project ({@code mvn[w] clean install}).
     *
     * @return the shell command string (never {@code null})
     */
    String resolveBuildCommand() {
        return resolveWrapper() + " clean install";
    }

    @Override
    @Tool(
        name = "testProject",
        value = "Run the Maven project's test suite using 'mvn test' (or the Maven wrapper) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public String testProject() {
        return runCommand(resolveTestCommand(), "Testing");
    }

    /**
     * Returns the Maven command to run the project's tests ({@code mvn[w] test}).
     *
     * @return the shell command string (never {@code null})
     */
    String resolveTestCommand() {
        return resolveWrapper() + " test";
    }

    /**
     * Returns the Maven executable to use: the Maven wrapper ({@code ./mvnw}
     * or {@code mvnw.cmd}) when present, otherwise the system {@code mvn}.
     */
    protected String resolveWrapper() {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        final File basedirFile = new File(basedir);
        if (isWindows) {
            if (new File(basedirFile, "mvnw.cmd").exists()) return "mvnw.cmd";
            if (new File(basedirFile, "mvnw").exists()) return "./mvnw";
            return "mvn";
        } else {
            return new File(basedirFile, "mvnw").exists() ? "./mvnw" : "mvn";
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Model model() {
        if (!modelParsed) {
            modelParsed = true;
            if (pomFile != null) {
                try (final InputStream in = pomFile.getInputStream()) {
                    cachedModel = new MavenXpp3Reader().read(in);
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Failed to parse pom.xml", ex);
                }
            }
        }
        return cachedModel;
    }

    /**
     * Detects the primary framework or platform used in this project from its Maven dependencies.
     *
     * <p>Returns the first recognised framework encountered in the dependency list.
     * For the full dependency list, use {@link #projectDependencies()}.</p>
     */
    private static String parseFramework(final List<Dependency> dependencies) {
        for (final Dependency dep : dependencies) {
            final String groupId = dep.getGroupId();
            if ("jakarta.platform".equals(groupId)) {
                if (dep.getVersion() != null && dep.getVersion().startsWith("8.0")) {
                    return "jakarta-8.0.0";
                }
                return "jakarta";
            }
            if (groupId.startsWith("javax.")) {
                return "javax";
            }
            if ("org.springframework.boot".equals(groupId)) {
                return "spring-boot";
            }
            if ("io.quarkus".equals(groupId) || groupId.startsWith("io.quarkus.")) {
                return "quarkus";
            }
            if ("io.micronaut".equals(groupId) || groupId.startsWith("io.micronaut.")) {
                return "micronaut";
            }
            if ("io.helidon".equals(groupId) || groupId.startsWith("io.helidon.")) {
                return "helidon";
            }
        }
        return null;
    }

    private static String parseJdkVersion(Model model) {
        final String source = model.getProperties().getProperty("maven.compiler.source");
        if (source != null) return source;
        final String target = model.getProperties().getProperty("maven.compiler.target");
        if (target != null) return target;
        if (model.getBuild() != null) {
            for (final Plugin plugin : model.getBuild().getPlugins()) {
                if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    final Object cfg = plugin.getConfiguration();
                    if (cfg instanceof Xpp3Dom dom) {
                        final Xpp3Dom sourceNode = dom.getChild("source");
                        if (sourceNode != null) {
                            return sourceNode.getValue();
                        }
                    }
                }
            }
        }
        return null;
    }
}

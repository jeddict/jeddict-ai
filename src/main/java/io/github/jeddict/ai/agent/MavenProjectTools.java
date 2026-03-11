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
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo.BuildMetadataResolver;
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
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
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
public class MavenProjectTools extends ProjectTools implements BuildMetadataResolver {

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
        return ProjectMetadataInfo.get(project(), this);
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
        final String eeVersion = parseEeVersion(model.getDependencies());
        if (eeVersion != null) {
            metadata.put("EE Version", eeVersion);
            final String importPrefix;
            if (eeVersion.startsWith("jakarta")) {
                importPrefix = eeVersion.equals("jakarta-8.0.0") ? "javax" : "jakarta";
            } else if (eeVersion.startsWith("javax")) {
                importPrefix = "javax";
            } else {
                importPrefix = null;
            }
            if (importPrefix != null) {
                metadata.put("EE Import Prefix", importPrefix);
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
        name = "eeVersion",
        value = "Return the Jakarta EE or Java EE version used by this Maven project (e.g. 'jakarta', 'javax')"
    )
    @ToolPolicy(READONLY)
    public String eeVersion() throws Exception {
        progress("Reading EE version from pom.xml");
        final Model model = model();
        final String v = model != null ? parseEeVersion(model.getDependencies()) : null;
        return v != null ? v : "No EE dependency found in pom.xml";
    }

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

    private static String parseEeVersion(List<Dependency> dependencies) {
        for (final Dependency dep : dependencies) {
            if ("jakarta.platform".equals(dep.getGroupId())) {
                if (dep.getVersion() != null && dep.getVersion().startsWith("8.0")) {
                    return "jakarta-8.0.0";
                }
                return "jakarta";
            }
            if (dep.getGroupId().startsWith("javax.")) {
                return "javax";
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

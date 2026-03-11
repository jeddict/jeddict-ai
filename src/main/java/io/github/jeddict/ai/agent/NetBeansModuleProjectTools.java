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
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for NetBeans Platform module projects.
 *
 * <p>NetBeans Platform module projects (also known as NBM projects) are
 * Ant-based projects managed by the {@code nbm-maven-plugin} or by the
 * classic {@code nbproject/} structure. This tool handles the classic
 * Ant-based variant where {@code nbproject/project.xml} is present.</p>
 *
 * <p>It implements {@link BuildMetadataResolver} so that
 * {@link ProjectMetadataInfo} can obtain the project type and code-name base
 * without parsing build files directly.</p>
 */
public class NetBeansModuleProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Extracts the module's code-name base from project.xml. */
    private static final Pattern CODE_NAME_BASE =
            Pattern.compile("<code-name-base>([^<]+)</code-name-base>");

    /**
     * Extracts each module dependency's code-name base from the
     * {@code <module-dependencies>} block in project.xml.
     */
    private static final Pattern DEP_MODULE =
            Pattern.compile("<dependency>\\s*<code-name-base>([^<]+)</code-name-base>",
                    Pattern.DOTALL);

    private final FileObject projectXml;

    /** Lazily read content of nbproject/project.xml; {@code null} until first access. */
    private String projectXmlContent;
    private boolean contentRead = false;

    public NetBeansModuleProjectTools(final Project project) throws IOException {
        super(project);
        final FileObject nbproject = project.getProjectDirectory().getFileObject("nbproject");
        this.projectXml = (nbproject != null) ? nbproject.getFileObject("project.xml") : null;
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the NetBeans Platform module project"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering NetBeans module project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        final String content = content();
        if (content == null) {
            return null;
        }
        final Matcher m = CODE_NAME_BASE.matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    @Override
    public String getProjectType() {
        return "netbeans-module";
    }

    @Override
    public String getBuildFileName() {
        return "nbproject/project.xml";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        return Collections.emptyMap();
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the module dependencies declared in nbproject/project.xml, one per line as the module code-name base"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading module dependencies from nbproject/project.xml");
        final String content = content();
        if (content == null) {
            return "Unable to read nbproject/project.xml";
        }
        final Matcher m = DEP_MODULE.matcher(content);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1).trim()).append('\n');
        }
        return sb.length() == 0
                ? "No module dependencies found in nbproject/project.xml"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (projectXml != null) {
                try {
                    projectXmlContent = projectXml.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read nbproject/project.xml", ex);
                }
            }
        }
        return projectXmlContent;
    }
}

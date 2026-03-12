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
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Apache Ant projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Ant-specific metadata by
 * inspecting the {@code build.xml} file.  It implements
 * {@link BuildMetadataResolver} so that {@link ProjectMetadataInfo} can obtain
 * the project type without parsing build files directly.</p>
 */
public class AntProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code <project name="my-app" ...>} in build.xml. */
    private static final Pattern PROJECT_NAME =
            Pattern.compile("<project[^>]*name\\s*=\\s*['\"]([^'\"]+)['\"]");

    private final FileObject buildXml;

    /** Lazily read content of build.xml; {@code null} until first access. */
    private String buildXmlContent;
    private boolean contentRead = false;

    public AntProjectTools(final Project project) throws IOException {
        super(project);
        this.buildXml = project.getProjectDirectory().getFileObject("build.xml");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: ant build configuration"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Ant project info: " + project());
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
        final Matcher m = PROJECT_NAME.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public String getProjectType() {
        return "ant";
    }

    @Override
    public String getBuildFileName() {
        return "build.xml";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        return Collections.emptyMap();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (buildXml != null) {
                try {
                    buildXmlContent = buildXml.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read build.xml", ex);
                }
            }
        }
        return buildXmlContent;
    }
}

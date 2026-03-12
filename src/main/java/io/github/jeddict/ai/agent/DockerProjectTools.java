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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Docker projects.
 *
 * <p>Extends the generic {@link ProjectTools} for projects that contain a
 * {@code Dockerfile} and/or a {@code docker-compose.yml} (or
 * {@code docker-compose.yaml}). It implements {@link BuildMetadataResolver} so
 * that {@link ProjectMetadataInfo} can obtain the project type without parsing
 * build files directly.</p>
 */
public class DockerProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches each {@code FROM <image>} directive in a Dockerfile (case-insensitive). */
    private static final Pattern FROM_DIRECTIVE =
            Pattern.compile("(?im)^FROM\\s+(\\S+)");

    private final FileObject dockerfile;
    private final FileObject composeFile;
    private final String buildFileName;

    /** Lazily read content of the Dockerfile; {@code null} until first access. */
    private String dockerfileContent;
    private boolean contentRead = false;

    public DockerProjectTools(final Project project) throws IOException {
        super(project);
        final FileObject dir = project.getProjectDirectory();

        FileObject foundDockerfile = dir.getFileObject("Dockerfile");
        if (foundDockerfile == null) {
            foundDockerfile = dir.getFileObject("dockerfile");
        }
        this.dockerfile = foundDockerfile;

        FileObject foundCompose = dir.getFileObject("docker-compose.yml");
        if (foundCompose == null) {
            foundCompose = dir.getFileObject("docker-compose.yaml");
        }
        this.composeFile = foundCompose;

        this.buildFileName = (dockerfile != null) ? dockerfile.getNameExt()
                : (composeFile != null) ? composeFile.getNameExt()
                : null;
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the Docker project: base images, compose file"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Docker project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        // Docker projects do not have a canonical application name
        return null;
    }

    @Override
    public String getProjectType() {
        return "docker";
    }

    @Override
    public String getBuildFileName() {
        return buildFileName;
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        if (dockerfile != null) {
            final String content = dockerfileContent();
            if (content != null) {
                final Matcher m = FROM_DIRECTIVE.matcher(content);
                final StringBuilder images = new StringBuilder();
                while (m.find()) {
                    if (images.length() > 0) {
                        images.append(", ");
                    }
                    images.append(m.group(1));
                }
                if (images.length() > 0) {
                    metadata.put("Base Images", images.toString());
                }
            }
        }
        if (composeFile != null) {
            metadata.put("Compose File", composeFile.getNameExt());
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the base images declared via FROM directives in the Dockerfile, one per line"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading base images from Dockerfile");
        if (dockerfile == null) {
            return "No Dockerfile found in project directory";
        }
        final String content = dockerfileContent();
        if (content == null) {
            return "Unable to read Dockerfile";
        }
        final Matcher m = FROM_DIRECTIVE.matcher(content);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1)).append('\n');
        }
        return sb.length() == 0
                ? "No FROM directives found in Dockerfile"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String dockerfileContent() {
        if (!contentRead) {
            contentRead = true;
            if (dockerfile != null) {
                try {
                    dockerfileContent = dockerfile.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read Dockerfile", ex);
                }
            }
        }
        return dockerfileContent;
    }
}

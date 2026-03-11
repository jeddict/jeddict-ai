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
import org.json.JSONObject;
import org.netbeans.api.project.Project;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Node.js projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Node.js-specific metadata
 * by inspecting {@code package.json}.  It implements
 * {@link BuildMetadataResolver} so that {@link ProjectMetadataInfo} can obtain
 * the project type and name without parsing build files directly.</p>
 */
public class NodeJsProjectTools extends ProjectTools implements BuildMetadataResolver {

    private final FileObject packageJson;

    /** Lazily parsed JSON; {@code null} until first access. */
    private JSONObject parsedJson;
    private boolean jsonParsed = false;

    public NodeJsProjectTools(final Project project) throws IOException {
        super(project);
        this.packageJson = project.getProjectDirectory().getFileObject("package.json");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: nodejs version, package name"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Node.js project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        final JSONObject json = json();
        if (json == null || !json.has("name")) {
            return null;
        }
        final String name = json.optString("name", null);
        return (name != null && !name.isBlank()) ? name : null;
    }

    @Override
    public String getProjectType() {
        return "nodejs";
    }

    @Override
    public String getBuildFileName() {
        return "package.json";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final JSONObject json = json();
        if (json == null) {
            return metadata;
        }
        // Expose the declared Node engine version, if present
        final JSONObject engines = json.optJSONObject("engines");
        if (engines != null) {
            final String nodeVersion = engines.optString("node", null);
            if (nodeVersion != null && !nodeVersion.isBlank()) {
                metadata.put("Node Version", nodeVersion);
            }
        }
        // Expose the package version
        final String version = json.optString("version", null);
        if (version != null && !version.isBlank()) {
            metadata.put("Package Version", version);
        }
        return metadata;
    }

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of dependencies declared in package.json (dependencies and devDependencies), one per line as name:version (type)"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from package.json");
        final JSONObject json = json();
        if (json == null) {
            return "Unable to read package.json";
        }
        final StringBuilder sb = new StringBuilder();
        appendDeps(sb, json.optJSONObject("dependencies"), "dependency");
        appendDeps(sb, json.optJSONObject("devDependencies"), "devDependency");
        return sb.length() == 0
                ? "No dependencies declared in package.json"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static void appendDeps(final StringBuilder sb, final JSONObject deps,
                                   final String type) {
        if (deps == null) {
            return;
        }
        for (final String name : deps.keySet()) {
            sb.append(name).append(':')
              .append(deps.getString(name))
              .append(" (").append(type).append(")\n");
        }
    }

    private JSONObject json() {
        if (!jsonParsed) {
            jsonParsed = true;
            if (packageJson != null) {
                try {
                    parsedJson = new JSONObject(packageJson.asText());
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Failed to parse package.json", ex);
                }
            }
        }
        return parsedJson;
    }
}

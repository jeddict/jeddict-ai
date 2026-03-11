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
 * Project-tool specialisation for PHP projects managed with Composer.
 *
 * <p>Extends the generic {@link ProjectTools} with PHP-specific metadata by
 * inspecting {@code composer.json}. It implements {@link BuildMetadataResolver}
 * so that {@link ProjectMetadataInfo} can obtain the project type and name
 * without parsing build files directly.</p>
 */
public class PhpProjectTools extends ProjectTools implements BuildMetadataResolver {

    private final FileObject composerJson;

    /** Lazily parsed JSON; {@code null} until first access. */
    private JSONObject parsedJson;
    private boolean jsonParsed = false;

    public PhpProjectTools(final Project project) throws IOException {
        super(project);
        this.composerJson = project.getProjectDirectory().getFileObject("composer.json");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: php version, package name"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering PHP project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        final JSONObject json = json();
        if (json == null) {
            return null;
        }
        final String name = json.optString("name", null);
        return (name != null && !name.isBlank()) ? name : null;
    }

    @Override
    public String getProjectType() {
        return "php";
    }

    @Override
    public String getBuildFileName() {
        return "composer.json";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final JSONObject json = json();
        if (json == null) {
            return metadata;
        }
        // PHP engine version requirement from the "require" block
        final JSONObject require = json.optJSONObject("require");
        if (require != null) {
            final String phpVersion = require.optString("php", null);
            if (phpVersion != null && !phpVersion.isBlank()) {
                metadata.put("PHP Version", phpVersion);
            }
        }
        final String version = json.optString("version", null);
        if (version != null && !version.isBlank()) {
            metadata.put("Package Version", version);
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of PHP dependencies from composer.json (require and require-dev), one per line as package:version (type)"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from composer.json");
        final JSONObject json = json();
        if (json == null) {
            return "Unable to read composer.json";
        }
        final StringBuilder sb = new StringBuilder();
        appendDeps(sb, json.optJSONObject("require"), "require");
        appendDeps(sb, json.optJSONObject("require-dev"), "require-dev");
        return sb.length() == 0
                ? "No dependencies declared in composer.json"
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
            if (composerJson != null) {
                try {
                    parsedJson = new JSONObject(composerJson.asText());
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Failed to parse composer.json", ex);
                }
            }
        }
        return parsedJson;
    }
}

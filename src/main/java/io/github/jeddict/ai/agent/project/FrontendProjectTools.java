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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;
import org.json.JSONObject;

/**
 * Project-tool specialisation for JavaScript/TypeScript frontend projects.
 *
 * <p>Extends {@link NodeJsProjectTools} with framework-aware detection.
 * The active framework is determined by the presence of well-known
 * configuration files ({@code angular.json}, {@code next.config.js},
 * {@code vite.config.js}, {@code webpack.config.js}) or by the presence of
 * the framework's package as a dependency in {@code package.json}
 * ({@code react}, {@code vue}).</p>
 *
 * <p>It overrides {@link #getProjectType()} to return the detected framework
 * name (e.g. {@code "angular"}, {@code "next.js"}, {@code "vite"},
 * {@code "webpack"}, {@code "react"}, {@code "vue"}) so that
 * {@link ProjectMetadataInfo} can label the project correctly.</p>
 */
public class FrontendProjectTools extends NodeJsProjectTools {

    private static final Logger LOG = Logger.getLogger(FrontendProjectTools.class.getName());

    private final String framework;

    public FrontendProjectTools(final Project project) throws IOException {
        super(project);
        this.framework = detectFramework(project);
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the frontend project: framework, package name, Node.js version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering frontend project info (" + framework + "): " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver overrides
    // -----------------------------------------------------------------------

    @Override
    public String getProjectType() {
        return framework;
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>(super.getProjectMetadata());
        metadata.put("Framework", framework);
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Detects the frontend framework by inspecting well-known config files
     * first, then falling back to {@code package.json} dependency names.
     */
    private static String detectFramework(final Project project) {
        final FileObject dir = project.getProjectDirectory();

        // Config-file based detection (most reliable)
        if (dir.getFileObject("angular.json") != null) {
            return "angular";
        }
        if (dir.getFileObject("next.config.js") != null
                || dir.getFileObject("next.config.mjs") != null
                || dir.getFileObject("next.config.ts") != null) {
            return "next.js";
        }
        if (dir.getFileObject("vite.config.js") != null
                || dir.getFileObject("vite.config.ts") != null
                || dir.getFileObject("vite.config.mjs") != null) {
            return "vite";
        }
        if (dir.getFileObject("webpack.config.js") != null
                || dir.getFileObject("webpack.config.ts") != null) {
            return "webpack";
        }

        // Dependency-based detection via package.json
        final FileObject packageJson = dir.getFileObject("package.json");
        if (packageJson != null) {
            try {
                final JSONObject json = new JSONObject(packageJson.asText());
                final JSONObject deps = json.optJSONObject("dependencies");
                final JSONObject devDeps = json.optJSONObject("devDependencies");
                if (hasDep(deps, devDeps, "react")) {
                    return "react";
                }
                if (hasDep(deps, devDeps, "vue")) {
                    return "vue";
                }
            } catch (IOException | org.json.JSONException ex) {
                // best-effort; if package.json is unreadable or malformed, fall through
                LOG.log(Level.WARNING, "Failed to parse package.json for framework detection", ex);
            }
        }

        return "frontend";
    }

    private static boolean hasDep(final JSONObject deps, final JSONObject devDeps,
                                   final String name) {
        return (deps != null && deps.has(name)) || (devDeps != null && devDeps.has(name));
    }
}

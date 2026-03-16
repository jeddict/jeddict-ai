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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Go projects using Go modules.
 *
 * <p>Extends the generic {@link ProjectTools} with Go-specific metadata by
 * inspecting {@code go.mod}. It implements {@link BuildMetadataResolver} so
 * that {@link ProjectMetadataInfo} can obtain the project type and name
 * without parsing build files directly.</p>
 */
public class GoProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code module github.com/user/repo} in {@code go.mod}. */
    private static final Pattern GO_MODULE =
            Pattern.compile("(?m)^module\\s+(\\S+)");

    /** Matches {@code go 1.21} in {@code go.mod}. */
    private static final Pattern GO_VERSION =
            Pattern.compile("(?m)^go\\s+([\\d.]+)");

    private final FileObject goMod;

    /** Lazily read content of {@code go.mod}; {@code null} until first access. */
    private String goModContent;
    private boolean contentRead = false;

    public GoProjectTools(final Project project) throws IOException {
        super(project);
        this.goMod = project.getProjectDirectory().getFileObject("go.mod");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the Go project: module path, Go version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Go project info: " + project());
        return appendSourceDirs(ProjectMetadataInfo.get(project(), this));
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
        final Matcher m = GO_MODULE.matcher(content);
        if (!m.find()) {
            return null;
        }
        // Use the last path component of the module path as the display name
        final String module = m.group(1);
        final int slash = module.lastIndexOf('/');
        return slash >= 0 ? module.substring(slash + 1) : module;
    }

    @Override
    public String getProjectType() {
        return "go";
    }

    @Override
    public String getBuildFileName() {
        return "go.mod";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final String content = content();
        if (content == null) {
            return metadata;
        }
        final Matcher module = GO_MODULE.matcher(content);
        if (module.find()) {
            metadata.put("Module", module.group(1));
        }
        final Matcher version = GO_VERSION.matcher(content);
        if (version.find()) {
            metadata.put("Go Version", version.group(1));
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of Go module dependencies from go.mod (require directives), one per line as module version"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from go.mod");
        final String content = content();
        if (content == null) {
            return "Unable to read go.mod";
        }
        final StringBuilder sb = new StringBuilder();
        boolean inRequireBlock = false;
        for (final String line : content.split("\\R")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("require (")) {
                inRequireBlock = true;
                continue;
            }
            if (trimmed.startsWith("require ") && !trimmed.contains("(")) {
                // single-line require directive: require module version
                sb.append(trimmed.substring("require ".length()).trim()).append('\n');
                continue;
            }
            if (inRequireBlock) {
                if (trimmed.equals(")")) {
                    inRequireBlock = false;
                    continue;
                }
                if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                    sb.append(trimmed).append('\n');
                }
            }
        }
        return sb.length() == 0
                ? "No require directives found in go.mod"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (goMod != null) {
                try {
                    goModContent = goMod.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read go.mod", ex);
                }
            }
        }
        return goModContent;
    }
}

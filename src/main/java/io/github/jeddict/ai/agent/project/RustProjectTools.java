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
 * Project-tool specialisation for Rust projects managed with Cargo.
 *
 * <p>Extends the generic {@link ProjectTools} with Rust-specific metadata by
 * inspecting {@code Cargo.toml}. It implements {@link BuildMetadataResolver}
 * so that {@link ProjectMetadataInfo} can obtain the project type and name
 * without parsing build files directly.</p>
 */
public class RustProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code name = "my-crate"} in the {@code [package]} section. */
    private static final Pattern CARGO_NAME =
            Pattern.compile("(?m)^name\\s*=\\s*\"([^\"]+)\"");

    /** Matches {@code edition = "2021"} in {@code Cargo.toml}. */
    private static final Pattern CARGO_EDITION =
            Pattern.compile("(?m)^edition\\s*=\\s*\"([^\"]+)\"");

    /** Matches {@code rust-version = "1.70"} in {@code Cargo.toml}. */
    private static final Pattern CARGO_RUST_VERSION =
            Pattern.compile("(?m)^rust-version\\s*=\\s*\"([^\"]+)\"");

    /** Matches the start of the {@code [dependencies]} section. */
    private static final Pattern CARGO_DEPS_SECTION =
            Pattern.compile("(?m)^\\[dependencies\\]");

    private final FileObject cargoToml;

    /** Lazily read content of {@code Cargo.toml}; {@code null} until first access. */
    private String cargoContent;
    private boolean contentRead = false;

    public RustProjectTools(final Project project) throws IOException {
        super(project);
        this.cargoToml = project.getProjectDirectory().getFileObject("Cargo.toml");
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the Rust project: crate name, edition, rust-version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Rust project info: " + project());
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
        final Matcher m = CARGO_NAME.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public String getProjectType() {
        return "rust";
    }

    @Override
    public String getBuildFileName() {
        return "Cargo.toml";
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final String content = content();
        if (content == null) {
            return metadata;
        }
        Matcher m = CARGO_EDITION.matcher(content);
        if (m.find()) {
            metadata.put("Edition", m.group(1));
        }
        m = CARGO_RUST_VERSION.matcher(content);
        if (m.find()) {
            metadata.put("Rust Version", m.group(1));
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of dependencies declared in the [dependencies] section of Cargo.toml, one per line"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from Cargo.toml");
        final String content = content();
        if (content == null) {
            return "Unable to read Cargo.toml";
        }
        final Matcher sectionMatcher = CARGO_DEPS_SECTION.matcher(content);
        if (!sectionMatcher.find()) {
            return "No [dependencies] section found in Cargo.toml";
        }
        final String afterSection = content.substring(sectionMatcher.end());
        final StringBuilder sb = new StringBuilder();
        for (final String line : afterSection.split("\\R")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("[")) {
                break; // start of next section
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            sb.append(trimmed).append('\n');
        }
        return sb.length() == 0
                ? "No dependencies listed in [dependencies]"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (cargoToml != null) {
                try {
                    cargoContent = cargoToml.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read Cargo.toml", ex);
                }
            }
        }
        return cargoContent;
    }
}

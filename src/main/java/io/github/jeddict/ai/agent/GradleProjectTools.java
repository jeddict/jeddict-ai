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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import org.openide.filesystems.FileObject;

/**
 * Project-tool specialisation for Gradle projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Gradle-specific metadata
 * (currently: JDK/compiler version) by inspecting the {@code build.gradle} or
 * {@code build.gradle.kts} file.  It also implements
 * {@link BuildMetadataResolver} so that {@link ProjectMetadataInfo} can obtain
 * this data without parsing build files directly.</p>
 */
public class GradleProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code sourceCompatibility = '11'} / {@code = JavaVersion.VERSION_11} etc. */
    private static final Pattern SOURCE_COMPAT =
            Pattern.compile("sourceCompatibility\\s*=\\s*['\"]?([\\w.]+)['\"]?");
    /** Matches the Kotlin-DSL style {@code jvmTarget = "17"}. */
    private static final Pattern JVM_TARGET =
            Pattern.compile("jvmTarget\\s*=\\s*['\"]([\\w.]+)['\"]");

    private final FileObject gradleFile;

    /** Lazily read content of the build file; {@code null} until first access. */
    private String buildFileContent;
    private boolean contentRead = false;

    public GradleProjectTools(final Project project) throws IOException {
        super(project);
        final FileObject dir = project.getProjectDirectory();
        FileObject gf = dir.getFileObject("build.gradle");
        if (gf == null) {
            gf = dir.getFileObject("build.gradle.kts");
        }
        this.gradleFile = gf;
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: jdk version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Gradle project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        // Gradle does not have a canonical project name in the build file in a
        // predictable location; return null so the directory name is used.
        return null;
    }

    @Override
    public String getEeVersion() {
        // Gradle projects typically declare their EE dependency in a variety of
        // ways (plugins, BOM imports, etc.).  A robust extraction would require
        // a full Groovy/Kotlin parser, so we intentionally return null here and
        // let the LLM call the dedicated tool if it needs this information.
        return null;
    }

    @Override
    public String getJdkVersion() {
        final String content = content();
        if (content == null) {
            return null;
        }
        Matcher m = SOURCE_COMPAT.matcher(content);
        if (m.find()) {
            return normalizeVersion(m.group(1));
        }
        m = JVM_TARGET.matcher(content);
        if (m.find()) {
            return normalizeVersion(m.group(1));
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Tool(
        name = "jdkVersion",
        value = "Return the Java source compatibility version configured in this Gradle project's build file"
    )
    @ToolPolicy(READONLY)
    public String jdkVersion() throws Exception {
        progress("Reading JDK version from Gradle build file");
        final String v = getJdkVersion();
        return v != null ? v : "No sourceCompatibility / jvmTarget found in Gradle build file";
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (gradleFile != null) {
                try {
                    buildFileContent = gradleFile.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read Gradle build file", ex);
                }
            }
        }
        return buildFileContent;
    }

    /**
     * Normalises a Gradle version token to a plain numeric string.
     * {@code "VERSION_11"}, {@code "JavaVersion.VERSION_11"}, {@code "11"},
     * {@code "'11'"} are all turned into {@code "11"}.
     */
    private static String normalizeVersion(String raw) {
        if (raw == null) return null;
        // Strip JavaVersion. prefix
        raw = raw.replaceFirst("(?i)JavaVersion\\.VERSION_", "").replaceFirst("(?i)VERSION_", "");
        // Strip surrounding quotes
        raw = raw.replaceAll("['\"]", "").trim();
        return raw.isEmpty() ? null : raw;
    }
}

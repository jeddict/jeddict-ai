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
 * Project-tool specialisation for Gradle projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Gradle-specific metadata
 * (currently: JDK/compiler version) by inspecting the {@code build.gradle} or
 * {@code build.gradle.kts} file.  It also implements
 * {@link BuildMetadataResolver} so that {@link ProjectMetadataInfo} can obtain
 * this data without parsing build files directly.</p>
 */
public class GradleProjectTools extends JvmProjectTools implements BuildMetadataResolver {

    /**
     * Matches Groovy DSL dependency declarations (unparenthesised):
     * <ul>
     *   <li>{@code implementation 'com.example:lib:1.0'}</li>
     *   <li>{@code implementation "com.example:lib:1.0"}</li>
     * </ul>
     */
    private static final Pattern DEP_GROOVY =
            Pattern.compile("^\\s*(\\w+)\\s+[\"']([^\"']+)[\"']", Pattern.MULTILINE);

    /**
     * Matches Kotlin DSL dependency declarations (parenthesised):
     * <ul>
     *   <li>{@code implementation("com.example:lib:1.0")}</li>
     *   <li>{@code implementation('com.example:lib:1.0')}</li>
     * </ul>
     */
    private static final Pattern DEP_KOTLIN =
            Pattern.compile("^\\s*(\\w+)\\s*\\([\"']([^\"']+)[\"']\\)", Pattern.MULTILINE);

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
    public String getProjectType() {
        return "gradle";
    }

    @Override
    public String getBuildFileName() {
        return gradleFile != null ? gradleFile.getNameExt() : null;
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final String jdkVersion = getJdkVersion();
        if (jdkVersion != null) {
            metadata.put("Java Version", jdkVersion);
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
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

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the list of dependencies declared in this Gradle project's build file, one per line as configuration groupId:artifactId:version"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from Gradle build file");
        final String content = content();
        if (content == null) {
            return "Unable to read Gradle build file";
        }
        final StringBuilder sb = new StringBuilder();
        collectDeps(sb, DEP_GROOVY.matcher(content));
        collectDeps(sb, DEP_KOTLIN.matcher(content));
        return sb.length() == 0
                ? "No dependencies declared in Gradle build file"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String getJdkVersion() {
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

    /**
     * Appends entries matched by {@code matcher} to {@code sb}.
     * Only coordinates containing at least one {@code :} (i.e. real
     * {@code groupId:artifactId[:version]} strings) are included so that
     * unrelated string literals are silently skipped.
     */
    private static void collectDeps(final StringBuilder sb, final Matcher matcher) {
        while (matcher.find()) {
            final String coord = matcher.group(2);
            if (coord.contains(":")) {
                sb.append(matcher.group(1)).append(' ').append(coord).append('\n');
            }
        }
    }
}

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
 * Project-tool specialisation for Makefile and CMake projects (C/C++).
 *
 * <p>Extends the generic {@link ProjectTools} for projects that use a
 * {@code CMakeLists.txt} or a plain {@code Makefile} / {@code makefile}.
 * CMake projects provide richer metadata (project name, minimum CMake version,
 * and {@code find_package} dependencies); plain Makefiles provide only the
 * project type label.</p>
 *
 * <p>It implements {@link BuildMetadataResolver} so that
 * {@link ProjectMetadataInfo} can obtain the project type without parsing
 * build files directly.</p>
 */
public class MakefileProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code cmake_minimum_required(VERSION 3.10)} or {@code cmake_minimum_required(VERSION 3.10 FATAL_ERROR)}. */
    private static final Pattern CMAKE_MIN_VERSION =
            Pattern.compile("cmake_minimum_required\\s*\\(\\s*VERSION\\s+([\\d.]+)",
                    Pattern.CASE_INSENSITIVE);

    /** Matches the first {@code project(MyProject ...)} call; captures the project name. */
    private static final Pattern CMAKE_PROJECT_NAME =
            Pattern.compile("\\bproject\\s*\\(\\s*(\\S+)", Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code find_package(OpenCV REQUIRED)} and optionally a version:
     * {@code find_package(Boost 1.70 COMPONENTS ...)}.
     */
    private static final Pattern CMAKE_FIND_PACKAGE =
            Pattern.compile("find_package\\s*\\(\\s*(\\S+)(?:\\s+([\\d.]+))?",
                    Pattern.CASE_INSENSITIVE);

    private final FileObject buildFile;
    private final String buildFileName;

    /** Lazily read content of the build file; {@code null} until first access. */
    private String buildFileContent;
    private boolean contentRead = false;

    public MakefileProjectTools(final Project project) throws IOException {
        super(project);
        final FileObject dir = project.getProjectDirectory();
        FileObject found = dir.getFileObject("CMakeLists.txt");
        if (found == null) {
            found = dir.getFileObject("Makefile");
        }
        if (found == null) {
            found = dir.getFileObject("makefile");
        }
        this.buildFile = found;
        this.buildFileName = (found != null) ? found.getNameExt() : null;
    }

    // -----------------------------------------------------------------------
    // ProjectTools overrides
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectInfo",
        value = "Return information about the project: cmake version, project name"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Makefile/CMake project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        if (!"CMakeLists.txt".equals(buildFileName)) {
            return null;
        }
        final String content = content();
        if (content == null) {
            return null;
        }
        final Matcher m = CMAKE_PROJECT_NAME.matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    @Override
    public String getProjectType() {
        return "CMakeLists.txt".equals(buildFileName) ? "cmake" : "makefile";
    }

    @Override
    public String getBuildFileName() {
        return buildFileName;
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        if (!"CMakeLists.txt".equals(buildFileName)) {
            return metadata;
        }
        final String content = content();
        if (content == null) {
            return metadata;
        }
        final Matcher m = CMAKE_MIN_VERSION.matcher(content);
        if (m.find()) {
            metadata.put("CMake Min Version", m.group(1));
        }
        return metadata;
    }

    // -----------------------------------------------------------------------
    // Additional @Tool methods exposed to the LLM agent
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "projectDependencies",
        value = "Return the packages found via find_package() in CMakeLists.txt, one per line as PackageName [version]"
    )
    @ToolPolicy(READONLY)
    public String projectDependencies() throws Exception {
        progress("Reading dependencies from " + (buildFileName != null ? buildFileName : "build file"));
        if (!"CMakeLists.txt".equals(buildFileName)) {
            return "Dependency information is not available for plain Makefile projects";
        }
        final String content = content();
        if (content == null) {
            return "Unable to read CMakeLists.txt";
        }
        final Matcher m = CMAKE_FIND_PACKAGE.matcher(content);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1));
            if (m.group(2) != null) {
                sb.append(' ').append(m.group(2));
            }
            sb.append('\n');
        }
        return sb.length() == 0
                ? "No find_package() calls found in CMakeLists.txt"
                : sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String content() {
        if (!contentRead) {
            contentRead = true;
            if (buildFile != null) {
                try {
                    buildFileContent = buildFile.asText();
                } catch (IOException ex) {
                    log.log(Level.WARNING, "Failed to read " + buildFileName, ex);
                }
            }
        }
        return buildFileContent;
    }
}

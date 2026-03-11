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
 * Project-tool specialisation for Python projects.
 *
 * <p>Extends the generic {@link ProjectTools} with Python-specific metadata
 * by inspecting {@code pyproject.toml}, {@code setup.py}, or
 * {@code requirements.txt} (whichever is present, in that order of
 * preference). It implements {@link BuildMetadataResolver} so that
 * {@link ProjectMetadataInfo} can obtain the project type and name without
 * parsing build files directly.</p>
 */
public class PythonProjectTools extends ProjectTools implements BuildMetadataResolver {

    /** Matches {@code name = "my-package"} in pyproject.toml. */
    private static final Pattern PYPROJECT_NAME =
            Pattern.compile("(?m)^name\\s*=\\s*['\"]([^'\"]+)['\"]");
    /** Matches {@code requires-python = ">=3.9"} or {@code requires_python = ">=3.9"} in pyproject.toml. */
    private static final Pattern PYPROJECT_PYTHON =
            Pattern.compile("(?m)requires[-_]python\\s*=\\s*['\"]([^'\"]+)['\"]");
    /** Matches {@code python_requires='>=3.9'} in setup.py. */
    private static final Pattern SETUP_PYTHON =
            Pattern.compile("python_requires\\s*=\\s*['\"]([^'\"]+)['\"]");

    private final FileObject buildFile;
    private final String buildFileName;

    /** Lazily read content of the build file; {@code null} until first access. */
    private String buildFileContent;
    private boolean contentRead = false;

    public PythonProjectTools(final Project project) throws IOException {
        super(project);
        final FileObject dir = project.getProjectDirectory();
        FileObject found = dir.getFileObject("pyproject.toml");
        if (found == null) {
            found = dir.getFileObject("setup.py");
        }
        if (found == null) {
            found = dir.getFileObject("requirements.txt");
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
        value = "Return information about the project: python version, package name"
    )
    @ToolPolicy(READONLY)
    public String projectInfo() throws Exception {
        progress("Gathering Python project info: " + project());
        return ProjectMetadataInfo.get(project(), this);
    }

    // -----------------------------------------------------------------------
    // BuildMetadataResolver implementation
    // -----------------------------------------------------------------------

    @Override
    public String getProjectName() {
        final String content = content();
        if (content == null || buildFileName == null) {
            return null;
        }
        if ("pyproject.toml".equals(buildFileName)) {
            final Matcher m = PYPROJECT_NAME.matcher(content);
            return m.find() ? m.group(1) : null;
        }
        return null;
    }

    @Override
    public String getProjectType() {
        return "python";
    }

    @Override
    public String getBuildFileName() {
        return buildFileName;
    }

    @Override
    public Map<String, String> getProjectMetadata() {
        final Map<String, String> metadata = new LinkedHashMap<>();
        final String content = content();
        if (content == null || buildFileName == null) {
            return metadata;
        }
        if ("pyproject.toml".equals(buildFileName)) {
            final Matcher m = PYPROJECT_PYTHON.matcher(content);
            if (m.find()) {
                metadata.put("Python Version", m.group(1));
            }
        } else if ("setup.py".equals(buildFileName)) {
            final Matcher m = SETUP_PYTHON.matcher(content);
            if (m.find()) {
                metadata.put("Python Version", m.group(1));
            }
        }
        return metadata;
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

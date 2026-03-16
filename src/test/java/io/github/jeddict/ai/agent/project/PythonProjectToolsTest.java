/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Files;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

@CacioTest
public class PythonProjectToolsTest extends TestBase {

    private static final String PYPROJECT_TOML_CONTENT =
        """
        [project]
        name = "my-package"
        requires-python = ">=3.11"

        [project.dependencies]
        requests = ">=2.28"
        click = ">=8.0"
        """;

    private static final String REQUIREMENTS_TXT_CONTENT =
        """
        # Production dependencies
        requests>=2.28.0
        click>=8.0.0
        """;

    private static final String SETUP_PY_CONTENT =
        """
        from setuptools import setup
        setup(
            name="my-package",
            python_requires=">=3.9",
        )
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_python()
    throws Exception {
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("python");
    }

    @Test
    public void getBuildFileName_returns_pyproject_toml_when_present()
    throws Exception {
        Files.writeString(projectPath.resolve("pyproject.toml"), PYPROJECT_TOML_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("pyproject.toml");
    }

    @Test
    public void getBuildFileName_returns_requirements_txt_as_fallback()
    throws Exception {
        Files.writeString(projectPath.resolve("requirements.txt"), REQUIREMENTS_TXT_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("requirements.txt");
    }

    @Test
    public void getBuildFileName_returns_null_when_no_python_files()
    throws Exception {
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isNull();
    }

    @Test
    public void getProjectName_returns_name_from_pyproject_toml()
    throws Exception {
        Files.writeString(projectPath.resolve("pyproject.toml"), PYPROJECT_TOML_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("my-package");
    }

    @Test
    public void getProjectName_returns_null_for_requirements_txt()
    throws Exception {
        Files.writeString(projectPath.resolve("requirements.txt"), REQUIREMENTS_TXT_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectName_returns_null_when_no_python_files()
    throws Exception {
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_python_version_from_pyproject_toml()
    throws Exception {
        Files.writeString(projectPath.resolve("pyproject.toml"), PYPROJECT_TOML_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Python Version", ">=3.11");
    }

    @Test
    public void getProjectMetadata_includes_python_version_from_setup_py()
    throws Exception {
        Files.writeString(projectPath.resolve("setup.py"), SETUP_PY_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Python Version", ">=3.9");
    }

    @Test
    public void getProjectMetadata_returns_empty_when_no_python_files()
    throws Exception {
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_parses_pyproject_toml_deps()
    throws Exception {
        Files.writeString(projectPath.resolve("pyproject.toml"), PYPROJECT_TOML_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("requests");
        then(deps).contains("click");
    }

    @Test
    public void projectDependencies_parses_requirements_txt()
    throws Exception {
        Files.writeString(projectPath.resolve("requirements.txt"), REQUIREMENTS_TXT_CONTENT);
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("requests>=2.28.0");
        then(deps).contains("click>=8.0.0");
        then(deps).doesNotContain("#");
    }

    @Test
    public void projectDependencies_returns_message_when_no_python_files()
    throws Exception {
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read Python build file");
    }

    @Test
    public void projectDependencies_returns_message_when_pyproject_has_no_deps_section()
    throws Exception {
        Files.writeString(projectPath.resolve("pyproject.toml"),
            "[project]\nname = \"empty\"\n");
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No [project.dependencies]");
    }

    @Test
    public void projectDependencies_returns_message_for_empty_requirements_txt()
    throws Exception {
        Files.writeString(projectPath.resolve("requirements.txt"), "# just a comment\n");
        final PythonProjectTools tool = new PythonProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No dependencies declared in requirements.txt");
    }
}

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
public class MakefileProjectToolsTest extends TestBase {

    private static final String CMAKE_CONTENT =
        """
        cmake_minimum_required(VERSION 3.16)
        project(MyProject)
        find_package(OpenCV 4.5 REQUIRED)
        find_package(Boost REQUIRED)
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver – CMake
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_cmake_for_cmake_lists()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"), CMAKE_CONTENT);
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("cmake");
    }

    @Test
    public void getProjectType_returns_makefile_for_makefile()
    throws Exception {
        Files.writeString(projectPath.resolve("Makefile"), "all:\n\techo hello\n");
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("makefile");
    }

    @Test
    public void getBuildFileName_returns_cmake_lists_txt()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"), CMAKE_CONTENT);
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("CMakeLists.txt");
    }

    @Test
    public void getProjectName_returns_cmake_project_name()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"), CMAKE_CONTENT);
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("MyProject");
    }

    @Test
    public void getProjectName_returns_null_for_plain_makefile()
    throws Exception {
        Files.writeString(projectPath.resolve("Makefile"), "all:\n\techo hello\n");
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectName_returns_null_when_no_build_file()
    throws Exception {
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_cmake_min_version()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"), CMAKE_CONTENT);
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("CMake Min Version", "3.16");
    }

    @Test
    public void getProjectMetadata_returns_empty_for_plain_makefile()
    throws Exception {
        Files.writeString(projectPath.resolve("Makefile"), "all:\n\techo hello\n");
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_lists_find_package_calls()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"), CMAKE_CONTENT);
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("OpenCV 4.5");
        then(deps).contains("Boost");
    }

    @Test
    public void projectDependencies_returns_message_for_plain_makefile()
    throws Exception {
        Files.writeString(projectPath.resolve("Makefile"), "all:\n\techo hello\n");
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("not available for plain Makefile");
    }

    @Test
    public void projectDependencies_returns_message_when_no_find_package_calls()
    throws Exception {
        Files.writeString(projectPath.resolve("CMakeLists.txt"),
            "cmake_minimum_required(VERSION 3.10)\nproject(Empty)\n");
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No find_package()");
    }

    @Test
    public void projectDependencies_returns_message_when_no_build_file()
    throws Exception {
        final MakefileProjectTools tool = new MakefileProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("not available for plain Makefile");
    }
}

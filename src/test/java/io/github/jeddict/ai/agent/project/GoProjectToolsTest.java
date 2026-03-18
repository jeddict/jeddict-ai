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
public class GoProjectToolsTest extends TestBase {

    private static final String GO_MOD_CONTENT =
        """
        module github.com/example/myapp

        go 1.21

        require (
            github.com/gin-gonic/gin v1.9.1
            github.com/stretchr/testify v1.8.4
        )
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_go()
    throws Exception {
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("go");
    }

    @Test
    public void getBuildFileName_returns_go_mod()
    throws Exception {
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("go.mod");
    }

    @Test
    public void getProjectName_returns_last_path_segment_of_module()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"), GO_MOD_CONTENT);
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("myapp");
    }

    @Test
    public void getProjectName_returns_module_name_when_no_slash()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"), "module myapp\ngo 1.21\n");
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("myapp");
    }

    @Test
    public void getProjectName_returns_null_when_no_go_mod()
    throws Exception {
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_module_and_go_version()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"), GO_MOD_CONTENT);
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        final java.util.Map<String, String> meta = tool.getProjectMetadata();
        then(meta).containsEntry("Module", "github.com/example/myapp");
        then(meta).containsEntry("Go Version", "1.21");
    }

    @Test
    public void getProjectMetadata_returns_empty_map_when_no_go_mod()
    throws Exception {
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_returns_require_block_contents()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"), GO_MOD_CONTENT);
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("github.com/gin-gonic/gin v1.9.1");
        then(deps).contains("github.com/stretchr/testify v1.8.4");
    }

    @Test
    public void projectDependencies_handles_single_line_require()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"),
            "module myapp\ngo 1.21\nrequire github.com/pkg/errors v0.9.1\n");
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("github.com/pkg/errors v0.9.1");
    }

    @Test
    public void projectDependencies_returns_message_when_no_require_directives()
    throws Exception {
        Files.writeString(projectPath.resolve("go.mod"), "module myapp\ngo 1.21\n");
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No require directives found");
    }

    @Test
    public void projectDependencies_returns_message_when_no_go_mod()
    throws Exception {
        final GoProjectTools tool = new GoProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read go.mod");
    }
}

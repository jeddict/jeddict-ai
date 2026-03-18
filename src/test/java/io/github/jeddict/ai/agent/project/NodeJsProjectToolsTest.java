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
public class NodeJsProjectToolsTest extends TestBase {

    private static final String PACKAGE_JSON_CONTENT =
        """
        {
          "name": "my-app",
          "version": "1.2.3",
          "engines": { "node": ">=18" },
          "dependencies": {
            "express": "^4.18.2"
          },
          "devDependencies": {
            "jest": "^29.0.0"
          }
        }
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_nodejs()
    throws Exception {
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("nodejs");
    }

    @Test
    public void getBuildFileName_returns_package_json()
    throws Exception {
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("package.json");
    }

    @Test
    public void getProjectName_returns_name_from_package_json()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("my-app");
    }

    @Test
    public void getProjectName_returns_null_when_no_package_json()
    throws Exception {
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_node_version_and_package_version()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        final java.util.Map<String, String> meta = tool.getProjectMetadata();
        then(meta).containsEntry("Node Version", ">=18");
        then(meta).containsEntry("Package Version", "1.2.3");
    }

    @Test
    public void getProjectMetadata_returns_empty_when_no_package_json()
    throws Exception {
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_lists_deps_and_dev_deps()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("express");
        then(deps).contains("dependency");
        then(deps).contains("jest");
        then(deps).contains("devDependency");
    }

    @Test
    public void projectDependencies_returns_message_when_no_package_json()
    throws Exception {
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read package.json");
    }

    @Test
    public void projectDependencies_returns_message_when_no_deps_declared()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"),
            "{\"name\": \"empty\", \"version\": \"1.0.0\"}");
        final NodeJsProjectTools tool = new NodeJsProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No dependencies declared");
    }
}

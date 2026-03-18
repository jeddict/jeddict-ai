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
public class FrontendProjectToolsTest extends TestBase {

    private static final String PACKAGE_JSON_CONTENT =
        """
        {
          "name": "my-frontend",
          "version": "1.0.0",
          "dependencies": {
            "react": "^18.0.0"
          }
        }
        """;

    private static final String ANGULAR_PACKAGE_JSON =
        """
        {
          "name": "my-angular-app",
          "version": "0.0.0"
        }
        """;

    // -----------------------------------------------------------------------
    // Framework detection via config files
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_angular_for_angular_json()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), ANGULAR_PACKAGE_JSON);
        Files.writeString(projectPath.resolve("angular.json"), "{}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("angular");
    }

    @Test
    public void getProjectType_returns_vite_for_vite_config()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        Files.writeString(projectPath.resolve("vite.config.js"), "export default {}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("vite");
    }

    @Test
    public void getProjectType_returns_webpack_for_webpack_config()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        Files.writeString(projectPath.resolve("webpack.config.js"), "module.exports = {}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("webpack");
    }

    // -----------------------------------------------------------------------
    // Framework detection via package.json dependencies
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_react_when_react_in_deps()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("react");
    }

    @Test
    public void getProjectType_returns_vue_when_vue_in_deps()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"),
            "{\"name\": \"my-vue-app\", \"dependencies\": {\"vue\": \"^3.0.0\"}}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("vue");
    }

    @Test
    public void getProjectType_returns_frontend_when_no_known_framework()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"),
            "{\"name\": \"my-app\"}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("frontend");
    }

    // -----------------------------------------------------------------------
    // getProjectMetadata
    // -----------------------------------------------------------------------

    @Test
    public void getProjectMetadata_includes_framework()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), ANGULAR_PACKAGE_JSON);
        Files.writeString(projectPath.resolve("angular.json"), "{}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Framework", "angular");
    }

    // -----------------------------------------------------------------------
    // next.js detection
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_next_js_for_next_config()
    throws Exception {
        Files.writeString(projectPath.resolve("package.json"), PACKAGE_JSON_CONTENT);
        Files.writeString(projectPath.resolve("next.config.js"), "module.exports = {}");
        final FrontendProjectTools tool = new FrontendProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("next.js");
    }
}

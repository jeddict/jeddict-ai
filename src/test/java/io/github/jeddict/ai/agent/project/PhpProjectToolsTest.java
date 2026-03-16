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
public class PhpProjectToolsTest extends TestBase {

    private static final String COMPOSER_JSON_CONTENT =
        """
        {
          "name": "vendor/my-package",
          "version": "2.1.0",
          "require": {
            "php": ">=8.1",
            "symfony/http-foundation": "^6.0"
          },
          "require-dev": {
            "phpunit/phpunit": "^10.0"
          }
        }
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_php()
    throws Exception {
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("php");
    }

    @Test
    public void getBuildFileName_returns_composer_json()
    throws Exception {
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("composer.json");
    }

    @Test
    public void getProjectName_returns_name_from_composer_json()
    throws Exception {
        Files.writeString(projectPath.resolve("composer.json"), COMPOSER_JSON_CONTENT);
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("vendor/my-package");
    }

    @Test
    public void getProjectName_returns_null_when_no_composer_json()
    throws Exception {
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_php_version_and_package_version()
    throws Exception {
        Files.writeString(projectPath.resolve("composer.json"), COMPOSER_JSON_CONTENT);
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        final java.util.Map<String, String> meta = tool.getProjectMetadata();
        then(meta).containsEntry("PHP Version", ">=8.1");
        then(meta).containsEntry("Package Version", "2.1.0");
    }

    @Test
    public void getProjectMetadata_returns_empty_when_no_composer_json()
    throws Exception {
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_lists_require_and_require_dev()
    throws Exception {
        Files.writeString(projectPath.resolve("composer.json"), COMPOSER_JSON_CONTENT);
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("symfony/http-foundation");
        then(deps).contains("require");
        then(deps).contains("phpunit/phpunit");
        then(deps).contains("require-dev");
    }

    @Test
    public void projectDependencies_returns_message_when_no_composer_json()
    throws Exception {
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read composer.json");
    }

    @Test
    public void projectDependencies_returns_message_when_no_deps()
    throws Exception {
        Files.writeString(projectPath.resolve("composer.json"),
            "{\"name\": \"vendor/empty\"}");
        final PhpProjectTools tool = new PhpProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No dependencies declared");
    }
}

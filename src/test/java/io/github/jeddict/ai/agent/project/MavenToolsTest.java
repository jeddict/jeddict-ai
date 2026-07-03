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
import io.github.jeddict.ai.agent.MavenTools;
import io.github.jeddict.ai.test.DummyProject;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

@CacioTest
public class MavenToolsTest extends TestBase {

    @Test
    public void add_dependency() throws Exception {
        final Path template = Paths.get("src", "test", "projects", "minimal");
        FileUtils.copyDirectory(template.toFile(), projectPath.toFile());

        final DummyProject dummyProject = new DummyProject(projectPath);
        final MavenTools tools = new MavenTools(dummyProject);

        final String result = tools.addDependency("org.junit.jupiter", "junit-jupiter", "5.10.2", "", "", "");

        then(result).isEqualTo("Dependency added successfully to pom.xml");
        then(Files.readString(projectPath.resolve("pom.xml")))
            .contains("<groupId>org.junit.jupiter</groupId>")
            .contains("<artifactId>junit-jupiter</artifactId>")
            .contains("<version>5.10.2</version>");
    }
}

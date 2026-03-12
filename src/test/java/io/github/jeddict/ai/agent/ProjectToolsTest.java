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
package io.github.jeddict.ai.agent;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

@CacioTest
public class ProjectToolsTest extends TestBase {

    @Test
    public void projectInfo_returns_project_metadata_as_text()
    throws Exception {
        Path homePath = Paths.get(".").toAbsolutePath().normalize();
        
        String projectDir = homePath.resolve("src/test/projects/minimal").toString();
        ProjectTools tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectInfo()).isEqualToIgnoringNewLines(
            """
            - name: name
            - folder: %s
            - type: maven
            - Source Directory: src/main/java
            - Test Source Directory: src/test/java
            """.formatted(projectDir)
        );

        
        projectDir = homePath.resolve("src/test/projects/jdk").toString();
        tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectInfo()).isEqualToIgnoringNewLines(
            """
            - name: jdk
            - folder: %s
            - type: maven
            - Java Version: 11
            - Source Directory: src/main/java
            - Test Source Directory: src/test/java
            """.formatted(projectDir)
        );

        projectDir = homePath.resolve("src/test/projects/jakarta").toString();
        tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectInfo()).isEqualToIgnoringNewLines(
            """
            - name: jakarta
            - folder: %s
            - type: maven
            - EE Version: jakarta
            - EE Import Prefix: jakarta
            - Java Version: 21
            - Source Directory: src/main/java
            - Test Source Directory: src/test/java
            """.formatted(projectDir)
        );

        projectDir = homePath.resolve("src/test/projects/javax").toString();
        tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectInfo()).isEqualToIgnoringNewLines(
            """
            - name: javax
            - folder: %s
            - type: maven
            - EE Version: javax
            - EE Import Prefix: javax
            - Java Version: 11
            - Source Directory: src/main/java
            - Test Source Directory: src/test/java
            """.formatted(projectDir)
        );
    }

    @Test
    public void projectSrcDir_returns_relative_path()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String projectDir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectSrcDir()).isEqualTo("src/main/java");
    }

    @Test
    public void projectSrcResourceDir_returns_relative_path()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String projectDir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectSrcResourceDir()).isEqualTo("src/main/resources");
    }

    @Test
    public void projectTestDir_returns_relative_path()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String projectDir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectTestDir()).isEqualTo("src/test/java");
    }

    @Test
    public void projectTestResourceDir_returns_relative_path()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String projectDir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tools = ProjectTools.forProject(project(projectDir));
        then(tools.projectTestResourceDir()).isEqualTo("src/test/resources");
    }

}

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
package io.github.jeddict.ai.scanner;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.agent.project.MavenProjectTools;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo.BuildMetadataResolver;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;

/**
 *
 */
@CacioTest
public class ProjectMetadataInfoTest extends TestBase {

    @Test
    public void get_returns_basic_info_for_maven_project() throws Exception {
        final Project project = project(projectDir);

        // Generic path: no BuildMetadataResolver → name comes from the project
        // directory; type, EE/JDK fields are absent (they require MavenProjectTools).
        final String info = ProjectMetadataInfo.get(project);
        then(info)
            .contains("- folder: " + Paths.get(project.getProjectDirectory().getPath()))
            .doesNotContain("- type:")
            .doesNotContain("- Source Directory:")
            .doesNotContain("- Test Source Directory:")
            .doesNotContain("- EE Version:")
            .doesNotContain("- Java Version:");
    }

    @Test
    public void get_with_resolver_returns_maven_metadata() throws Exception {
        final Project project = project(projectDir);

        // MavenProjectTools acts as the BuildMetadataResolver: reads pom.xml.
        final BuildMetadataResolver resolver = new MavenProjectTools(project);
        final String info = ProjectMetadataInfo.get(project, resolver);
        then(info)
            .contains("- name: name")        // <name> from pom.xml
            .contains("- folder: " + Paths.get(project.getProjectDirectory().getPath()))
            .contains("- type: maven")
            .doesNotContain("- Source Directory:")
            .doesNotContain("- Test Source Directory:");
    }

}

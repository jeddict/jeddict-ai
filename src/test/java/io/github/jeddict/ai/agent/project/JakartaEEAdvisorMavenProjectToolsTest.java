/**
 * Copyright 2025 the original author or authors from the Jeddict project
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
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JakartaEEAdvisorMavenProjectTools}.
 */
@CacioTest
public class JakartaEEAdvisorMavenProjectToolsTest extends TestBase {

    // -----------------------------------------------------------------------
    // resolveWrapper
    // -----------------------------------------------------------------------

    @Test
    public void resolveWrapper_uses_mvn_when_no_wrapper_present()
    throws Exception {
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveWrapper()).isEqualTo("mvn");
    }

    @Test
    public void resolveWrapper_uses_mvnw_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("mvnw"));
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveWrapper()).isEqualTo("./mvnw");
    }

    // -----------------------------------------------------------------------
    // jakartaEEAdvise — command construction
    // -----------------------------------------------------------------------

    @Test
    public void jakartaEEAdvise_uses_full_plugin_coordinates()
    throws Exception {
        then(JakartaEEAdvisorMavenProjectTools.PLUGIN)
            .isEqualTo("fish.payara.advisor:advisor-maven-plugin:2.0");
    }

    @Test
    public void jakartaEEAdvise_defaults_to_version_10_when_null()
    throws Exception {
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveAdviseCommand(null))
            .isEqualTo("mvn fish.payara.advisor:advisor-maven-plugin:2.0:advise -DadviseVersion=10");
    }

    @Test
    public void jakartaEEAdvise_defaults_to_version_10_when_blank()
    throws Exception {
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveAdviseCommand(""))
            .isEqualTo("mvn fish.payara.advisor:advisor-maven-plugin:2.0:advise -DadviseVersion=10");
    }

    @Test
    public void jakartaEEAdvise_uses_supplied_version_11()
    throws Exception {
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveAdviseCommand("11"))
            .isEqualTo("mvn fish.payara.advisor:advisor-maven-plugin:2.0:advise -DadviseVersion=11");
    }

    @Test
    public void jakartaEEAdvise_uses_mvnw_in_command_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("mvnw"));
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveAdviseCommand("10"))
            .isEqualTo("./mvnw fish.payara.advisor:advisor-maven-plugin:2.0:advise -DadviseVersion=10");
    }

    // -----------------------------------------------------------------------
    // microprofileAdvise — command construction
    // -----------------------------------------------------------------------

    @Test
    public void microprofileAdvise_defaults_to_version_6_command()
    throws Exception {
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveMicroprofileAdviseCommand(null))
            .isEqualTo("mvn fish.payara.advisor:advisor-maven-plugin:2.0:microprofile-advise -DadviseVersion=6");
    }

    @Test
    public void microprofileAdvise_uses_mvnw_in_command_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("mvnw"));
        final JakartaEEAdvisorMavenProjectTools tool =
            new JakartaEEAdvisorMavenProjectTools(projectDir);
        then(tool.resolveMicroprofileAdviseCommand("6"))
            .isEqualTo("./mvnw fish.payara.advisor:advisor-maven-plugin:2.0:microprofile-advise -DadviseVersion=6");
    }

    // -----------------------------------------------------------------------
    // Jakarta framework detection — advisor added only for jakarta projects
    // -----------------------------------------------------------------------

    @Test
    public void jakartaMavenProject_metadata_contains_jakarta_ee_version()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jakarta").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(MavenProjectTools.class);
        final java.util.Map<String, String> metadata =
            ((MavenProjectTools) tool).getProjectMetadata();
        then(metadata).containsKey("EE Version");
        then(metadata.get("EE Version")).startsWith("jakarta");
    }

    @Test
    public void nonJakartaMavenProject_metadata_does_not_contain_jakarta_ee_version()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(MavenProjectTools.class);
        final java.util.Map<String, String> metadata =
            ((MavenProjectTools) tool).getProjectMetadata();
        then(metadata).doesNotContainKey("EE Version");
    }
}

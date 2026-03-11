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
public class MavenProjectToolsTest extends TestBase {

    @Test
    public void forProject_returns_MavenProjectTools_for_maven_project()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/minimal").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(MavenProjectTools.class);
    }

    @Test
    public void projectInfo_includes_name_from_pom_xml()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/minimal").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        then(tool.projectInfo()).contains("- name: name");
    }

    @Test
    public void projectInfo_includes_jdk_version()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jdk").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        then(tool.projectInfo()).contains("- Java Version: 11");
    }

    @Test
    public void projectInfo_includes_jakarta_ee_version()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jakarta").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        final String info = tool.projectInfo();
        then(info)
            .contains("- EE Version: jakarta")
            .contains("- EE Import Prefix: jakarta")
            .contains("- Java Version: 21");
    }

    @Test
    public void projectInfo_includes_javax_ee_version()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/javax").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        final String info = tool.projectInfo();
        then(info)
            .contains("- EE Version: javax")
            .contains("- EE Import Prefix: javax")
            .contains("- Java Version: 11");
    }

    @Test
    public void mavenEeVersion_returns_jakarta_for_jakarta_project()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jakarta").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        then(tool.mavenEeVersion()).isEqualTo("jakarta");
    }

    @Test
    public void mavenJdkVersion_returns_version_from_pom_xml()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jdk").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        then(tool.mavenJdkVersion()).isEqualTo("11");
    }

    @Test
    public void mavenEeVersion_returns_message_when_no_ee_dep()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/jdk").toString();
        final MavenProjectTools tool = new MavenProjectTools(project(dir));
        then(tool.mavenEeVersion()).contains("No EE dependency");
    }

}

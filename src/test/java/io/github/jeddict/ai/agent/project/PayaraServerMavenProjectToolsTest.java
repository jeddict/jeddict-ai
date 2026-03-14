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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

@CacioTest
public class PayaraServerMavenProjectToolsTest extends TestBase {

    @Test
    public void forProject_returns_PayaraServerMavenProjectTools_for_payara_server_project()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-server").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(PayaraServerMavenProjectTools.class);
    }

    @Test
    public void payaraServerProjectTools_is_instance_of_MavenProjectTools()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-server").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(MavenProjectTools.class);
    }

    // buildProject is NOT overridden: standard mvn clean install is used

    @Test
    public void resolveBuildCommand_uses_mvn_clean_install()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-server").toString();
        final PayaraServerMavenProjectTools tool = new PayaraServerMavenProjectTools(project(dir));
        then(tool.resolveBuildCommand()).isEqualTo("mvn clean install");
    }

    @Test
    public void resolveBuildCommand_uses_mvnw_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("mvnw"));
        final PayaraServerMavenProjectTools tool = new PayaraServerMavenProjectTools(project(projectDir));
        then(tool.resolveBuildCommand()).isEqualTo("./mvnw clean install");
    }

    // runJavaClass and resolveRunCommand are NOT overridden: standard exec:java is used

    @Test
    public void resolveRunCommand_uses_standard_exec_java()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-server").toString();
        final PayaraServerMavenProjectTools tool = new PayaraServerMavenProjectTools(project(dir));
        then(tool.resolveRunCommand("com.example.Main"))
            .isEqualTo("mvn exec:java -Dexec.mainClass=com.example.Main");
    }

    @Test
    public void resolveTestCommand_uses_mvn_test()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-server").toString();
        final PayaraServerMavenProjectTools tool = new PayaraServerMavenProjectTools(project(dir));
        then(tool.resolveTestCommand()).isEqualTo("mvn test");
    }

    @Test
    public void resolveTestCommand_uses_mvnw_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("mvnw"));
        final PayaraServerMavenProjectTools tool = new PayaraServerMavenProjectTools(project(projectDir));
        then(tool.resolveTestCommand()).isEqualTo("./mvnw test");
    }

    // Payara Server-specific method names

    @Test
    public void startServer_method_exists() throws Exception {
        final Method m = PayaraServerMavenProjectTools.class.getMethod("startServer");
        then(m).isNotNull();
    }

    @Test
    public void stopServer_method_exists() throws Exception {
        final Method m = PayaraServerMavenProjectTools.class.getMethod("stopServer");
        then(m).isNotNull();
    }

    @Test
    public void devMode_method_exists() throws Exception {
        final Method m = PayaraServerMavenProjectTools.class.getMethod("devMode");
        then(m).isNotNull();
    }

    @Test
    public void bundleMicro_method_does_not_exist() {
        then(PayaraServerMavenProjectTools.class.getMethods())
            .extracting(Method::getName)
            .doesNotContain("bundleMicro");
    }

    @Test
    public void stopMicro_method_does_not_exist() {
        then(PayaraServerMavenProjectTools.class.getMethods())
            .extracting(Method::getName)
            .doesNotContain("stopMicro");
    }
}

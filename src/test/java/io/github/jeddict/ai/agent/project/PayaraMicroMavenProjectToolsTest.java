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
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

@CacioTest
public class PayaraMicroMavenProjectToolsTest extends TestBase {

    @Test
    public void forProject_returns_PayaraMicroMavenProjectTools_for_payara_micro_project()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-micro").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(PayaraMicroMavenProjectTools.class);
    }

    @Test
    public void payaraMicroProjectTools_is_instance_of_MavenProjectTools()
    throws Exception {
        final Path homePath = Paths.get(".").toAbsolutePath().normalize();
        final String dir = homePath.resolve("src/test/projects/payara-micro").toString();
        final ProjectTools tool = ProjectTools.forProject(project(dir));
        then(tool).isInstanceOf(MavenProjectTools.class);
    }

    // Payara Micro-specific method names

    @Test
    public void bundleMicro_method_exists() throws Exception {
        final Method m = PayaraMicroMavenProjectTools.class.getMethod("bundleMicro");
        then(m).isNotNull();
    }

    @Test
    public void startMicro_method_exists() throws Exception {
        final Method m = PayaraMicroMavenProjectTools.class.getMethod("startMicro");
        then(m).isNotNull();
    }

    @Test
    public void stopMicro_method_exists() throws Exception {
        final Method m = PayaraMicroMavenProjectTools.class.getMethod("stopMicro");
        then(m).isNotNull();
    }

    @Test
    public void reloadApplication_method_exists() throws Exception {
        final java.lang.reflect.Method m = PayaraMicroMavenProjectTools.class.getMethod("reloadApplication");
        then(m).isNotNull();
    }

    @Test
    public void devMode_method_exists() throws Exception {
        final java.lang.reflect.Method m = PayaraMicroMavenProjectTools.class.getMethod("devMode");
        then(m).isNotNull();
    }

    @Test
    public void stopServer_method_does_not_exist() {
        then(PayaraMicroMavenProjectTools.class.getMethods())
            .extracting(java.lang.reflect.Method::getName)
            .doesNotContain("stopServer");
    }
}

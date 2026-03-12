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
public class GradleProjectToolsTest extends TestBase {

    @Test
    public void resolveRunCommand_uses_gradle_when_no_wrapper_present()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveRunCommand("com.example.Main"))
            .isEqualTo("gradle run --main-class=com.example.Main");
    }

    @Test
    public void resolveRunCommand_uses_gradlew_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("gradlew"));
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveRunCommand("com.example.App"))
            .isEqualTo("./gradlew run --main-class=com.example.App");
    }

}

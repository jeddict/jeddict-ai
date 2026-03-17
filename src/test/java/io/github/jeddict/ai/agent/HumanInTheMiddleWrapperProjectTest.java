/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.github.jeddict.ai.agent.project.MavenProjectTools;
import io.github.jeddict.ai.agent.project.ProjectTools;
import io.github.jeddict.ai.test.TestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Regression tests for {@link HumanInTheMiddleWrapper} wrapping project-aware
 * tools whose constructors take {@code Project} (rather than a plain
 * {@code String} basedir).  These tests require a headless AWT environment
 * (hence {@code @CacioTest}).
 */
@CacioTest
public class HumanInTheMiddleWrapperProjectTest extends TestBase {

    private List<ToolExecutionRequest> interceptionEvents;
    private Function<ToolExecutionRequest, Boolean> interceptor;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        interceptionEvents = new ArrayList<>();
        interceptor = execution -> {
            interceptionEvents.add(execution);
            return true;
        };
    }

    @Test
    void wrapping_ProjectTools_does_not_throw() throws Exception {
        // Regression: wrapping a ProjectTools (whose constructor takes a Project
        // and calls basedirOf(project)) must not throw
        // "IllegalArgumentException: project cannot be null".
        final ProjectTools original = ProjectTools.forProject(project(projectDir));
        final ProjectTools wrapped = new HumanInTheMiddleWrapper(interceptor).wrap(original);
        then(wrapped).isNotNull();
    }

    @Test
    void wrapping_MavenProjectTools_does_not_throw() throws Exception {
        // Same regression check for the MavenProjectTools subclass.
        final MavenProjectTools original = new MavenProjectTools(project(projectDir));
        final MavenProjectTools wrapped = new HumanInTheMiddleWrapper(interceptor).wrap(original);
        then(wrapped).isNotNull();
    }
}

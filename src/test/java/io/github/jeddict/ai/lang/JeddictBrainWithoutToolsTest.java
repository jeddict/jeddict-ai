/*
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
package io.github.jeddict.ai.lang;

import io.github.jeddict.ai.agent.pair.HackerWithoutTools;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.HACKER_WITHOUT_TOOLS;
import io.github.jeddict.ai.test.TestBase;
import org.junit.jupiter.api.Test;


/**
 * The JeddictBrainTest class is a test class that extends TestBase.
 * It contains unit tests for the JeddictBrain class, verifying its constructors,
 * listener management, and functionality such as code analysis.
 */
//
// TODO. argument sanity check in constructors
//
public class JeddictBrainWithoutToolsTest extends TestBase {

    @Test
    public void get_hacker_without_tool_if_model_does_not_support_tools() {
        final JeddictBrain brain = new JeddictBrain("dummy", false);  // no tools
        brain.probedModels.put("dummy", Boolean.FALSE);

        final HackerWithoutTools h = brain.pairProgrammer(HACKER_WITHOUT_TOOLS);

    }

}

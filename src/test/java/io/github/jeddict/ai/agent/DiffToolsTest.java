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

import io.github.jeddict.ai.test.TestBase;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;

import static org.assertj.core.api.BDDAssertions.then;

public class DiffToolsTest extends TestBase {

    @Test
    void diff_is_intercepted_before_execution() throws Exception {
        // 1. Setup
        List<String> interceptionEvents = new ArrayList<>();
        // This is the logic we want to execute before the tool method
        java.util.function.Function<String, Boolean> interceptorLogic = (s) -> {
            interceptionEvents.add("intercepted");
            return true;
        };

        // The tool and wrapper factory (which don't exist yet)
        DiffTools originalTool = new DiffTools(projectDir);

        // 2. Action
        // We wrap the tool and cast it to its original type to call the method
        DiffTools wrappedTool = new HumanInTheMiddleWrapper(interceptorLogic).wrap(originalTool);
        wrappedTool.diff("test.txt", "new content");

        // 3. Assertion
        // We assert that our "before" logic was triggered
        then(interceptionEvents)
                .withFailMessage("The interceptor logic was not executed before the tool method.")
                .containsExactly("intercepted");
    }
}

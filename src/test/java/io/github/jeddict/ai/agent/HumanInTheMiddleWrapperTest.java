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

import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.lang.reflect.Method;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HumanInTheMiddleWrapperTest extends TestBase {

    private List<ToolExecutionRequest> interceptionEvents;
    private Function<ToolExecutionRequest, Boolean> interceptor;
    private DummyTool wrappedTool;
    private DummyTool originalTool;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        interceptionEvents = new ArrayList<>();
        // Default interceptor allows execution
        interceptor = execution -> {
            interceptionEvents.add(execution);
            return true;
        };
        originalTool = new DummyTool(projectDir);
        wrappedTool = new HumanInTheMiddleWrapper(interceptor).wrap(originalTool);
    }

    @Test
    void dummyToolRead_is_not_intercepted() {
        wrappedTool.dummyToolRead();

        then(originalTool.executed()).isTrue();
        then(interceptionEvents).isEmpty();
    }

    @Test
    void dummyToolInteractive_is_not_intercepted() {
        wrappedTool.dummyToolInteractive();

        then(originalTool.executed()).isTrue();
        then(interceptionEvents).isEmpty();
    }

    @Test
    void dummyToolWrite_is_intercepted_with_tool_execution() {
        wrappedTool.dummyToolWrite();

        then(originalTool.executed()).isTrue();
        then(interceptionEvents).hasSize(1);
        final ToolExecutionRequest execution = interceptionEvents.get(0);
        then(execution.name()).isEqualTo("dummyToolWrite");
        then(execution.arguments()).isEqualTo("{}");
    }

    @Test
    void dummyTool_with_arguments_is_intercepted_with_formatted_message() {
        wrappedTool.dummyToolWithArgs("val1", List.of("a", "b"));

        then(originalTool.executed()).isTrue();
        then(interceptionEvents).hasSize(1);
        
        final ToolExecutionRequest execution = interceptionEvents.get(0);
        
        then(execution.name()).isEqualTo("dummyToolWithArgs");
        then(execution.arguments()).isEqualTo("{\"arg2\":\"[a, b]\",\"arg1\":\"val1\"}");
    }

    @Test
    void dummyTool_without_policy_is_intercepted() {
        wrappedTool.dummyTool();

        then(originalTool.executed()).isTrue();
        then(interceptionEvents).hasSize(1);
        final ToolExecutionRequest execution = interceptionEvents.get(0);
        then(execution.name()).isEqualTo("dummyTool");
    }

    @Test
    void non_tool_methods_are_delegated_correctly_without_interception() {
        originalTool.dummyToolWrite();
        then(wrappedTool.executed()).isTrue();
        // The executed() call itself is a non-tool method, should not trigger interceptionEvents
        then(interceptionEvents).isEmpty(); 
    }

    @Test
    void tool_annotation_is_preserved_on_wrapped_methods() throws Exception {
        // 1. Setup
        Class<?> wrappedClass = wrappedTool.getClass();
        
        // 2. Action
        Method wrappedMethod = wrappedClass.getMethod("dummyToolWrite");
        
        // 3. Assertion
        then(wrappedMethod.isAnnotationPresent(Tool.class))
            .withFailMessage("The @Tool annotation was not preserved on the wrapped method.")
            .isTrue();
    }

    @Test
    void execution_is_blocked_with_ToolExecutionException_if_hitm_returns_false() {
        final Function<ToolExecutionRequest, Boolean> blockingInterceptor = s -> false;
        DummyTool blockingWrappedTool = new HumanInTheMiddleWrapper(blockingInterceptor).wrap(originalTool);

        assertThatThrownBy(() -> blockingWrappedTool.dummyToolWrite())
                .isInstanceOf(ToolExecutionRejected.class)
                .hasMessageContaining("REJECTED:user cancelled action: dummyToolWrite");

        then(originalTool.executed()).isFalse();
    }
}
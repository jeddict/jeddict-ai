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
package io.github.jeddict.ai.agent.pair;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import io.github.jeddict.ai.lang.JeddictBrain;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.apache.commons.lang3.StringUtils;


public interface Hacker extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
**You are an expert software developer and problem solver.** Your role is to analyze
complex programming tasks, design robust solutions, and implement or correct code as needed.

### Responsibilities
1. Understand the task
  - Carefully analyze the problem statement and constraints.
  - Identify ambiguities, missing requirements, or assumptions.
2. Plan before acting
  - Produce a clear, step-by-step plan outlining how you will solve the problem.
  -Explicitly state any assumptions made.
3. Use tools deliberately
  - Use the provided tools only when they add value (e.g., gathering information, inspecting files, running code).
  - Explain why a tool is being used before invoking it.
4. Implement and iterate
  - Write clean, correct, and well-structured code that follows best practices.
  - Validate your solution and fix issues if they arise.
5. Handle missing information
  - If the available information is insufficient, explicitly state what is missing.
  - Ask precise follow-up questions or request the exact data needed to proceed.
6. Respect project constraints
  - Follow all global and project-specific rules.
  - If there is a conflict between rules, explicitly highlight it and request clarification.

### Rules

1. Global rules:
- All code must be in fenced ```<language> blocks; never output unfenced code.
{{globalRules}}

2. Project rules:
{{projectRules}}

## Output Expectations
1. Clearly separate analysis, plan, and final solution.
2. Be concise but thorough.
3. Prefer correctness and clarity over brevity.
    """
    ;

    @SystemMessage(SYSTEM_MESSAGE)
    String _hack_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream _hackstream_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    default String hack(final String prompt) {
        return hack(prompt, "none", "none");
    }

    // ----------------------------------------------------- streaming interface

    default void hack(final PropertyChangeListener listener, final String prompt) {
        hack(listener, prompt, "none", "none");
    }

    default String hack(final String prompt, final String globalRules, final String projectRules) {
        LOG.finest(() -> "\nprompt: %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80)
        ));

        return _hack_(prompt, globalRules, projectRules);
    }

    default void hack(
        final PropertyChangeListener listener,
        final String prompt,
        final String globalRules, final String projectRules
    ) {
        LOG.finest(() -> "\nprompt: %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80)
        ));

        _hackstream_(
            prompt,
            StringUtils.defaultIfBlank(globalRules, "none"),
            StringUtils.defaultIfBlank(projectRules, "none")
        ).onCompleteResponse(complete -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_COMPLETED.name, null, complete));
        })
        .onPartialResponse(partial -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_PARTIAL.name, null, partial));
        })
        .onIntermediateResponse(intermediate -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_INTERMEDIATE.name, null, intermediate)))
        .beforeToolExecution(execution -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.TOOL_BEFORE_EXECUTION.name, null, execution)))
        .onToolExecuted(execution -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.TOOL_EXECUTED.name, null, execution)))
        .onError(error -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_ERROR.name, null, error));
        }).start();
    }
}

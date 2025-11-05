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

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestSpecialistTest extends PairProgrammerTestBase {

    @Test
    public void pair_is_a_PairProgrammer() {
        final TestSpecialist pair = AgenticServices.agentBuilder(TestSpecialist.class)
            .chatModel(model)
            .build();
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void generateTestCase_returns_AI_provided_response_with_and_without_rules() {
        final String QUERY = "use mock 'hello world.txt' and create JUnit5 tests using AssertJ";
        final String ALL_CLASSES = "here we have all project classes";
        final String CLASS = "the class";
        final String METHOD = "the method";
        final String PROMPT = "the test prompt";
        final String SESSION_RULES = "these are the session rules";
        final List<String> FRAMEWORKS = List.of("JUnit5", "AssertJ");

        //
        // With all parameters
        //
        String expectedSystem = TestSpecialist.SYSTEM_MESSAGE
            .replace("{{rules}}", SESSION_RULES)
            .replace("{{frameworks}}", FRAMEWORKS.toString());
        String expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", QUERY)
            .replace("{{class}}", CLASS)
            .replace("{{method}}", METHOD)
            .replace("{{project}}", ALL_CLASSES)
            .replace("{{prompt}}", PROMPT);

        final TestSpecialist pair = AgenticServices.agentBuilder(TestSpecialist.class)
            .chatModel(model)
            .build();

        String answer = pair.generateTestCase(QUERY, ALL_CLASSES, CLASS, METHOD, PROMPT, SESSION_RULES, List.of());

        ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer.trim()).isEqualTo("hello world");

        //
        // Without project classes
        //
        expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", QUERY)
            .replace("{{class}}", CLASS)
            .replace("{{method}}", METHOD)
            .replace("{{project}}", "")
            .replace("{{prompt}}", PROMPT);

        pair.generateTestCase(QUERY, null, CLASS, METHOD, PROMPT, SESSION_RULES, List.of());

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        //
        // Without class
        //
        expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", QUERY)
            .replace("{{class}}", "")
            .replace("{{method}}", METHOD)
            .replace("{{project}}", "")
            .replace("{{prompt}}", PROMPT);

        pair.generateTestCase(QUERY, null, null, METHOD, PROMPT, SESSION_RULES, List.of());

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        //
        // Without method
        //
        expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", QUERY)
            .replace("{{class}}", CLASS)
            .replace("{{method}}", "")
            .replace("{{project}}", ALL_CLASSES)
            .replace("{{prompt}}", PROMPT);

        pair.generateTestCase(QUERY, ALL_CLASSES, CLASS, null, PROMPT, SESSION_RULES, List.of());

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        //
        // Without history
        //
        expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", QUERY)
            .replace("{{class}}", CLASS)
            .replace("{{method}}", METHOD)
            .replace("{{project}}", ALL_CLASSES)
            .replace("{{prompt}}", PROMPT);

        pair.generateTestCase(QUERY, ALL_CLASSES, CLASS, METHOD, PROMPT, SESSION_RULES, null);

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        //
        // Without query
        //
        expectedSystem = TestSpecialist.SYSTEM_MESSAGE
            .replace("{{rules}}", SESSION_RULES)
            .replace("{{frameworks}}", "[]");
        expectedUser = TestSpecialist.USER_MESSAGE
            .replace("{{query}}", "")
            .replace("{{class}}", CLASS)
            .replace("{{method}}", METHOD)
            .replace("{{project}}", ALL_CLASSES)
            .replace("{{prompt}}", PROMPT);

        pair.generateTestCase(null, ALL_CLASSES, CLASS, METHOD, PROMPT, SESSION_RULES, List.of());

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );
    }

}

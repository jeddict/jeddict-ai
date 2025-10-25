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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class CodeAdvisorTest extends PairProgrammerTestBase {

    private static final String LINE = "String name=\"this is the line of code\";";
    private static final String CLASS = "use mock 'suggest names.txt'";
    private static final String CLASSES = "classes data";
    private static final String PROJECT_INFO = "JDK 17";

    private CodeAdvisor pair;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(CodeAdvisor.class)
            .chatModel(model)
            .build();

    }

    @Test
    public void suggestVariableNames_returns_AI_provided_response() {
        final String expectedSystem = CodeAdvisor.SYSTEM_MESSAGE;
        final String expectedUser =
            CodeAdvisor.USER_MESSAGE.replace("{{element}}", "variable")
                .replace("{{line}}", LINE).replace("{{class}}", CLASS)
                .replace("{{classes}}", CLASSES).replace("{{project}}", "")
            +
            "\nYou must put every item on a separate line.";

        final List<String> names = pair.suggestVariableNames(CLASSES, CLASS, LINE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(names).containsExactly("one", "two", "three");
    }

    @Test
    public void suggestMethodNames_returns_AI_provided_response() {
        final String expectedSystem = CodeAdvisor.SYSTEM_MESSAGE;
        final String expectedUser =
            CodeAdvisor.USER_MESSAGE.replace("{{element}}", "method")
                .replace("{{line}}", LINE).replace("{{class}}", CLASS)
                .replace("{{classes}}", CLASSES).replace("{{project}}", "")
            +
            "\nYou must put every item on a separate line.";

        final List<String> names = pair.suggestMethodNames(CLASSES, CLASS, LINE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(names).containsExactly("one", "two", "three");
    }

    @Test
    public void suggestStringLiterals_returns_AI_provided_response() {
        final String expectedSystem = CodeAdvisor.SYSTEM_MESSAGE;
        final String expectedUser =
            CodeAdvisor.USER_MESSAGE.replace("{{element}}", "string literals")
                .replace("{{line}}", LINE).replace("{{class}}", CLASS)
                .replace("{{classes}}", CLASSES).replace("{{project}}", "")
            +
            "\nYou must put every item on a separate line.";

        final List<String> names = pair.suggestStringLiterals(CLASSES, CLASS, LINE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(names).containsExactly(
            "one",
            "one and an half (not a valid identifier)",
            "two",
            "three",
            "-four"
        );
    }

    @Test
    public void suggestMethodInvocations_returns_AI_provided_response() {
        final String expectedSystem = CodeAdvisor.SYSTEM_MESSAGE;
        final String expectedUser =
            CodeAdvisor.USER_MESSAGE.replace("{{element}}", "method invocation")
                .replace("{{line}}", LINE).replace("{{class}}", CLASS)
                .replace("{{classes}}", CLASSES).replace("{{project}}", PROJECT_INFO)
            +
            "\nYou must put every item on a separate line.";

        final List<String> names = pair.suggestMethodInvocations(PROJECT_INFO, CLASSES, CLASS, LINE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(names).containsExactly(
            "one",
            "one and an half (not a valid identifier)",
            "two",
            "three",
            "-four"
        );
    }
}
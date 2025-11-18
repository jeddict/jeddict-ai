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
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ShakespeareTest extends PairProgrammerTestBase {

    final String TEXT = "this is some text";
    final String CODE = "use mock 'hello world.txt'";

    private Shakespeare pair;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(Shakespeare.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void fixGrammar_returns_AI_provided_response() {
        final String expectedSystem = Shakespeare.SYSTEM_MESSAGE;
        final String expectedUser = Shakespeare.USER_MESSAGE
            .replace("{{message}}", "")
            .replace("{{text}}", TEXT)
            .replace("{{code}}", CODE);

        final String fixedText = pair.fixGrammar(TEXT, CODE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(fixedText.trim()).isEqualTo("hello world");
    }

    @Test
    public void enhanceText_returns_AI_provided_response() {
        final String expectedSystem = Shakespeare.SYSTEM_MESSAGE;
        final String expectedUser = Shakespeare.USER_MESSAGE
            .replace("{{message}}", Shakespeare.USER_MESSAGE_ENHANCE_TEXT)
            .replace("{{text}}", TEXT)
            .replace("{{code}}", CODE);

        final String fixedText = pair.enhanceText(TEXT, CODE);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(fixedText.trim()).isEqualTo("hello world");
    }
}

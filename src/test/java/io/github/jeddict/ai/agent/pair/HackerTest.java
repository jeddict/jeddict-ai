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

import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.lang.JeddictBrain;
import static io.github.jeddict.ai.lang.JeddictBrain.EventProperty.CHAT_COMPLETED;
import io.github.jeddict.ai.test.DummyPropertyChangeListener;
import io.github.jeddict.ai.test.DummyTool;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class HackerTest extends PairProgrammerTestBase {

    private static final String GLOBAL_RULES = "globale rules";
    private static final String PROJECT_RULES = "project rules";

    private Hacker pair;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AiServices.builder(Hacker.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void hack_returns_AI_provided_response() {
        final String expectedSystem = Hacker.SYSTEM_MESSAGE
            .replace("{{globalRules}}", "none")
            .replace("{{projectRules}}", "none");
        final String expectedUser = "use mock 'hello world.txt'";

        final String answer = pair.hack(expectedUser);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void hack_with_rules_returns_AI_provided_response() {
        final String expectedSystem = Hacker.SYSTEM_MESSAGE
            .replace("{{globalRules}}", GLOBAL_RULES)
            .replace("{{projectRules}}", PROJECT_RULES);
        final String expectedUser = "use mock 'hello world.txt'";

        final String answer = pair.hack(expectedUser, GLOBAL_RULES, PROJECT_RULES);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void hack_with_streaming() throws IOException {
        final DummyPropertyChangeListener streamListener = new DummyPropertyChangeListener();
        final DummyTool tool = new DummyTool();
        final String[] msg = new String[2];

        pair = AiServices.builder(Hacker.class)
            .streamingChatModel(model)
            .tools(List.of(tool))
            .build();

        pair.hack(streamListener, "execute mock dummyTool", "", "");
        then(tool.executed()).isTrue();

        int i = 0;
        then(streamListener.events).isNotEmpty();
        PropertyChangeEvent e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.CHAT_INTERMEDIATE.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.TOOL_BEFORE_EXECUTION.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.TOOL_EXECUTED.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.CHAT_PARTIAL.name);
        then(e.getNewValue()).isEqualTo("true");
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(CHAT_COMPLETED.name);
        then(((ChatResponse)e.getNewValue()).aiMessage().text()).isEqualTo("true");
    }

}

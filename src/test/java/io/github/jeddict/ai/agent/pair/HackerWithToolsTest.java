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
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.test.DummyTool;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class HackerWithToolsTest extends PairProgrammerTestBase {

    private static final String GLOBAL_RULES = "globale rules";
    private static final String PROJECT_RULES = "project rules";
    private static final String PROJECT_INFO = "some project info";

    private HackerWithTools pair;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AiServices.builder(HackerWithTools.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void hack_returns_AI_provided_response() {
        final String expectedSystem = HackerWithTools.SYSTEM_MESSAGE
            .replace("{{globalRules}}", "")
            .replace("{{projectRules}}", "")
            .replace("{{projectInfo}}", "");
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
        final String expectedSystem = HackerWithTools.SYSTEM_MESSAGE
            .replace("{{globalRules}}", GLOBAL_RULES)
            .replace("{{projectRules}}", PROJECT_RULES)
            .replace("{{projectInfo}}", PROJECT_INFO);
        final String expectedUser = "use mock 'hello world.txt'";

        final String answer = pair.hack(expectedUser, PROJECT_INFO, GLOBAL_RULES, PROJECT_RULES);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void hack_with_streaming() {
        final DummyTool tool = new DummyTool();
        final String[] msg = new String[2];

        final StringBuilder result = new StringBuilder();
        pair = AiServices.builder(HackerWithTools.class)
            .streamingChatModel(model)
            .tools(List.of(tool))
            .registerListeners(new AiServiceCompletedListener() {
                @Override
                public void onEvent(final AiServiceCompletedEvent e) {
                    final ChatResponse res = (ChatResponse)e.result().get();
                    result.append('=').append(res.aiMessage().text());
                }
            }, new AiServiceResponseReceivedListener() {
                @Override
                public void onEvent(final AiServiceResponseReceivedEvent e) {
                    final ChatResponse response = e.response();
                    if (response.aiMessage().hasToolExecutionRequests()) {
                        result.append(response.aiMessage().toolExecutionRequests().get(0).name());
                    }
                }
            })
            .build();

        pair.hack(null, "execute tool dummyTool", "", "", "");

        then(tool.executed()).isTrue();
        then(result.toString()).isEqualTo("dummyTool=true");
    }
}

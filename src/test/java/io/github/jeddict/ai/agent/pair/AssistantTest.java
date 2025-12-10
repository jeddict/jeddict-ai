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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import static io.github.jeddict.ai.lang.JeddictBrain.EventProperty.CHAT_COMPLETED;
import static io.github.jeddict.ai.lang.JeddictBrain.EventProperty.CHAT_PARTIAL;
import io.github.jeddict.ai.test.DummyPropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class AssistantTest extends PairProgrammerTestBase {

    private static final String PROMPT = "use mock 'hello world.txt'";
    private static final String PROJECT = "JDK 17";
    private static final String GLOBAL_RULES = "global rules";
    private static final String PROJECT_RULES = "project rules";


    private Assistant pair;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AiServices.builder(Assistant.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void chat_returns_AI_provided_response() throws Exception {
        //
        // method declaration
        //
        final TreePath tree = codeFromSayHello();

        final String expectedSystem = Assistant.SYSTEM_MESSAGE
                .replace("{{globalRules}}", GLOBAL_RULES)
                .replace("{{projectRules}}", PROJECT_RULES);
        final String expectedUser = Assistant.USER_MESSAGE
                .replace("{{prompt}}", PROMPT)
                .replace("{{code}}", tree.getCompilationUnit().toString())
                .replace("{{project}}", PROJECT);

        final String answer =
                pair.chat(PROMPT, tree, PROJECT, GLOBAL_RULES, PROJECT_RULES);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer).isEqualTo("hello world\n");
    }

    @Test
    public void chat_with_prompt_only() {
        then(pair.chat("use mock 'hello world.txt'")).isEqualTo("hello world\n");
    }

    @Test
    public void chat_with_streaming() throws Exception {
        final DummyPropertyChangeListener streamListener = new DummyPropertyChangeListener();
        final TreePath tree = codeFromSayHello(); // method declaration

        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .build();

        final String expectedSystem = Assistant.SYSTEM_MESSAGE
                .replace("{{globalRules}}", GLOBAL_RULES)
                .replace("{{projectRules}}", PROJECT_RULES);
        final String expectedUser = Assistant.USER_MESSAGE
                .replace("{{prompt}}", PROMPT)
                .replace("{{code}}", tree.getCompilationUnit().toString())
                .replace("{{project}}", PROJECT);

        pair.chat(streamListener, PROMPT, tree, PROJECT, GLOBAL_RULES, PROJECT_RULES);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(streamListener.events).isNotEmpty();
        PropertyChangeEvent e = streamListener.events.get(0);
        then(e.getPropertyName()).isEqualTo(CHAT_PARTIAL.name);
        then(e.getNewValue()).isEqualTo("hello world");
        e = streamListener.events.get(1);
        then(e.getPropertyName()).isEqualTo(CHAT_COMPLETED.name);
        then(((ChatResponse)e.getNewValue()).aiMessage().text()).isEqualTo("hello world\n");
    }

    @Test
    public void chat_with_streaming_prompt_only() {
        final DummyPropertyChangeListener streamListener = new DummyPropertyChangeListener();

        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .build();

        pair.chat(streamListener, PROMPT);

        then(streamListener.events).isNotEmpty();
        PropertyChangeEvent e = streamListener.events.get(0);
        then(e.getPropertyName()).isEqualTo(CHAT_PARTIAL.name);
        then(e.getNewValue()).isEqualTo("hello world");
        e = streamListener.events.get(1);
        then(e.getPropertyName()).isEqualTo(CHAT_COMPLETED.name);
        then(((ChatResponse)e.getNewValue()).aiMessage().text()).isEqualTo("hello world\n");
    }

    // --------------------------------------------------------- private methods

    private TreePath codeFromSayHello() throws IOException {
        final JavacTask task = parseSayHello();
        final Iterable<? extends CompilationUnitTree> ast = task.parse();
        task.analyze();
        final CompilationUnitTree unit = ast.iterator().next();

        //
        // method declaration
        //
        return findTreePathAtCaret(unit, task, 302);
    }

}

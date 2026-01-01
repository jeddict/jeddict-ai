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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.lang.DummyJeddictBrainListener;
import io.github.jeddict.ai.util.ContextHelper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

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

        then(answer).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void chat_with_prompt_only() {
        then(pair.chat("use mock 'hello world.txt'")).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void chat_with_streaming() throws Exception {
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        final TreePath tree = codeFromSayHello(); // method declaration

        final StringBuffer ret = new StringBuffer();
        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .registerListeners(new AiServiceCompletedListener() {
                @Override
                public void onEvent(final AiServiceCompletedEvent e) {
                    final ChatResponse res = (ChatResponse)e.result().get();
                    ret.append(res.aiMessage().text());
                }
            })
            .build();

        pair.chat(listener, PROMPT, tree, PROJECT, GLOBAL_RULES, PROJECT_RULES);

        then(ret).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void chat_with_images_returns_AI_provided_response() {
        final String expectedSystem = Assistant.SYSTEM_MESSAGE
            .replace("{{globalRules}}", GLOBAL_RULES)
            .replace("{{projectRules}}", PROJECT_RULES);
        final String expectedUser = Assistant.USER_MESSAGE
                .replace("{{prompt}}", PROMPT)
                .replace("{{code}}", "")
                .replace("{{project}}", PROJECT);


        final FileObject imgFO = FileUtil.toFileObject(
            FileUtil.normalizeFile(new File(".", "src/test/resources/images/4x4.png"))
        );

        final List<String> images = ContextHelper.getImageFilesContext(Set.of(imgFO), List.of("png"));

        final String answer = pair.chat(
            PROMPT, images, PROJECT, GLOBAL_RULES, PROJECT_RULES
        );

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        final List<ChatMessage> messages = request.chatRequest().messages();

        //
        // System message
        //
        then(((SystemMessage)messages.get(0)).text())
            .isEqualTo(expectedSystem);

        final UserMessage userMessage = (UserMessage)messages.get(1);
        then(userMessage.contents()).containsExactly(
            TextContent.from(expectedUser),
            ImageContent.from(images.get(0))
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void chat_with_streaming_prompt_only() {

        final StringBuilder result = new StringBuilder();
        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .registerListeners(new AiServiceCompletedListener() {
                @Override
                public void onEvent(final AiServiceCompletedEvent e) {
                    final ChatResponse res = (ChatResponse)e.result().get();
                    result.append(res.aiMessage().text());
                }
            })
            .build();

        pair.chat(null, PROMPT);

        then(result).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void chat_with_streaming_and_code() throws Exception {
        final TreePath tree = codeFromSayHello();

        final StringBuilder result = new StringBuilder();
        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .registerListeners(new AiServiceCompletedListener() {
                @Override
                public void onEvent(final AiServiceCompletedEvent e) {
                    final ChatResponse res = (ChatResponse)e.result().get();
                    result.append(res.aiMessage().text());
                }
            })
            .build();

        pair.chat(null, PROMPT, tree, PROJECT, GLOBAL_RULES, PROJECT_RULES);

        then(result).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void chat_with_streaming_and_images() {

        final StringBuilder result = new StringBuilder();
        pair = AiServices.builder(Assistant.class)
            .streamingChatModel(model)
            .registerListeners(new AiServiceCompletedListener() {
                @Override
                public void onEvent(final AiServiceCompletedEvent e) {
                    final ChatResponse res = (ChatResponse)e.result().get();
                    result.append(res.aiMessage().text());
                }
            })
            .build();

        final FileObject imgFO = FileUtil.toFileObject(
            FileUtil.normalizeFile(new File(".", "src/test/resources/images/4x4.png"))
        );

        final List<String> images = ContextHelper.getImageFilesContext(Set.of(imgFO), List.of("png"));

        pair.chat(null, PROMPT, images, PROJECT, GLOBAL_RULES, PROJECT_RULES);

        then(result).isEqualToIgnoringNewLines("hello world");
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

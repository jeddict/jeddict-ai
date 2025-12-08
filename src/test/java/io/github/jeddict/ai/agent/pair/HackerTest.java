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

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.util.ContextHelper;
import java.io.File;
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
public class HackerTest extends PairProgrammerTestBase {

    private static final String PROJECT_INFO = "JDK 17";


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
            .replace("{{project}}", PROJECT_INFO)
            .replace("{{rules}}", "none");
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
            .replace("{{project}}", PROJECT_INFO)
            .replace("{{rules}}", "some rules");
        final String expectedUser = "use mock 'hello world.txt'";

        final String answer = pair.hack(expectedUser, "some rules");

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void hack_with_images_returns_AI_provided_response() {
        final SystemMessage expectedSystem = new SystemMessage(
            Hacker.SYSTEM_MESSAGE.replace("{{rules}}", "some rules")
        );
        final String expectedUser = "use mock 'hello world.txt'";

        final FileObject imgFO = FileUtil.toFileObject(
            FileUtil.normalizeFile(new File(".", "src/test/resources/images/4x4.png"))
        );

        final List<String> images = ContextHelper.getImageFilesContext(Set.of(imgFO), List.of("png"));

        final String answer = pair.hack(
            expectedUser, images, "some rules"
        );

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        final List<ChatMessage> messages = request.chatRequest().messages();

        //
        // System message
        //
        then(String.valueOf((SystemMessage)messages.get(0)))
            .isEqualTo(String.valueOf(expectedSystem));

        final UserMessage userMessage = (UserMessage)messages.get(1);
        then(userMessage.contents()).containsExactly(
            TextContent.from(expectedUser),
            ImageContent.from(images.get(0))
        );

        then(answer.trim()).isEqualTo("hello world");
    }

    @Test
    public void hack_with_streaming() {
        final DummyTool tool = new DummyTool();
        final String[] msg = new String[2];

        pair = AiServices.builder(Hacker.class)
            .streamingChatModel(model)
            .tools(List.of(tool))
            .build();

        pair.hackstream("execute mock dummyTool").onToolExecuted(
            (execution) -> { msg[1] = execution.result(); }
        ).onCompleteResponse(
            (response) -> {
                msg[0] = response.aiMessage().text();
            }
        ).onError((t) -> t.printStackTrace())
        .start();

        then(msg).containsExactly("true", "true");
        then(tool.executed()).isTrue();
    }

}

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
package io.github.jeddict.ai.models;

import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.jeddict.ai.test.DummyChatModelListener;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.test.TestBase;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DummyChatModelTest extends TestBase {

    @Test
    public void doChat_returns_provided_message() {
        final DummyChatModel chat = new DummyChatModel();

        ChatRequest chatRequest = ChatRequest.builder().messages(
            UserMessage.from("use mock 'hello world.txt'")
        ).build();

        then(chat.doChat(chatRequest).aiMessage().text().trim()).isEqualTo("hello world");
    }

    @Test
    public void doChat_return_the_error_page_if_the_mock_does_not_exist() {
        final DummyChatModel chat = new DummyChatModel();

        ChatRequest chatRequest = ChatRequest.builder().messages(
            UserMessage.from("use mock none.txt")
        ).build();

        then(chat.doChat(chatRequest).aiMessage().text().trim())
            .startsWith("Oops! Mock file '" + Paths.get("src/test/resources/mocks/none.txt").toUri().getPath() + "' not found.");
    }

    @Test
    public void doChat_returns_the_default_page_if_no_mock_is_given() {
        final DummyChatModel chat = new DummyChatModel();

        ChatRequest chatRequest = ChatRequest.builder().messages(
            UserMessage.from("Hello!")
        ).build();

        then(chat.doChat(chatRequest).aiMessage().text().trim())
            .startsWith("To use a mock, send a prompt containing the following instruction:");
    }

    @Test
    public void listeners_registers_the_provided_listeners() {
        // Given
        final DummyChatModel chat = new DummyChatModel();
        final ChatModelListener listener1 = new ChatModelListener() {}; // Concrete listener 1
        final ChatModelListener listener2 = new ChatModelListener() {}; // Concrete listener 2

        // When 1: Add the first listener
        chat.addListener(listener1);

        // Then 1: Check if the first listener is registered
        then(chat.listeners()).containsExactly(listener1);

        // When 2: Add the second listener
        chat.addListener(listener2);

        // Then 2: Check if both listeners are registered
        then(chat.listeners()).containsExactly(listener1, listener2);
    }

    @Test
    public void chat_string_invokes_listeners() {
        // Given
        final DummyChatModel chat = new DummyChatModel();

        DummyChatModelListener testListener = new DummyChatModelListener();
        chat.addListener(testListener);

        String userMessageString = "use mock 'hello world.txt'";

        // When
        chat.chat(userMessageString);

        // Then
        thenReceivedMessageIs(testListener, new UserMessage(userMessageString));
    }

    @Test
    public void chat_array_invokes_listeners() {
        // Given
        final DummyChatModel chat = new DummyChatModel();

        DummyChatModelListener testListener = new DummyChatModelListener();
        chat.addListener(testListener);

        ChatMessage[] messagesArray = new ChatMessage[]{new UserMessage("use mock 'hello world.txt'")};

        // When
        chat.chat(messagesArray);

        // Then
        thenReceivedMessageIs(testListener, (UserMessage) messagesArray[0]);
    }

    @Test
    public void tools_support() throws IOException {
        DummyChatModel chat = new DummyChatModel();

        //
        // With known tool and ToolChoice.REQUIRED
        //
        // Given
        chat.toolChoice = ToolChoice.REQUIRED;

        ChatRequest request = ChatRequest.builder()
            .messages(List.of(
                UserMessage.from("execute tool dummyTool")
            ))
            .toolSpecifications(
                ToolSpecifications.toolSpecificationsFrom(new DummyTool())
            )
            .build();

        // When
        ChatResponse response = chat.chat(request);

        // Then
        then(response.aiMessage().hasToolExecutionRequests()).isTrue();

        //
        // With known tool and ToolChoice.AUTO
        //
        chat = new DummyChatModel();

        // Given
        chat.toolChoice = ToolChoice.AUTO;

        request = ChatRequest.builder()
            .messages(List.of(
                UserMessage.from("execute tool dummyTool")
            ))
            .toolSpecifications(
                ToolSpecifications.toolSpecificationsFrom(new DummyTool())
            )
            .build();

        // When
        response = chat.chat(request);

        // Then
        then(response.aiMessage().hasToolExecutionRequests()).isTrue();

        //
        // With unknown tool
        //
        chat = new DummyChatModel();
        // Given
        request = ChatRequest.builder()
            .messages(List.of(
                UserMessage.from("execute tool fileRead")
            ))
            .toolSpecifications(
                ToolSpecifications.toolSpecificationsFrom(new DummyTool())
            )
            .build();

        // When
        response = chat.chat(request);

        // Then
        then(response.aiMessage().hasToolExecutionRequests()).isFalse();
    }

    @Test
    public void no_tools_support() throws IOException {
        final DummyChatModel chat = new DummyChatModel();

        // Given
        chat.toolChoice = ToolChoice.NONE;

        ChatRequest request = ChatRequest.builder()
            .messages(List.of(
                UserMessage.from("execute tool dummyTool")
            ))
            .toolSpecifications(
                ToolSpecifications.toolSpecificationsFrom(new DummyTool())
            )
            .build();

        // When
        ChatResponse response = chat.chat(request);

        // Then
        then(response.aiMessage().hasToolExecutionRequests()).isFalse();
    }

    @Test
    public void simulate_streaming() throws IOException {
        final DummyChatModel chat = new DummyChatModel();

        final List<String> messages = new ArrayList();
        chat.chat("use mock 'hello world.txt'", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(final String partialResponse) {
                messages.add(partialResponse);
            }

            @Override
            public void onCompleteResponse(final ChatResponse res) {
                // .trim() to make it platform independent (i.e. \n vs \r\n)
                messages.add(res.aiMessage().text().trim());
            }

            @Override
            public void onError(Throwable thrwbl) {
            }
        });

        then(messages).containsExactly("hello world", "hello world");
    }

    @Test
    public void simulate_model_error() {
        final DummyChatModel chat = new DummyChatModel();

        chat.error = new RuntimeException("this is an error");

        thenThrownBy(() -> chat.chat("any prompt"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("this is an error");

        //
        // TODO: add error handling when streaming
        //
    }

    // --------------------------------------------------------- private methods

    private void thenReceivedMessageIs(DummyChatModelListener testListener, UserMessage expectedUserMessage) {
        then(testListener.lastRequestContext).isPresent();
        then(testListener.lastRequestContext.get().chatRequest().messages()).containsExactly(expectedUserMessage);

        then(testListener.lastResponseContext).isPresent();
        then(testListener.lastResponseContext.get().chatResponse().aiMessage()).isInstanceOf(AiMessage.class);
        then(testListener.lastResponseContext.get().chatResponse().aiMessage().text().trim()).isEqualTo("hello world"); // Default mock response
    }

}

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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.test.DummyChatModelListener;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

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
            .startsWith("Oops! Mock file 'src/test/resources/mocks/none.txt' not found.");
    }

    @Test
    public void doChat_returns_the_default_page_if_no_mock_is_given() {
        final DummyChatModel chat = new DummyChatModel();

        ChatRequest chatRequest = ChatRequest.builder().messages(
            UserMessage.from("Hello!")
        ).build();

        then(chat.doChat(chatRequest).aiMessage().text().trim())
            .startsWith("To use the mock server, send a prompt containing the following instruction:");
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
    public void listeners_are_invoked_during_chat_operation() {
        // Given
        final DummyChatModel chat = new DummyChatModel();

        DummyChatModelListener testListener1 = new DummyChatModelListener();
        DummyChatModelListener testListener2 = new DummyChatModelListener();
        chat.addListener(testListener1);
        chat.addListener(testListener2);

        UserMessage userMessage = new UserMessage("use mock 'hello world.txt'");
        ChatRequest chatRequest = ChatRequest.builder().messages(userMessage).build();

        // When
        chat.doChat(chatRequest);

        // Then for listener 1
        thenReceivedMessageIs(testListener1, userMessage);

        // Then for listener 2
        thenReceivedMessageIs(testListener2, userMessage);
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

    // --------------------------------------------------------- private methods

    private void thenReceivedMessageIs(DummyChatModelListener testListener, UserMessage expectedUserMessage) {
        then(testListener.lastRequestContext).isPresent();
        then(testListener.lastRequestContext.get().chatRequest().messages()).containsExactly(expectedUserMessage);

        then(testListener.lastResponseContext).isPresent();
        then(testListener.lastResponseContext.get().chatResponse().aiMessage()).isInstanceOf(AiMessage.class);
        then(testListener.lastResponseContext.get().chatResponse().aiMessage().text().trim()).isEqualTo("hello world"); // Default mock response
    }

}

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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ToolChoice;
import java.util.ArrayList;
import java.util.Collections;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class DummyChatModel implements ChatModel, StreamingChatModel {

    private final Logger LOG = Logger.getLogger(DummyChatModel.class.getCanonicalName());

    private static final String DEFAULT_MOCK_FILE = "src/test/resources/mocks/default.txt";
    private static final String ERROR_MOCK_FILE = "src/test/resources/mocks/error.txt";
    private static final Pattern MOCK_INSTRUCTION_PATTERN =
        Pattern.compile("use mock\\s+(?:'([^']+)'|(\\S+))", Pattern.CASE_INSENSITIVE);

    private final List<ChatModelListener> listeners;

    public ToolChoice toolChoice = ToolChoice.AUTO;

    public RuntimeException error = null;

    public String lastToolExecutionResult = null;

    public boolean toolExecuted = false;

    public DummyChatModel() {
        this.listeners = new ArrayList<>();
    }

    public void addListener(ChatModelListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public List<ChatModelListener> listeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public ChatResponse chat(final ChatRequest chatRequest) {
        LOG.info(() -> "> " + String.valueOf(chatRequest));
        final ChatResponse chatResponse = ChatModel.super.chat(chatRequest);
        LOG.info(() -> "< " + String.valueOf(chatResponse));

        return chatResponse;
    }

    @Override
    public ChatResponse doChat(final ChatRequest chatRequest) {
        LOG.info(() -> "> " + String.valueOf(chatRequest));

        if (error != null) {
            LOG.info(() -> "> DummyChatModel instructed to raise " + error);

            throw error;
        }

        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, provider(), Collections.emptyMap());
        for (ChatModelListener listener : listeners) {
            listener.onRequest(requestContext);
        }

        final StringBuilder bodyBuilder = new StringBuilder();

        //
        // build a string with all system and user messages
        //
        chatRequest.messages().forEach((msg) -> {
            bodyBuilder.append("\n");
            if (msg instanceof UserMessage usrMsg) {
                for(Content c: usrMsg.contents()) {
                    if (c instanceof TextContent txt) {
                        bodyBuilder.append(txt.text()).append("\n");
                    } else if (c instanceof ImageContent img) {
                        bodyBuilder.append(img.image().url());
                    } else {
                        bodyBuilder.append(String.valueOf(usrMsg));
                    }
                }
            } else if (msg instanceof SystemMessage sysMsg) {
                bodyBuilder.append(sysMsg.text());
            } else {
                bodyBuilder.append(String.valueOf(msg));
            }
        });

        final String body = bodyBuilder.toString();

        AiMessage responseMessage = null;

        //
        // Simulate the request to execute a tool if toolChoice is REQUIRED.
        // This is not meant to be generic for now, it supports now a simple use
        // case where the execution of one tool only is requested, plus with no
        // arguments. It is primarily tailored to support
        // <code>ToolsProbingTool.probeToolsSupport()</code>
        //
        // When tools are ivolved, langchain4j triggers the execution of the tools:
        //
        // client       langchain4j         Model       Tool
        //   |               |                |           |
        //   |----prompt---->|                |           |
        //   |               |<--exec tool X--|           |
        //   |               |<--exec and get result----> |
        //   |               |-----result---->|           |
        //   |               |<---response----|           |
        //   |<--answer------|                |           |
        //   |               |                |           |
        //
        // Note that the model needs to provide an answer with ... to tell
        // langchain4j the execution of the task is complete.
        //

        if ((toolChoice == ToolChoice.REQUIRED) || (toolChoice == ToolChoice.AUTO)) {
            if (!toolExecuted) {
                final String tool = on(chatRequest.toolSpecifications()).loop((specification) -> {
                    final String name = specification.name();
                    if (body.contains("execute mock " + name)) {
                        _break_(name);
                    }
                });

                if (tool != null) {
                    toolExecuted = true; // set before execution to make sure
                                         // execution is done even in case of
                                         // exceptions
                    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                        .id("XKSdkL2PU")
                        .name(tool)
                        .arguments("{}")
                        .build();

                    responseMessage = AiMessage.from(toolRequest);
                }
            } else {
                toolExecuted = false;  // geeting ready for the next prompt
                responseMessage = on(chatRequest.messages()).loop((msg) -> {
                    if (msg instanceof ToolExecutionResultMessage toolResult) {
                        _break_(AiMessage.from(toolResult.text()));
                    }
                });
            }
        }

        //
        // If responseMessage is still null, either tool execution was not
        // required or no tool has been found in the prompt
        //
        if (responseMessage == null) {
            //
            // If any message contained the mock instruction pattern, extract the
            // mock, read its content.
            // In case of errors replace the placeholder {{error}} with the error
            // descritpion/message
            //
            Matcher matcher = MOCK_INSTRUCTION_PATTERN.matcher(bodyBuilder.toString());

            Path mockPath = Path.of(DEFAULT_MOCK_FILE);
            if (matcher.find()) {
                String mockFile = matcher.group(1); // Quoted file name
                if (mockFile == null) {
                    mockFile = matcher.group(2); // Unquoted file name
                }
                mockPath = Path.of("src/test/resources/mocks").resolve(mockFile).normalize();
            }

            String errorMessage = null;
            if (!Files.exists(mockPath)) {
                errorMessage = "Mock file '" + mockPath.toUri().getPath() + "' not found.";
                mockPath = Path.of(ERROR_MOCK_FILE);
            }

            String mockContent;
            try {
                mockContent = Files.readString(mockPath, StandardCharsets.UTF_8);
                mockContent = mockContent.replaceAll("\\{error}", errorMessage);
            } catch (IOException x) {
                mockContent = "Error reading mock file: " + x.getMessage();
            }

            responseMessage = AiMessage.from(mockContent);
        }

        ChatResponse chatResponse =
            ChatResponse.builder().aiMessage(responseMessage).build();

        ChatModelResponseContext responseContext = new ChatModelResponseContext(chatResponse, chatRequest, provider(), Collections.emptyMap());
        for (ChatModelListener listener : listeners) {
            listener.onResponse(responseContext);
        }

        LOG.info(() -> "< " + String.valueOf(chatResponse));

        return chatResponse;
    }

    @Override
    public void doChat(final ChatRequest chatRequest, final StreamingChatResponseHandler handler) {
        LOG.info(() -> "> " + chatRequest + ", " + handler);

        final ChatResponse response = doChat(chatRequest);
        final String answer = response.aiMessage().text();

        if (answer != null) {
            for(String m: answer.trim().split("\n")) {
                handler.onPartialResponse(m.trim());
            }
        }

        handler.onCompleteResponse(response);
    }

    @Override
    public void chat(final ChatRequest chatRequest, final StreamingChatResponseHandler handler) {
        LOG.info(() -> "> " + chatRequest + ", " + handler);
        StreamingChatModel.super.chat(chatRequest, handler);
    }

    @Override
    public String chat(final String userMessage) {
        LOG.info(() -> "> " + String.valueOf(userMessage));
        final String chatResponse = ChatModel.super.chat(userMessage);
        LOG.info(() -> "< " + String.valueOf(chatResponse));

        return chatResponse;
    }

    @Override
    public void chat(final String userMessage, final StreamingChatResponseHandler handler) {
        LOG.info(() -> "> " + String.valueOf(userMessage) + ", " + handler);
        StreamingChatModel.super.chat(userMessage, handler);
    }

    @Override
    public ChatResponse chat(final ChatMessage[] messages) {
        LOG.info(() -> "> " + String.valueOf(List.of(messages)));
        final ChatResponse chatResponse = ChatModel.super.chat(messages);
        LOG.info(() -> "< " + String.valueOf(chatResponse));

        return chatResponse;
    }

    @Override
    public void chat(final List<ChatMessage> messages, final StreamingChatResponseHandler handler) {
        LOG.info(() -> "> " + String.valueOf(messages) + ", " + handler);
        StreamingChatModel.super.chat(messages, handler);
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        final Set capabilities = ChatModel.super.supportedCapabilities();
        LOG.info(() -> "< " + String.valueOf(capabilities));

        return capabilities;
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        final ChatRequestParameters params = ChatModel.super.defaultRequestParameters();
        LOG.info(() -> "< " + String.valueOf(params));

        return params;
    }

    @Override
    public ModelProvider provider() {
        final ModelProvider provider = ChatModel.super.provider();
        LOG.info(() -> "provider: " + String.valueOf(provider));

        return provider;
    }
}

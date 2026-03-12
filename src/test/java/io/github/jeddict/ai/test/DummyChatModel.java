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

package io.github.jeddict.ai.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.SystemMessage;
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
import dev.langchain4j.model.chat.request.ToolChoice;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;


public class DummyChatModel implements ChatModel, StreamingChatModel {

    private final Logger LOG = Logger.getLogger(DummyChatModel.class.getCanonicalName());

    private static final String DEFAULT_MOCK_FILE = "src/test/resources/mocks/default.txt";
    private static final String ERROR_MOCK_FILE = "src/test/resources/mocks/error.txt";
    private static final Pattern MOCK_INSTRUCTION_PATTERN =
        Pattern.compile("use mock\\s+(?:'([^']+)'|(\\S+))", Pattern.CASE_INSENSITIVE);

    /**
     * Not that these ChatModelListener are not meant to be called by the model
     * code itself. Langchain4j will use them as needed to trigger request/response
     * related events.
     */
    private final List<ChatModelListener> listeners;

    public ToolChoice toolChoice = ToolChoice.AUTO;

    public RuntimeException error = null;

    public String lastToolExecutionResult = null;

    public boolean toolExecuted = false;


    public DummyChatModel() {
        this.listeners = new ArrayList<>();
    }

    public void addListener(final ChatModelListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
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
            LOG.info(() -> "DummyChatModel instructed to raise " + error);

            throw error;
        }

        final String mockInstruction = messageWithInstruction(chatRequest.messages());

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
                    // (?i) makes "execute tool " case-insensitive
                    // \Q and \E escape the 'name' to ensure special characters don't break the regex
                    // "(?![a-zA-Z])" lookahead expression asserting that the next character is not a-z or A-Z.
                    // the lookahead is needed to make sure an exatct match (e.g.
                    // is name is dummyTool, execute tool dummyToolSomething
                    // does not match)
                    final String regex = "(?i)execute tool " + Pattern.quote(name) + "(?![a-zA-Z])";

                    if (Pattern.compile(regex).matcher(mockInstruction).find()) {
                        _break_(name);
                    }
                });

                if (tool != null) {
                    LOG.info(() -> "Requesting to execute tool %s".formatted(tool));

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
                toolExecuted = false;  // getting ready for the next prompt
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
            // mock, read its content. I first check the mock file itself, and
            // if not found, the same file prepended by "n. " where n is the value
            // of {@code count} starting from 1.
            //
            // In case of errors replace the placeholder {{error}} with the error
            // descritpion/message
            //
            final Matcher matcher = MOCK_INSTRUCTION_PATTERN.matcher(mockInstruction);

            Path mockPath = Path.of(DEFAULT_MOCK_FILE);
            String mockFile = null;

            while(matcher.find()) {
                mockFile = matcher.group(1); // Quoted file name
                if (mockFile == null) {
                    mockFile = matcher.group(2); // Unquoted file name
                }
            }

            if (mockFile != null) {
                mockPath = Path.of("src/test/resources/mocks").resolve(mockFile).normalize();
            }

            String errorMessage = "";
            if (!Files.exists(mockPath)) {
                mockPath = Path.of("src/test/resources/mocks").resolve(mockFile).normalize();
                if (!Files.exists(mockPath)) {
                    errorMessage = "Mock file '%s' not found.".formatted(
                        mockPath.toUri().getPath()  // not elegant but cross platform...
                    );
                    mockPath = Path.of(ERROR_MOCK_FILE);
                }
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

    // --------------------------------------------------------- private methods

    /**
     *
     * If the last message is a user message and ontains "use mock" we pick
     * that message. If not in the user message, let's check the previous
     * response: if it contains "use mock", we pick the response.
     * If not in the response let's check the system message if provided
     * (assuming it is in the first message): if it contains "use mock" we
     * pick the system message.
     * If no matches are found, an empty string is returned
     */
    private String messageWithInstruction(final List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        final int size = messages.size();
        if (messages.get(size-1) instanceof UserMessage msg) {
            if (msg.hasSingleText() && doesContainIstruction(msg.singleText())) {
                return msg.singleText();
            } else {
                final String contentText = on(msg.contents()).loop((content) -> {
                   if (content.type() == ContentType.TEXT) {
                       final String text = content.toString();
                       if (doesContainIstruction(text)) {
                            _break_(text);
                        }
                   }
                });
                if (contentText != null) {
                    return contentText;
                }
            }
        }

        if ((size > 2) && (messages.get(size-2) instanceof AiMessage msg)) {
            final String text = msg.text();
            if (doesContainIstruction(text)) {
                return text;
            }
        }

        if (messages.get(0) instanceof SystemMessage msg) {
            final String text = msg.text();
            if (doesContainIstruction(text)) {
                return text;
            }
        }

        return "";
    }

    private boolean doesContainIstruction(final String text) {
        if (text == null) {
            return false;
        }
        final String lowerText = text.toLowerCase();
        return lowerText.toLowerCase().contains("use mock")
            || lowerText.toLowerCase().contains("execute tool");
    }
}

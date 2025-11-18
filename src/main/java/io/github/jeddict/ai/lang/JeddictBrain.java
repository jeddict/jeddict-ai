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
package io.github.jeddict.ai.lang;

/**
 *
 * @author Shiwani Gupta
 */
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.Assistant;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.response.TokenHandler;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.PropertyChangeEmitter;
import io.github.jeddict.ai.util.Utilities;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;

public class JeddictBrain implements PropertyChangeEmitter {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    private int memorySize = 0;

    public enum EventProperty {
        CHAT_TOKENS("chatTokens"),
        CHAT_ERROR("chatError"),
        CHAT_COMPLETED("chatComplete"),
        CHAT_PARTIAL("chatPartial"),
        CHAT_INTERMEDIATE("chatIntermediate"),
        TOOL_BEFORE_EXECUTION("toolBeforeExecution"),
        TOOL_EXECUTED("toolExecuted")
        ;

        public final String name;

        EventProperty(final String name) {
            this.name = JeddictBrain.class.getCanonicalName() + '.' + name;
        }
    }

    public static String UNSAVED_PROMPT = "Unsaved user message";

    public final Optional<ChatModel> chatModel;
    public final Optional<StreamingChatModel> streamingChatModel;
    protected final List<AbstractTool> tools;

    public final String modelName;

    public JeddictBrain(
        final boolean streaming
    ) {
        this("", streaming, List.of());
    }

    public JeddictBrain(
        final String modelName,
        final boolean streaming,
        final List<AbstractTool> tools
    ) {
        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }
        this.modelName = modelName;

        final JeddictChatModelBuilder builder =
            new JeddictChatModelBuilder(this.modelName);

        if (streaming) {
            this.streamingChatModel = Optional.of(builder.buildStreaming());
            this.chatModel = Optional.empty();
        } else {
            this.chatModel = Optional.of(builder.build());
            this.streamingChatModel = Optional.empty();
        }
        this.tools = (tools != null)
                   ? List.of(tools.toArray(new AbstractTool[0])) // immutable
                   : List.of();
    }

    /**
     *
     * @return the agent memory size in messages (including the system prompt)
     */
    public int memorySize() {
        return memorySize;
    }

    /**
     * Instructs JeddictBrain to use a message memory of the provided size when
     * creating the agents.
     *
     * @param size the size of the memory (0 = no memory) - must be positive
     *
     * @return self
     */
    public JeddictBrain withMemory(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be greather than 0 (where 0 means no memory)");
        }
        this.memorySize = size; return this;
    }

    public String generate(final Project project, final String prompt) {
        return generateInternal(project, false, prompt, null, null);
    }

    public String generate(final Project project, final String prompt, List<String> images, List<Response> responseHistory) {
        return generateInternal(project, false, prompt, images, responseHistory);
    }

    public String generate(final Project project, boolean agentEnabled, final String prompt) {
        return generateInternal(project, agentEnabled, prompt, null, null);
    }

    public String generate(final Project project, boolean agentEnabled, final String prompt, List<String> images, List<Response> responseHistory) {
        return generateInternal(project, agentEnabled, prompt, images, responseHistory);
    }

    public UserMessage buildUserMessage(String prompt, List<String> imageBase64Urls) {
        List<Content> parts = new ArrayList<>();

        // Add the prompt text
        parts.add(new TextContent(prompt));

        // Add each image as ImageContent
        for (String imageUrl : imageBase64Urls) {
            parts.add(new ImageContent(imageUrl));
        }

        // Convert list to varargs
        return UserMessage.from(parts.toArray(new Content[0]));
    }

    //
    // TODO: P3 - better use of langchain4j functionalities (see https://docs.langchain4j.dev/tutorials/agents)
    // TODO: P3 - after refactory project should not be needed any more
    //
    private String generateInternal(Project project, boolean agentEnabled, String prompt, List<String> images, List<Response> responseHistory) {
        if (chatModel.isEmpty() && streamingChatModel.isEmpty()) {
            throw new IllegalStateException("AI assistant model not intitalized, this looks like a bug!");
        }

        if (project != null) {
            prompt = prompt + "\n" + ProjectMetadataInfo.get(project);
        }
        String systemMessage = null;
        String globalRules = PreferencesManager.getInstance().getGlobalRules();
        if (globalRules != null) {
            systemMessage = globalRules;
        }
        if (project != null) {
            String projectRules = PreferencesManager.getInstance().getProjectRules(project);
            if (projectRules != null) {
                systemMessage = systemMessage + '\n' + projectRules;
            }
        }
        List<ChatMessage> messages = new ArrayList<>();
        if (systemMessage != null && !systemMessage.trim().isEmpty()) {
            messages.add(SystemMessage.from(systemMessage));
        }

        //
        // add conversation history (multiple responses)
        //
        // Note that the query can be null when the conversation started from
        // AssistantChatManager.performRewrite() (i.e. from an AI hint)
        //
        if (responseHistory != null && !responseHistory.isEmpty()) {
            for (Response res : responseHistory) {
                final String q = (res.getQuery() != null)
                               ? res.getQuery() : UNSAVED_PROMPT;
                messages.add(UserMessage.from(q));
                messages.add(AiMessage.from(res.toString()));
            }
        }

        if (images != null && !images.isEmpty()) {
            messages.add(buildUserMessage(prompt, images));
        } else {
            messages.add(UserMessage.from(prompt));
        }
        //
        // TODO: P3 - decouple token counting from saving stats; saving stats should listen to this event
        //
        fireEvent(EventProperty.CHAT_TOKENS, TokenHandler.saveInputToken(messages));

        final StringBuilder response = new StringBuilder();
        try {

            if (streamingChatModel.isPresent()) {
                if(agentEnabled) {
                    final Assistant assistant = AiServices.builder(Assistant.class)
                        .streamingChatModel(streamingChatModel.get())
                        .tools(tools.toArray())
                        .build();

                    assistant.stream(messages)
                        .onCompleteResponse(complete -> {
                            fireEvent(EventProperty.CHAT_COMPLETED, complete);
                            //handler.onCompleteResponse(partial);
                        })
                        .onPartialResponse(partial -> {
                            fireEvent(EventProperty.CHAT_PARTIAL, partial);
                            //handler.onPartialResponse(partial);
                        })
                        .onIntermediateResponse(intermediate -> fireEvent(EventProperty.CHAT_INTERMEDIATE, intermediate))
                        .beforeToolExecution(execution -> fireEvent(EventProperty.TOOL_BEFORE_EXECUTION, execution))
                        .onToolExecuted(execution -> fireEvent(EventProperty.TOOL_EXECUTED, execution))
                        .onError(error -> {
                            fireEvent(EventProperty.CHAT_ERROR, error);
                            //handler.onError(error);
                        })
                        .start();
                } else {
                    streamingChatModel.get().chat(messages, new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(final String partial) {
                            fireEvent(EventProperty.CHAT_PARTIAL, partial);
                        }

                        @Override
                        public void onCompleteResponse(final ChatResponse completed) {
                            fireEvent(EventProperty.CHAT_COMPLETED, completed);
                        }

                        @Override
                        public void onError(final Throwable error) {
                            fireEvent(EventProperty.CHAT_ERROR, error);
                        }
                    });
                }
            } else {
                final ChatModel model = chatModel.get();

                ChatResponse chatResponse = null;
                if (agentEnabled) {
                    Assistant assistant = AiServices.builder(Assistant.class)
                            .chatModel(model)
                            .tools(tools.toArray())
                            .build();
                    chatResponse = assistant.chat(messages);

                } else {
                    chatResponse = model.chat(messages);
                }
                fireEvent(EventProperty.CHAT_COMPLETED, chatResponse);
                response.append(chatResponse.aiMessage().text());

                CompletableFuture.runAsync(() -> TokenHandler.saveOutputToken(response.toString()));
            }
        } catch (Exception x) {
            LOG.finest(() -> "Communication error: " + x.getMessage());
            response.append(Utilities.errorHTMLBlock(x));
            fireEvent(EventProperty.CHAT_ERROR, x);
        }

        LOG.finest(() -> "Returning " + response);

        return response.toString();
    }

    /**
     * Creates and configures a pair programmer agent based on the specified specialist.
     *
     * @param <T> the type of the agent to be created
     * @param specialist the specialist that defines the type of the agent and its behavior
     *
     * @return an instance of the configured agent
     */
    public <T> T pairProgrammer(final PairProgrammer.Specialist specialist) {
        AgentBuilder<T> builder =
            AgenticServices.agentBuilder(specialist.specialistClass)
            .chatModel(chatModel.get());

        if (memorySize > 0) {
            builder.chatMemory(MessageWindowChatMemory.withMaxMessages(memorySize));
        }

        return (T)builder.build();
    }

    public String generateDescription(
        final Project project,
        final String source, final String methodContent, final List<String> images,
        final List<Response> previousChatResponse, final String userQuery,
        final String sessionRules
    ) {
        return generateDescription(project, false, source, methodContent, images, previousChatResponse, userQuery, sessionRules);
    }

    public String generateDescription(
        final Project project, final boolean agentEnabled,
        final String source, final String methodContent, final List<String> images,
        final List<Response> previousChatResponse, final String userQuery,
        final String sessionRules
    ) {
        StringBuilder prompt = new StringBuilder();
        if (sessionRules != null && !sessionRules.isEmpty()) {
            prompt.append(sessionRules).append("\n\n");
        }

        if (methodContent != null) {
            prompt.append("Method Content:\n")
                    .append(methodContent)
                    .append("\n\nDo not return complete Java Class, return only Method");
        } else if (source != null) {
            prompt.append("Source:\n")
                    .append(source)
                    .append("\n\n");
        }
        prompt.append("User Query:\n")
                .append(userQuery);

        String response = generate(project, agentEnabled, prompt.toString(), images, previousChatResponse);

        LOG.finest(response);

        return response;
    }

    public void addProgressListener(final PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
    }

    public void removeProgressListener(final PropertyChangeListener listener) {
        removePropertyChangeListener(listener);
    }

    private void fireEvent(EventProperty property, Object value) {
        LOG.finest(() -> "Firing event " + property + " with value " + value);
        firePropertyChange(property.name, null, value);
    }

}

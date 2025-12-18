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
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.util.PropertyChangeEmitter;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class JeddictBrain implements PropertyChangeEmitter {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    private int memorySize = 0;

    public enum InteractionMode {
        QUERY,       // no tools, mainly queries
        AGENT,       // tools execution no human interaction
        INTERACTIVE  // tools execution previa human confirmation
    }

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
    public final InteractionMode mode;

    protected final List<AbstractTool> tools;

    public final String modelName;

    public JeddictBrain(
        final boolean streaming
    ) {
        this("", streaming, InteractionMode.QUERY, List.of());
    }

    public JeddictBrain(
        final String modelName, final boolean streaming
    ) {
        this(modelName, streaming, InteractionMode.QUERY, List.of());
    }

    public JeddictBrain(
        final String modelName,
        final boolean streaming,
        final InteractionMode mode,
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
        this.mode = mode;
        this.tools = (tools != null)
                   ? List.copyOf(tools) // immutable
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

    /**
     * Creates and configures a pair programmer agent based on the specified specialist.
     *
     * @param <T> the type of the agent to be created
     * @param specialist the specialist that defines the type of the agent and its behavior
     *
     * @return an instance of the configured agent
     */
    public <T> T pairProgrammer(final PairProgrammer.Specialist specialist) {
        //
        // The HACKER is a top level AI agent that interacts with the user. As
        // such it must support many more functionalities then other Agents and
        // overcome some design limitations currently in langchain4j agents (see
        // https://github.com/langchain4j/langchain4j/issues/4098,
        // https://github.com/langchain4j/langchain4j/issues/4177,
        // https://github.com/langchain4j/langchain4j/issues/3519 )
        //
        // Assistant is a top level tool, but for generale enquiries and
        // interactions with the AI. It is mainly for use in QUERY interaction
        // mode. Since it supports streaming, we need to use AiServices.
        //
        if ((specialist == PairProgrammer.Specialist.HACKER) ||
            (specialist == PairProgrammer.Specialist.ASSISTANT)) {
            final AiServices builder = AiServices.builder(specialist.specialistClass);

            if (specialist == PairProgrammer.Specialist.HACKER) {
                builder.tools(tools.toArray());
            }

            chatModel.ifPresentOrElse(
                (model) -> builder.chatModel(model),
                () -> builder.streamingChatModel(streamingChatModel.get())
            );

            if (memorySize > 0) {
                builder.chatMemory(MessageWindowChatMemory.withMaxMessages(memorySize));
            }

            return (T)builder.build();
        }

        //
        // Build normal utility agents
        //
        AgentBuilder<T> builder =
            AgenticServices.agentBuilder(specialist.specialistClass)
                .chatModel(chatModel.get());

        if (memorySize > 0) {
            builder.chatMemory(MessageWindowChatMemory.withMaxMessages(memorySize));
        }

        return (T)builder.build();
    }

    public void addProgressListener(final PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
    }

    public void removeProgressListener(final PropertyChangeListener listener) {
        removePropertyChangeListener(listener);
    }

}

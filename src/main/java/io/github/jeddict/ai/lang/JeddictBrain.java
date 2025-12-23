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
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.ToolsProber;
import io.github.jeddict.ai.agent.ToolsProbingTool;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.util.PropertyChangeEmitter;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JeddictBrain implements PropertyChangeEmitter {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    private int memorySize = 0;

    public enum InteractionMode {
        QUERY, // no tools, mainly queries
        AGENT, // tools execution no human interaction
        INTERACTIVE  // tools execution previa human confirmation
    }

    private static final String EVENT_NAME_PREFIX = EventProperty.class.getPackageName() + ".event.";
    public enum EventProperty {
        CHAT_ERROR(EVENT_NAME_PREFIX + "chat.error"),
        CHAT_COMPLETED(EVENT_NAME_PREFIX + "chat.completed"),
        CHAT_PARTIAL(EVENT_NAME_PREFIX + "chat.partial"),
        CHAT_INTERMEDIATE(EVENT_NAME_PREFIX + "chat.intermediate"),
        REQUEST_START(EVENT_NAME_PREFIX + "request.start"),
        REQUEST_END(EVENT_NAME_PREFIX + "request.end"),
        REQUEST_ERROR(EVENT_NAME_PREFIX + "request.error"),
        TOOL_EXECUTING(EVENT_NAME_PREFIX + "tool.executing"),
        TOOL_EXECUTED(EVENT_NAME_PREFIX + "tool.executed");

        public final String name;

        EventProperty(final String name) {
            this.name = JeddictBrain.class.getCanonicalName() + '.' + name;
        }
    }

    public final InteractionMode mode;
    public final boolean streaming;

    protected final List<AbstractTool> tools;

    public final String modelName;

    protected Map<String, Boolean> probedModels = new HashMap(); // per instance on purpose to avoid race conditions

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
        this.streaming = streaming;
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
        this.memorySize = size;
        return this;
    }

    /**
     * Creates and configures a pair programmer agent based on the specified
     * specialist.
     *
     * @param <T> the type of the agent to be created
     * @param specialist the specialist that defines the type of the agent and
     * its behavior
     *
     * @return an instance of the configured agent
     */
    public <T> T pairProgrammer(PairProgrammer.Specialist specialist) {
        //
        // HACKER is a top level AI agent that interacts with the user. As such,
        // it must support many more functionalities then other agents and
        // overcome some design limitations currently in langchain4j agents (see
        // https://github.com/langchain4j/langchain4j/issues/4098,
        // https://github.com/langchain4j/langchain4j/issues/4177,
        // https://github.com/langchain4j/langchain4j/issues/3519 )
        //
        // Assistant is a top level tool, but for generale enquiries and
        // interactions with the AI. It is mainly for use in QUERY interaction
        // mode. Since it supports streaming, we need to use AiServices.
        //
        if (
            (specialist == PairProgrammer.Specialist.HACKER) ||
            (specialist == PairProgrammer.Specialist.ASSISTANT) ||
            (specialist == PairProgrammer.Specialist.HACKER_WITHOUT_TOOLS)) {
            if (specialist == PairProgrammer.Specialist.HACKER) {
                if (!probeToolSupport()) {
                    specialist = PairProgrammer.Specialist.HACKER_WITHOUT_TOOLS;
                }
            }

            final AiServices builder = AiServices.builder(specialist.specialistClass);
            if (streaming) {
                builder.streamingChatModel(model());
            } else {
                builder.chatModel(model());
            }
            if (memorySize > 0) {
                builder.chatMemory(MessageWindowChatMemory.withMaxMessages(memorySize));
            }
            if (specialist == PairProgrammer.Specialist.HACKER) {
                builder.tools(tools.toArray());
            }


            return (T) builder.build();
        }

        //
        // Build normal utility agents
        //
        final AgentBuilder<T> builder
                = AgenticServices.agentBuilder(specialist.specialistClass)
                        .chatModel(model());

        if (memorySize > 0) {
            builder.chatMemory(MessageWindowChatMemory.withMaxMessages(memorySize));
        }

        return (T) builder.build();
    }

    public void addProgressListener(final PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
    }

    public void removeProgressListener(final PropertyChangeListener listener) {
        removePropertyChangeListener(listener);
    }

    // --------------------------------------------------------- private methods

    protected boolean probeToolSupport() {
        final String LOG_MSG = "model %s %s tools execution";

        //
        // If the model was already probed, return immediately the value
        //
        if (probedModels.containsKey(modelName)) {
            final boolean toolsSupport = probedModels.get(modelName);
            LOG.info(() ->
                (LOG_MSG + " (cached)").formatted(modelName, (toolsSupport) ? "supports" : "does not support")
            );
            return toolsSupport;
        }

        LOG.finest(() -> "probing that %s supports tools".formatted(modelName));

        //
        // Otherwise probe the model by trying to trigger the execution of the
        // ToolsProbingTool tool
        //
        final ToolsProbingTool probeTool = new ToolsProbingTool();
        final ToolsProber prober = AgenticServices.agentBuilder(ToolsProber.class)
            .chatModel(model(false))
            .tools(probeTool)
            .build();
        final boolean toolsSupport = prober.probe(probeTool.probeText);

        probedModels.put(modelName, toolsSupport);

        LOG.info(
            LOG_MSG.formatted(modelName, (toolsSupport) ? "supports" : "does not support")
        );

        return toolsSupport;
    }

    // --------------------------------------------------------- private methods

    /**
     * Returns a streaming or not streaming model based on preferred type provided
     * in the constructor (and saved in {@code streaming}
     *
     * @param <T>
     *
     * @return the model
     */
    private <T> T model() {
        return model(streaming);
    }

    /**
     * Returns a streaming or not streaming model based on {@code usStreaming}
     *
     * @param <T>
     * @param useStreaming
     *
     * @return the model
     */
    private <T> T model(final boolean useStreaming) {

        final JeddictChatModelBuilder builder
                = new JeddictChatModelBuilder(this.modelName, new TokenTrackingListener());

        return (useStreaming) ? (T)builder.buildStreaming() : (T)builder.build();
    }


    private void fireEvent(EventProperty property, Object value) {
        fireEvent(property, null, value);
    }

    private void fireEvent(EventProperty property, Object oldValue, Object newValue) {
        LOG.finest(() ->
            "Firing event %s with values (%s,%s)"
            .formatted(property, String.valueOf(oldValue), String.valueOf(newValue))
        );
        firePropertyChange(property.name, oldValue, newValue);
    }

    // --------------------------------------------------- TokenTrackingListener

    private class TokenTrackingListener implements ChatModelListener {
        /**
         * The main purpose of this listener is to estimate the amount of tokens
         * a request generates. However, there is not a standard definition of
         * what a token is and what is considered a token derived from the
         * user messages and what is instead generated by the models.
         * Models can turn the provided prompt and tool definition into something
         * more digestible for the model and count the added tokens.
         * Therefore, instead of trying to approximate closely what the AI model
         * may consider a token the estimation extracted here is based on the
         * {@code String.valueOf()} of the context's request.
         *
         * @param context
         */
        @Override
        public void onRequest(final ChatModelRequestContext context) {
            fireEvent(
                EventProperty.REQUEST_START, context.chatRequest()
            );
        }

        @Override
        public void onResponse(final ChatModelResponseContext context) {
            fireEvent(
                EventProperty.REQUEST_END, context.chatRequest(), context.chatResponse()
            );
        }

        @Override
        public void onError(final ChatModelErrorContext context) {
            fireEvent(
                EventProperty.REQUEST_ERROR, context.error()
            );
        }
    }

}

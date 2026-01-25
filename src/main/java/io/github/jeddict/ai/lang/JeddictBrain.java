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
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.HumanInTheMiddleWrapper;
import io.github.jeddict.ai.agent.ToolsProber;
import io.github.jeddict.ai.agent.ToolsProbingTool;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import static io.github.jeddict.ai.lang.InteractionMode.INTERACTIVE;
import io.github.jeddict.ai.util.PropertyChangeEmitter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import static ste.lloop.Loop.on;

public class JeddictBrain implements PropertyChangeEmitter {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    private int memorySize = 0;

    private final List<JeddictBrainListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(JeddictBrainListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        listeners.add(listener);
    }

    public void removeListener(JeddictBrainListener listener) {
        listeners.remove(listener);
    }

    public List<JeddictBrainListener> listeners() {
        return Collections.unmodifiableList(listeners);
    }

    public final InteractionMode mode;
    public final boolean streaming;

    protected final List<AbstractTool> tools;

    public final String modelName;

    protected Map<String, Boolean> probedModels = new HashMap(); // per instance on purpose to avoid race conditions

    private Function<String, Boolean> defaultInteraction = (text) -> {
        throw new ToolExecutionException("Write, unknown and null policy tools can not be executed");
    };

    public JeddictBrain(
        final boolean streaming
    ) {
        this("", streaming, InteractionMode.ASK, null, List.of());
    }

    public JeddictBrain(
        final String modelName, final boolean streaming
    ) {
        this(modelName, streaming, InteractionMode.ASK, null, List.of());
    }

    public JeddictBrain(
        final String modelName,
        final boolean streaming,
        final InteractionMode mode,
        final List<AbstractTool> tools
    ) {
        this(modelName, streaming, mode, null, tools);
    }

    public JeddictBrain(
        final String modelName,
        final boolean streaming,
        final InteractionMode mode,
        final Function<String, Boolean> defaultInteraction,
        final List<AbstractTool> tools
    ) {
        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }
        this.modelName = modelName;
        this.streaming = streaming;
        this.mode = mode;

        //
        // if interaction mode is INTERACTIVE, wrap the tools to make sure
        // human in the middle is applied (See HumanInTheMiddleWrapper)
        //
        if ((tools != null) && !tools.isEmpty()) {
            switch (this.mode) {
                case INTERACTIVE -> {
                    if (defaultInteraction != null) {
                        this.defaultInteraction = defaultInteraction;
                    }
                    final HumanInTheMiddleWrapper wrapper =
                        new HumanInTheMiddleWrapper(this.defaultInteraction);

                    this.tools = new ArrayList();
                    on(tools).loop(
                        (tool) -> this.tools.add(wrapper.wrap(tool))
                    );
                }

                case AGENT -> {
                    this.defaultInteraction = null;
                    this.tools = List.copyOf(tools); // make an immutable copy
                }

                default -> {
                    this.defaultInteraction = null;
                    this.tools = List.of();
                }
            }
        } else {
            this.defaultInteraction = null;
            this.tools = List.of();
        }
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

        builder.registerListeners(allListeners());
        builder.toolExecutionErrorHandler(this::toolExecutionErrorHandler);
        builder.toolArgumentsErrorHandler(this::toolArgumentsErrorHandler);

        builder.hallucinatedToolNameStrategy((exec) -> {
            final ToolExecutionRequest ter = (ToolExecutionRequest)exec;
            
            LOG.finest(() -> "tool hallucination: " + ter.name());
            return ToolExecutionResultMessage.from(
                ter, "Error: there is no tool called " + ter.name() + " try with a different name"
            );
        });

        return (T) builder.build();
    }

    // -------------------------------------------------------- ToolErrorHandler

    public ToolErrorHandlerResult toolExecutionErrorHandler(final Throwable error, final ToolErrorContext context) {
        LOG.finest("tool execution error: %s (%s)".formatted(String.valueOf(error), String.valueOf(context)));

        //
        // Exceptions raised by a tool are usually wrapped into a InvocationTaargetException
        //
        Throwable target = error;
        if (error instanceof InvocationTargetException) {
            target = ((InvocationTargetException)error).getTargetException();
            if (target == null) {
                target = error;
            }
        }

        final StringBuilder message = new StringBuilder(target.getClass().getSimpleName());
        message.append(':').append(StringUtils.defaultString(target.getMessage()));

        return ToolErrorHandlerResult.text(message.toString());
    }

    // -------------------------------------------------------- ToolErrorHandler

    public ToolErrorHandlerResult toolArgumentsErrorHandler(Throwable error,  ToolErrorContext context) {
        LOG.finest("tool arguments error: %s (%s)".formatted(error, String.valueOf(context)));
        return ToolErrorHandlerResult.text(String.valueOf(error));
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
     * Returns a new streaming or not streaming model based on {@code useStreaming}
     *
     * @param <T>
     * @param useStreaming
     *
     * @return the model
     */
    private <T> T model(final boolean useStreaming) {
        //
        // At the moment lanchain4j provides the chat request at an higher level
        // with the event AiServicesResponseEvent. This is fired only once the
        // request has been processed by the model and provides both the request
        // and the response (see https://github.com/langchain4j/langchain4j/issues/4365)
        // Howver, we want to know when a request starts, so have to use a ChatModelListener
        //
        final JeddictChatModelBuilder builder =
            new JeddictChatModelBuilder(
                this.modelName,
                new ChatModelListener() {
                    @Override
                    public void onRequest(ChatModelRequestContext ctx) {
                        on(listeners).loop((l) -> l.onRequest(ctx.chatRequest()));
                    }

                    @Override
                    public void onResponse(ChatModelResponseContext ctx) {
                        on(listeners).loop((l) -> l.onResponse(ctx.chatRequest(), ctx.chatResponse()));
                    }
                }
            );


        return (useStreaming) ? (T)builder.buildStreaming() : (T)builder.build();
    }

    private List<AiServiceListener> allListeners() {
        return List.of(
            new AiServiceCompletedListener() {
                @Override
                public void onEvent(AiServiceCompletedEvent e) {
                    //
                    // e.resul() can be a ChatResponse or other objects (e.g.
                    // a String). If not the former, let's convert it taking
                    // the toString() of the object so that the listener can
                    // rely on receive always the same object
                    //
                    final Object obj = e.result().get();

                    final ChatResponse result = (obj instanceof ChatResponse)
                                              ? (ChatResponse)obj
                                              : ChatResponse.builder().aiMessage(AiMessage.from(String.valueOf(obj))).build();
                    LOG.finest(() ->
                        "%s\n%s".formatted(String.valueOf(e.eventClass()), String.valueOf(result))
                    );
                    on(listeners).loop((l) -> l.onChatCompleted(result));
                }
            },
            new AiServiceErrorListener() {
                @Override
                public void onEvent(final AiServiceErrorEvent e) {
                    final Throwable t = e.error();
                    LOG.finest(() -> e.eventClass() + "\n" + t);
                    on(listeners).loop((l) -> l.onError(t));
                }
            },
            new AiServiceStartedListener() {
                @Override
                public void onEvent(final AiServiceStartedEvent e) {
                    final SystemMessage system = e.systemMessage().get();
                    final UserMessage user = e.userMessage();
                    LOG.finest(() ->
                        "%s\n%s\n%s".formatted(String.valueOf(e.eventClass()), String.valueOf(system), String.valueOf(user))
                    );

                    on(listeners).loop((l) -> l.onChatStarted(system, user));
                }
            },
            new ToolExecutedEventListener() {
                @Override
                public void onEvent(final ToolExecutedEvent e) {
                    final ToolExecutionRequest request = e.request();
                    final String result = e.resultText();
                    LOG.finest(() ->
                        "%s\n%s\n%s".formatted(String.valueOf(e.eventClass()), String.valueOf(request), String.valueOf(result))
                    );

                    on(listeners).loop((l) -> l.onToolExecuted(request, result));
                }
            }
        );
    }

}

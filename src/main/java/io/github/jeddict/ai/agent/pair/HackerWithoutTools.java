/**
 * Copyright 2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.toonformat.jtoon.JToon;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.ExecutionJSONObject;
import io.github.jeddict.ai.agent.ToolNotFoundException;
import io.github.jeddict.ai.agent.UtilTools;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.json.JSONArray;
import org.json.JSONObject;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;


enum SystemPromptInfo {
    TOOLS("tools"),
    GLOBAL_RULES("globalRules"),
    PROJECT_RULES("projectRules"),
    PROJECT_INFO("projectInfo");

    public final String value;

    SystemPromptInfo(final String info) {
        this.value = info;
    }
}

public class HackerWithoutTools implements Hacker {

    private static final String ERROR_NO_STREAMING
        = "HackerWithoutTool does not support streaming; use hack(prompt, globalRules, ProjectRules, projectInfo) instead";

    private final ToolifiedAiService toolify;
    private final Logger LOG = Logger.getLogger(getClass().getName());

    protected final JeddictListenerAdapter listenersAdapter;  // controller for low level events
    protected final List<AbstractTool> tools;
    protected final Parser parser;

    private final Map<SystemPromptInfo, String> systemPromptInfo = new HashMap();

    private int maxIterations = 10;

    public HackerWithoutTools(
        final ChatModel model,
        final AiServices<ToolifiedAiService> builder,
        final List<AbstractTool> tools
    ) {
        this.listenersAdapter = on(model.listeners()).loop((l) -> {
            if (l instanceof JeddictListenerAdapter la) {
                _break_(la);
            }
        });

        if (listenersAdapter == null) {
            throw new IllegalArgumentException("the model must have one listener of type HackerWithoutTool.JeddictListenerAdapter");
        }

        this.tools = new ArrayList();
        this.tools.addAll(tools);
        if (on(tools).loop((tool) -> {
            if (tool instanceof UtilTools) {
                _break_(true);
            }
        }) != Boolean.TRUE) {
            try {
                this.tools.add(new UtilTools());
            } catch (IOException x) {
                //
                // nothing to do
                //
            }
        }
        this.parser = Parser.builder().build();
        builder.systemMessageProvider(this::systemPrompt);

        toolify = builder.build();
    }

    public void maxIterations(final int n) {
        maxIterations = n;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public List<JeddictBrainListener> listeners() {
        return listenersAdapter.listeners;
    }

    @Override
    public String hack(String prompt, String globalRules, String projectRules, String projectInfo) {
        final String[] nextPrompt = new String[] { prompt };

        final List<JeddictBrainListener> listeners = listeners();

        systemPromptInfo.put(SystemPromptInfo.GLOBAL_RULES, globalRules);
        systemPromptInfo.put(SystemPromptInfo.PROJECT_RULES, projectRules);
        systemPromptInfo.put(SystemPromptInfo.PROJECT_INFO, projectInfo);

        String answer;
        int n = 0;

        on(listeners).loop((l) -> {
            l.onChatStarted(
                SystemMessage.from(systemPrompt(null)),
                UserMessage.from(prompt)
            );
        });

        final List<ExecutionJSONObject> executions = new ArrayList();

        do {
            executions.clear();
            answer = toolify._hack_(nextPrompt[0]);

            //
            // Is there any tool execution block? if so, let's execute it.
            // First collect the tools so that we can fire the response event,
            // then we can execute the tools if any.
            //
            final Node document = parser.parse(answer);

            document.accept(new AbstractVisitor() {
                @Override
                public void visit(FencedCodeBlock block) {
                    // Check if the info string (language tag) is "tool"
                    final String type = StringUtils.defaultString(block.getInfo());
                    if (type.startsWith("tool:")) {
                        final String name = type.substring(5);
                        final String content = block.getLiteral();
                        LOG.info(() ->
                            "found tool block for tool %s with content:\n%s".formatted(
                                name,
                                StringUtils.abbreviateMiddle(content, "...", 100)
                            )
                        );
                        executions.add(new ExecutionJSONObject(name, content));
                    }
                }
            });

            on(listeners).loop((l) -> {
                l.onResponse(listenersAdapter.lastRequest, responseWithTools(executions));
            });


            on(executions).loop((execution) -> {
                try {
                    final String result = executeTool(execution);
                    nextPrompt[0] = "%s: OK\n%s".formatted(execution.name, result);
                    on(listeners).loop((l) -> {
                       l.onToolExecuted(toolExecutionRequest(execution), result);
                    });
                } catch (ToolExecutionException x) {
                    //
                    // If the exception does not have any root cause,
                    // the issue is with the process of executing a tool
                    // which we want to report to the listeners.
                    // If instead there is a root cause, the tool itself
                    // got an error, therefore we just need to notify it
                    // to the llm
                    //
                    if (x.getCause() == null) {
                        on(listeners).loop((l) -> l.onError(x));
                    } else {
                        nextPrompt[0] = "%s: ERR %s".formatted(execution.name, String.valueOf(x.getCause()));
                    }
                } catch (Throwable x) {
                    //
                    // for any other issues we can't do much more than
                    // reporting it to the listeners
                    //
                    on(listeners).loop((l) -> l.onError(x));
                }
            });

            ++n;
        } while (!executions.isEmpty() && n < maxIterations);

        on(listeners).loop((l) -> {
            l.onChatCompleted(listenersAdapter.lastResponse);
        });

        return answer;
    }

    public String hack(final String prompt) {
        return hack(prompt, "", "", "");
    }

    /**
     * Note streaming is not supported with HackerWithoutTools for now.
     * @param listener
     * @param prompt
     * @param projectInfo
     * @param globalRules
     * @param projectRules
     */
    @Override
    public void hack(
        final JeddictBrainListener listener,
        final String prompt, final String projectInfo,
        final String globalRules, final String projectRules
    ) {
        throw new IllegalArgumentException(ERROR_NO_STREAMING);
    }

    // --------------------------------------------------------- private methods

    private String executeTool(final ExecutionJSONObject toolExecution) {
        LOG.finest(() -> "executing " + toolExecution.name);

        final String[] result = new String[1];
        final Boolean found = on(tools).loop((tool) -> {
            final Class cls = tool.getClass();
            final Boolean foundInClass = on(cls.getMethods()).loop((method) -> {
                if (method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class) && method.getName().equals(toolExecution.name)) {
                    try {
                        final List<String> parameterNames = new ArrayList<>();
                        on(method.getParameters()).loop(p -> parameterNames.add(p.getName()));

                        final Object[] args = (parameterNames.isEmpty()) ? new Object[0] : toolExecution.arguments(parameterNames.toArray(new String[0]));

                        final Object ret = method.invoke(tool, args);

                        if (ret == null) {
                            result[0] = null;
                        } else {
                            result[0] = (ret instanceof String strret) ? strret : String.valueOf(ret);
                        }
                    } catch (Exception x) {
                        LOG.info(() -> "tried to execute %s on %s@%d but got the error %s".formatted(
                            toolExecution.name, tool.getClass(), tool.hashCode(), x.toString()
                        ));
                        throw new ToolExecutionException(
                             (x instanceof InvocationTargetException ite) ? ite.getTargetException() : x
                        );
                    }
                    _break_(true);
                }
            });
            if (foundInClass != null) {
                _break_(true);
            }
        });

        if (found == null) {
            //
            // ToolsExecutionException always return cause. If not provided
            // it returns itself, which is not very useful. That is why we
            // create a Exception cause.
            //
            throw new ToolNotFoundException(toolExecution.name);
        }

        return result[0];
    }

    /**
     * This is to build the final system message, which is built of the following:
     *
     * 1. the prompt provided by calling {@code systemPromptProvider}
     * 2. the tool descriptions for the provided {@code tools}
     *
     * @param o - ignored
     *
     * @return the combination of the provided system prompt and tools description
     */
    private String systemPrompt(Object o) {

        //
        // 1. the provided system message
        //
        //final String systemPrompt = systemPromptProvider.apply(o);

        //
        // Tools descriptions
        //
        final JSONArray array = new JSONArray();
        on(tools).loop((toolIstance) -> {
            on(ToolSpecifications.toolSpecificationsFrom(toolIstance.getClass())).loop((toolDesc) -> {
                final JSONObject tool = new JSONObject();

                final JSONObject toolArgs = new JSONObject();
                if (toolDesc.parameters() != null) {
                    on(toolDesc.parameters().properties()).loop((name, description) -> {
                        if (description != null) {
                            toolArgs.put(name, description.description());
                        }
                    });
                }

                tool.put("name", toolDesc.name());
                if (toolDesc.description() != null) {
                    tool.put("description", toolDesc.description());
                }
                if (toolArgs.length() > 0) {
                    tool.put("arguments", toolArgs);
                }

                array.put(tool);
            });
        });

        return ToolifiedAiService.SYSTEM_MESSAGE
            .replace(
                "{{" + SystemPromptInfo.TOOLS.value + "}}", JToon.encodeJson(array.toString())
            )
            .replace(
                "{{" + SystemPromptInfo.GLOBAL_RULES.value + "}}", systemPromptInfo.get(SystemPromptInfo.GLOBAL_RULES)
            ).replace(
                "{{" + SystemPromptInfo.PROJECT_RULES.value + "}}", systemPromptInfo.get(SystemPromptInfo.PROJECT_RULES)
            ).replace(
                "{{" + SystemPromptInfo.PROJECT_INFO.value + "}}", systemPromptInfo.get(SystemPromptInfo.PROJECT_INFO)
            );
    }

    private ToolExecutionRequest toolExecutionRequest(final ExecutionJSONObject execution) {
        return ToolExecutionRequest.builder()
            .id(execution.name + '@' + execution.name.hashCode())
            .name(execution.name)
            .arguments(String.valueOf(execution.arguments()))
            .build();
    }

    private ChatResponse responseWithTools(final List<ExecutionJSONObject> executions) {
        final List<ToolExecutionRequest> toolExecutionRequests = new ArrayList();
        on(executions).loop((execution) -> {
            toolExecutionRequests.add(toolExecutionRequest(execution));
        });

        final ChatResponse response = listenersAdapter.lastResponse;
        final AiMessage currentAIMessage =  response.aiMessage();
        ChatResponse.Builder builder = response.toBuilder().aiMessage(
            AiMessage.from(currentAIMessage.text(), toolExecutionRequests)
        );

        return builder.build();
    }

    // -------------------------------------------------- JeddictListenerAdapter

    public static class JeddictListenerAdapter implements ChatModelListener {

        protected ChatRequest lastRequest = null;
        protected ChatResponse lastResponse = null;

        public final List<JeddictBrainListener> listeners = new ArrayList();

        public JeddictListenerAdapter(final List<JeddictBrainListener> listeners) {
            this.listeners.addAll(listeners);
        }

        @Override
        public void onRequest(ChatModelRequestContext requestContext) {
            on(listeners).loop((listener) -> {
                listener.onRequest(requestContext.chatRequest());
            });
        }

        @Override
        public void onResponse(ChatModelResponseContext responseContext) {
            lastRequest = responseContext.chatRequest();
            lastResponse = responseContext.chatResponse();
        }

        @Override
        public void onError(ChatModelErrorContext errorContext) {
            on(listeners).loop((listener) -> {
                listener.onError(errorContext.error());
            });
        }

    }

}
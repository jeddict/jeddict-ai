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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.FileSystemTools;
import io.github.jeddict.ai.agent.UtilTools;
import static io.github.jeddict.ai.agent.pair.HackerWithToolsTest.GLOBAL_RULES;
import static io.github.jeddict.ai.agent.pair.HackerWithToolsTest.PROJECT_INFO;
import static io.github.jeddict.ai.agent.pair.HackerWithToolsTest.PROJECT_RULES;
import io.github.jeddict.ai.agent.pair.HackerWithoutTools.JeddictListenerAdapter;
import io.github.jeddict.ai.lang.DummyJeddictBrainListener;
import io.github.jeddict.ai.test.DummyChatModel;
import io.github.jeddict.ai.test.DummyChatModelListener;
import io.github.jeddict.ai.test.DummyTool;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

/**
 *
 */
public class HackerWithoutToolsTest {

    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ARGUMENTS = "arguments";

    @TempDir
    Path basedir;

    final DummyChatModel MODEL = chatModel();
    final AiServices<ToolifiedAiService> BUILDER =
        AiServices.builder(ToolifiedAiService.class)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
        .chatModel(MODEL);

    @Test
    public void pair_is_a_PairProgrammer() {
        final HackerWithoutTools pair = new HackerWithoutTools(MODEL, BUILDER, List.of());
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void listeners_configuration_at_construction() {
        //
        // The model provided to HackerWithoutTools() must have one listener of
        // type HackerWithoutTools.ListenerController, which will be used to
        // properly simulate the chain of events of models supporting tools.
        // We want to pass it in the constructor to avoid that HackerWithoutTools
        // manipulates the listeners by its own in the constructor.
        // It must be the only one otherwise we may end up with
        //

        //
        // OK
        //
        then(new HackerWithoutTools(MODEL, BUILDER, List.of()).listenersAdapter)
            .isNotNull().isInstanceOf(JeddictListenerAdapter.class);
        //
        // KO
        //
        MODEL.listeners().clear(); MODEL.listeners().add(new DummyChatModelListener());
        thenThrownBy(() -> {
            new HackerWithoutTools(MODEL, BUILDER, List.of());
        }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("the model must have one listener of type HackerWithoutTool.JeddictListenerAdapter");
    }

    @Test
    public void hack_returns_AI_provided_response() {
        final String expectedSystem = ToolifiedAiService.SYSTEM_MESSAGE
            .replace("{{tools}}",
            """
            [1]:
              - name: echo
                description: Echo the given message back as return value
                arguments:
                  message: message to echo
            """
            ).replace("{{globalRules}}", "")
            .replace("{{projectRules}}", "")
            .replace("{{projectInfo}}", "");
        final String prompt = "use mock 'hello world.txt'";

        final HackerWithoutTools pair = new HackerWithoutTools(MODEL, BUILDER, List.of());

        final String answer = pair.hack(prompt);

        final JeddictListenerAdapter listener = modelListener();

        final SystemMessage system = (SystemMessage)listener.lastRequest.messages().get(0);
        then(system.text()).isEqualToIgnoringNewLines(expectedSystem);

        then(answer).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void hack_with_rules_returns_AI_provided_response() {
        final String expectedSystem = ToolifiedAiService.SYSTEM_MESSAGE
        .replace("{{tools}}",
            """
            [1]:
              - name: echo
                description: Echo the given message back as return value
                arguments:
                  message: message to echo
            """
            ).replace("{{globalRules}}", GLOBAL_RULES)
            .replace("{{projectRules}}", PROJECT_RULES)
            .replace("{{projectInfo}}", PROJECT_INFO);
        final String prompt = "use mock 'hello world.txt'";
        final HackerWithoutTools pair = new HackerWithoutTools(MODEL, BUILDER, List.of());

        final String answer = pair.hack(prompt, GLOBAL_RULES, PROJECT_RULES, PROJECT_INFO);

        final ChatRequest request = modelListener().lastRequest;

        final SystemMessage system = (SystemMessage)request.messages().get(0);
        then(system.text()).isEqualToIgnoringNewLines(expectedSystem);

        then(answer).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void hack_with_streaming_throws_IllegalArgumentException() throws IOException {
        final String ERROR = "HackerWithoutTool does not support streaming; use hack(prompt, globalRules, ProjectRules, projectInfo) instead";
        //
        // ChatModel, but trying to use the streaming hack()
        //
        final HackerWithoutTools pair = new HackerWithoutTools(MODEL, BUILDER, List.of());

        thenThrownBy(() -> {
            pair.hack(null, "prompt", GLOBAL_RULES, PROJECT_RULES, PROJECT_INFO);
        }).isInstanceOf(IllegalArgumentException.class).hasMessage(ERROR);
    }

    @Test
    public void list_of_tools() throws Exception {
        final DummyTool dummyTools = new DummyTool();
        final UtilTools utilTools = new UtilTools();

        HackerWithoutTools hacker = new HackerWithoutTools(
            MODEL, BUILDER, List.of(dummyTools)
        );

        then(hacker.tools).hasSize(2);
        then(hacker.tools).contains(dummyTools);
        then((boolean)on(hacker.tools).loop((t) -> {
            if (t instanceof UtilTools) {
                _break_(true);
            }
        })).isTrue();

        hacker = new HackerWithoutTools(
            MODEL, BUILDER, List.of(dummyTools, utilTools)
        );
        then(hacker.tools).containsExactlyInAnyOrder(dummyTools, utilTools);
    }

    @Test
    public void system_message_contains_tool_description() throws Exception {
        final List<AbstractTool> tools = List.of(new DummyTool());

        final HackerWithoutTools hacker = new HackerWithoutTools(
            MODEL, BUILDER, tools
        );

        hacker.hack("use mock 'hello world.txt'");

        final JeddictListenerAdapter listener = modelListener();
        final ChatRequest r = (ChatRequest)listener.lastRequest;
        final SystemMessage m = (SystemMessage)r.messages().get(0);
        String systemMessage = m.text();

        then(systemMessage)
            .startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50))
            .contains("[8]:\n")
            .contains(
            """
              - name: dummyTool
                description: simple tool that does nothing
            """)
            .contains(
            """
              - name: dummyToolWithArgs
                description: simple tool that does nothing but with arguments
                arguments:
                  arg1: the first argument
            """)
            .contains(
            """
              - name: dummyToolRead
                description: simple READONLY tool that does nothing
            """)
            .contains(
            """
              - name: dummyToolInteractive
                description: simple INTERACTIVE tool that does nothing
            """)
            .contains(
            """
              - name: dummyToolWrite
                description: simple READWRITE tool that does nothing
            """)
            .contains(
            """
              - name: dummyToolUnknown
            """)
            .contains(
            """
              - name: dummyToolError
            """)
            .contains(
            """
              - name: echo
                description: Echo the given message back as return value
                arguments:
                  message: message to echo
            """);
    }

    @Test
    public void execute_tools() throws Exception {
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);

        //
        // No arguments
        //
        HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER, tools);

        hacker.hack("use mock 'dummy tool.txt'");
        then(tool.executed()).isTrue();
        then(tool.arguments()).isNull();

        //
        // With arguments
        //
        tool.reset();
        hacker = new HackerWithoutTools(MODEL, BUILDER, tools);
        hacker.hack("use mock 'dummy tool with args.txt'");
        then(tool.executed()).isTrue();
        then(tool.arguments())
            .containsExactly("val1", List.of("val2"));
    }

    @Test
    public void execute_tool_chat_session() throws Exception {
        final String USER_PROMPT = "use mock 'multi dummy tool.1.txt'";

        //
        // When a tool is executed, the hacker shall return back autonomously to
        // the LLM to deliver the result of the execution
        //
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);

        final HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER, tools);

        hacker.hack(USER_PROMPT);

        //
        // 1. --> prompt
        // 2. <-- llm response with tool execution
        // 3. --> tool execution result
        // 4. <-- llm response
        //
        final JeddictListenerAdapter listener = modelListener();
        final ChatRequest lastRequest = listener.lastRequest;
        final ChatResponse lastResponse = listener.lastResponse;

        then(lastResponse.aiMessage().text()).isEqualToIgnoringNewLines("Done executing the DummyTool");
        Object[] messages = lastRequest.messages().toArray();
        then(messages).hasSize(6);


        SystemMessage systemPrompt = null;
        UserMessage userMessage = null;
        AiMessage aiMessage = null;

        int i = 0;
        systemPrompt = (SystemMessage)messages[i++];
        then(systemPrompt.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo(USER_PROMPT);

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("first round");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo("dummyTool: OK\ntrue");

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("second round");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).contains("dummyToolWithArgs: OK\ntrue\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
    }

    @Test
    public void provide_error_message_if_tool_does_not_exist() throws Exception {
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool);

        final HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER, tools);

        hacker.hack("use mock 'not existing tool.txt'");

        final ChatRequest request = modelListener().lastRequest;
        final UserMessage msg = (UserMessage)request.messages().get(3);
        then(msg.contents().toString()).contains("iDoNotExist: ERR java.lang.RuntimeException: tool iDoNotExist not found");
    }

    @Test
    public void send_error_on_tool_execution_exception() throws Exception {
        final String USER_PROMPT = "use mock 'tool in error.1.txt'";
        final DummyTool tool = new DummyTool();
        final List<AbstractTool> tools = List.of(tool, new UtilTools());

        final HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER, tools);

        hacker.hack(USER_PROMPT);

        final ChatRequest lastRequest = hacker.listenersAdapter.lastRequest;

        Object[] messages = lastRequest.messages().toArray();
        then(messages).hasSize(6);

        SystemMessage systemPrompt = null;
        UserMessage userMessage = null;
        AiMessage aiMessage = null;

        int i = 0;
        systemPrompt = (SystemMessage)messages[i++];
        then(systemPrompt.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo(USER_PROMPT);

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).contains("This tool throws an exception");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).isEqualTo("dummyToolError: ERR java.lang.RuntimeException: error in dummyTool");

        aiMessage = (AiMessage)messages[i++];
        then(aiMessage.text()).startsWith("This tool execution is malformed");

        userMessage = (UserMessage)messages[i++];
        then(userMessage.singleText()).startsWith("echo: OK\nERR error parsing tool execution for malformedTool");
    }

    @Test
    public void create_calculator_application() throws Exception {
        final FileSystemTools tools = new FileSystemTools(basedir.toAbsolutePath().toString());

        final HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER,  List.of(tools));

        hacker.hack("use mock 'create calculator.txt'");

        then(basedir.resolve("calculator")).exists();
        then(basedir.resolve("calculator/pom.xml")).exists();
        then(basedir.resolve("calculator/src/main/java/com/example/calculator/CalculatorApp.java")).exists();
        then(basedir.resolve("calculator/src/main/resources")).exists();
        then(basedir.resolve("calculator/README.md")).exists();
    }

    @Test
    public void all_listeners_receive_all_events_ok() throws IOException {
        final String USER_PROMPT = "use mock 'multi dummy tool.1.txt'";
        final DummyTool tool = new DummyTool();

        final DummyJeddictBrainListener
            listener1 = new DummyJeddictBrainListener(),
            listener2 = new DummyJeddictBrainListener();

        modelListener().listeners.addAll(List.of(listener1, listener2));
        tool.addListener(listener1); tool.addListener(listener2);
        final HackerWithoutTools hacker = new HackerWithoutTools(MODEL, BUILDER, List.of(tool));
        hacker.hack(USER_PROMPT);

        //
        // 0 - chatStarted
        //
        final AtomicInteger i = new AtomicInteger();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatStarted");

            final SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
            final UserMessage u = (UserMessage)((Object[])args.getRight())[1];
            then(s.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));
            then(u.singleText()).isEqualToIgnoringNewLines(USER_PROMPT);
        });

        //
        // 1 - chatRequest - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));
            then(u.singleText()).startsWith(USER_PROMPT);
        });

        //
        // 2 - chatResponse - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isTrue();
        });

        //
        // 3 - toolProgress
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onProgress");

            then(args.getRight()).isEqualTo("\nexecuting dummyTool");
        });

        //
        // 4 - toolExecuted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onToolExecuted");

            final Object[] exec = (Object[])args.getRight();
            final ToolExecutionRequest request = (ToolExecutionRequest)exec[0];
            final String result = (String)exec[1];

            then(request.name()).isEqualTo("dummyTool");
            then(result).isEqualTo("true");
        });

        //
        // 5 - chatRequest - tool execution result
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            then(req.messages()).hasSize(4);
            then(((UserMessage)req.messages().get(3)).singleText()).isEqualTo("dummyTool: OK\ntrue");
        });

        //
        //  6 - chatResponse
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isTrue();
        });

        //
        // 7 - toolProgress
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onProgress");

            then((String)args.getRight()).startsWith("\nexecuting dummyToolWithArgs");
        });

        //
        // 8 - toolExecuted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onToolExecuted");

            final Object[] exec = (Object[])args.getRight();
            final ToolExecutionRequest request = (ToolExecutionRequest)exec[0];
            final String result = (String)exec[1];

            then(request.name()).isEqualTo("dummyToolWithArgs");
            then(result).isEqualTo("true\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
        });

        //
        // 9 - chatRequest - tool execution result
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            then(req.messages()).hasSize(6);
            then(((UserMessage)req.messages().get(5)).singleText())
                .isEqualTo("dummyToolWithArgs: OK\ntrue\narg1: Hello World\narg2: [Hello Paris, Hello Delhi, Hello Rome]");
        });

        //
        //  10 - chatResponse
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith(ToolifiedAiService.SYSTEM_MESSAGE.substring(0, 50));
            then(u.singleText()).startsWith(USER_PROMPT);
            then(res.aiMessage().hasToolExecutionRequests()).isFalse();
        });

        //
        // 11 - chatCompleted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatCompleted");

            final ChatResponse res = (ChatResponse)args.getRight();
            then(res.aiMessage().text()).isEqualToIgnoringNewLines("Done executing the DummyTool");
        });
    }

/*
    @Test
    public void limit_the_number_of_req_res() throws IOException {
        final String SYSTEM_PROMPT = "use mock 'multi dummy tool.txt'";
        final String USER_PROMPT = "let's go!";
        final DummyTool tool = new DummyTool();
        final DummyChatModel model = chatModel();
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();

        final HackerWithoutTools hacker = new HackerWithoutTools(
            "endpoint", "apikey", "dummy", (o) -> SYSTEM_PROMPT, List.of(tool), model
        );
        hacker.addListener(listener); hacker.maxIterations(1);

        hacker.hack(USER_PROMPT);

        //
        // just one iteration
        //
        then(listener.collector).hasSize(5);

        listener.collector.clear();
        hacker.maxIterations(10);

        hacker.hack(USER_PROMPT);

        //
        // full roundtrip
        //
        then(listener.collector).hasSize(7);
    }

    */
    // --------------------------------------------------------- private methods

    /**
     * A model used in LLMToolify must be initialized with the listener adapter
     * so that events generated by the model can be properly manipulated and
     * routed by HackerWithoutTool
     */
    private DummyChatModel chatModel() {

        final DummyJeddictBrainListener jeddictBrainListener = new DummyJeddictBrainListener();
        final DummyChatModel model = new DummyChatModel();

        model.listeners().add(new JeddictListenerAdapter(List.of(jeddictBrainListener)));

        return model;
    }

    private JeddictListenerAdapter modelListener() {
        return (JeddictListenerAdapter)MODEL.listeners().get(0);
    }
}

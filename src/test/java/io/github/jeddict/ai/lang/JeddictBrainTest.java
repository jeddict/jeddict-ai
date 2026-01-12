/*
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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.pair.Assistant;
import io.github.jeddict.ai.agent.pair.Hacker;
import io.github.jeddict.ai.agent.pair.HackerWithTools;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.ASSISTANT;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.HACKER;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.TEST;
import io.github.jeddict.ai.agent.pair.Shakespeare;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.test.TestBase;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;
import org.apache.commons.lang3.tuple.Pair;
import static ste.lloop.Loop.on;


/**
 * The JeddictBrainTest class is a test class that extends TestBase.
 * It contains unit tests for the JeddictBrain class, verifying its constructors,
 * listener management, and functionality such as code analysis.
 */
//
// TODO. argument sanity check in constructors
//
public class JeddictBrainTest extends TestBase {

    @Test
    public void constructors() throws Exception {
        final String N1 = "dummy", N2 = "dummy2";
        final List<AbstractTool> T = List.of();

        PreferencesManager.getInstance().setStreamEnabled(true);

        JeddictBrain brain = new JeddictBrain(N1, true);

        then(brain.modelName).isSameAs(N1);
        then(brain.streaming).isTrue();
        then(brain.tools).isEmpty();

        brain = new JeddictBrain(N2, false);

        then(brain.modelName).isSameAs(N2);
        then(brain.streaming).isFalse();
        then(brain.tools).isEmpty();

        final DummyTool D = new DummyTool();
        brain = new JeddictBrain(N2, true, InteractionMode.AGENT, List.of(D));

        then(brain.modelName).isSameAs(N2);
        then(brain.streaming).isTrue();
        then(brain.tools).isNotSameAs(T).containsExactly(D);
    }

    @Test
    public void constructors_sanity_check() {
        thenThrownBy(() -> {
            new JeddictBrain(null, false);
        }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("modelName can not be null");
    }

    @Test
    void add_and_remove_listeners() {
        final JeddictBrain brain = new JeddictBrain("dummy", false);

        final JeddictBrainListener listener1 = new DummyJeddictBrainListener();
        final JeddictBrainListener listener2 = new DummyJeddictBrainListener();

        then(brain.listeners()).isEmpty();

        brain.addListener(listener1);
        then(brain.listeners()).containsExactly(listener1);

        brain.addListener(listener2);
        then(brain.listeners()).containsExactly(listener1, listener2);

        brain.removeListener(listener1);
        then(brain.listeners()).containsExactly(listener2);

        brain.removeListener(listener2);
        then(brain.listeners()).isEmpty();

        // Sanity checks
        thenThrownBy(() -> brain.addListener(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("listener cannot be null");

        // Should not throw when removing null or non-existent listener
        brain.removeListener(null);
        brain.removeListener(new DummyJeddictBrainListener());
    }

    @Test
    public void all_listeners_receive_receive_all_events_ok() {
        final DummyTool tool = new DummyTool();
        final JeddictBrain brain = new JeddictBrain("dummy-with-tools", false, InteractionMode.AGENT, List.of(tool));

        final DummyJeddictBrainListener listener1 = new DummyJeddictBrainListener(),
            listener2 = new DummyJeddictBrainListener();

        brain.probedModels.put("dummy-with-tools", Boolean.TRUE);
        brain.addListener(listener1);
        brain.addListener(listener2);
        tool.addListener(listener1);
        tool.addListener(listener2);

        final Hacker a = brain.pairProgrammer(HACKER);
        a.hack("execute tool dummyTool", "", "", "");

        //
        // chatStarted
        //
        final AtomicInteger i = new AtomicInteger();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatStarted");

            final SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
            final UserMessage u = (UserMessage)((Object[])args.getRight())[1];
            then(s.text()).startsWith("You are an expert software developer");
            then(u.singleText()).startsWith("execute tool dummyTool");
        });

        //
        // chatRequest - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith("You are an expert software developer");
            then(u.singleText()).startsWith("execute tool dummyTool");
        });

        //
        // chatResponse - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);

            then(s.text()).startsWith("You are an expert software developer");
            then(u.singleText()).startsWith("execute tool dummyTool");
            then(res.aiMessage().hasToolExecutionRequests()).isTrue();
        });

        //
        // toolProgress
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onProgress");

            then(args.getRight()).isEqualTo("executing dummyTool");
        });

        //
        // toolExecuted
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
        // chatRequest - tool execution result
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            then(req.messages()).hasSize(4);
        });

        //
        //  chatResponse
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onResponse");

            final ChatRequest req = (ChatRequest)((Object[])args.getRight())[0];
            final ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];

            then(req.messages()).hasSize(4); // System, User, Ai, ToolExecutionResult
            then(res.aiMessage().text()).isEqualTo("true");
        });

        //
        // chatCompleted
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatCompleted");

            final ChatResponse res = (ChatResponse)args.getRight();
            then(res.aiMessage().text()).isEqualToIgnoringNewLines("true");
        });
    }

    @Test
    public void all_listeners_receive_receive_all_events_error() {
        //
        // Given a JeddictBrain with a dummy model in QUERY mode that will throw
        // an error
        //
        final JeddictBrain brain = new JeddictBrain("dummy-with-error", false);

        //
        // Given more than one listener
        //
        final DummyJeddictBrainListener listener1 = new DummyJeddictBrainListener(),
            listener2 = new DummyJeddictBrainListener();
        brain.addListener(listener1);
        brain.addListener(listener2);


        //
        // When chatting...
        //
        final Assistant a = brain.pairProgrammer(ASSISTANT);
        try { a.chat("hello world"); } catch (RuntimeException x) {}

        //
        // Then the error is signalled as an event
        //

        //
        // chatStarted
        //
        final AtomicInteger i = new AtomicInteger();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onChatStarted");

            final SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
            final UserMessage u = (UserMessage)((Object[])args.getRight())[1];
            then(s.text()).startsWith("You are an expert software developer");
            then(u.singleText()).startsWith("hello world");
        });

        //
        // chatRequest - prompt
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onRequest");

            final ChatRequest req = (ChatRequest)args.getRight();

            final SystemMessage s = (SystemMessage)req.messages().get(0);
            final UserMessage u = (UserMessage)req.messages().get(1);
            then(s.text()).startsWith("You are an expert software developer");
            then(u.singleText()).startsWith("hello world");
        });

        //
        // error
        //
        i.getAndIncrement();
        on(listener1, listener2).loop((listener) -> {
            final Pair<String, Object> args = listener.collector.get(i.get());

            then(args.getLeft()).isEqualTo("onError");

            final Throwable t = (Throwable)args.getRight();
            then(t).hasMessage("something went wrong");
        });
    }

    @Test
    public void get_new_pair_programmer() {
        final JeddictBrain brain = new JeddictBrain("dummy-with-tools", false);

        for (PairProgrammer.Specialist s: PairProgrammer.Specialist.values()) {
            final Object pair1 = brain.pairProgrammer(s);
            final Object pair2 = brain.pairProgrammer(s);

            then(pair1).isNotNull(); then(pair2).isNotNull();
            if (s == TEST) {
                then(pair1).isInstanceOf(s.specialistClass);
                then(pair2).isInstanceOf(s.specialistClass);
            } else {
                then(pair1.getClass().getInterfaces()).contains(s.specialistClass);
                then(pair2.getClass().getInterfaces()).contains(s.specialistClass);
            }
            then(pair2).isNotSameAs(pair1);  // create a new pair every call
        }
    }

    /**
     * There are two types of agents, created with two builders: interactive and
     * non interactive. Both must receive events.
     */
    @Test
    public void interactive_and_not_interactove_agents_receive_events() {
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        final JeddictBrain brain = new JeddictBrain(false);

        brain.addListener(listener); then(listener.collector).isEmpty();

        //
        // 1. Non interactive
        //
        final Shakespeare pair = brain.pairProgrammer(PairProgrammer.Specialist.SHAKESPEARE);

        pair.review("a message", "the text", "the code");

        then(listener.collector).hasSize(2);

        //
        // 2. Interactive
        //
        listener.collector.clear();

        final Assistant assistant = brain.pairProgrammer(PairProgrammer.Specialist.ASSISTANT);

        assistant.chat("hello");

        then(listener.collector).hasSize(4);
    }

    @Test
    public void get_agentic_haker_with_tools() {
        final DummyTool tool = new DummyTool();
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.AGENT, List.of(tool)
        );

        HackerWithTools h = brain.pairProgrammer(HACKER);

        h.hack("execute tool dummyTool");

        then(tool.executed()).isTrue();
    }

    @Test
    public void get_agentic_Hacker_streaming() {
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        final DummyTool tool = new DummyTool();
        final String[] msg = new String[2];
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", true,
            InteractionMode.AGENT, List.of(tool)
        );
        brain.addListener(listener);
        brain.probedModels.put("dummy-with-tools", Boolean.TRUE);

        HackerWithTools h = brain.pairProgrammer(HACKER);

        h.hack(listener, "execute tool dummyTool");

        then(tool.executed()).isTrue();

        int i = 0;
        then(listener.collector).isNotEmpty().hasSize(8);

        //
        // chatStarted
        //
        Pair<String, Object> args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onChatStarted");

        SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
        UserMessage u = (UserMessage)((Object[])args.getRight())[1];
        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("execute tool dummyTool");

        //
        // chatRequest - prompt
        //
        args = listener.collector.get(i++);

        then(args.getLeft()).isEqualTo("onRequest");

        ChatRequest req = (ChatRequest)args.getRight();
        s = (SystemMessage)req.messages().get(0);
        u = (UserMessage)req.messages().get(1);

        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("execute tool dummyTool");

        //
        // chatResponse
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onResponse");

        req = (ChatRequest)((Object[])args.getRight())[0];
        ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
        s = (SystemMessage)req.messages().get(0);
        u = (UserMessage)req.messages().get(1);
        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("execute tool dummyTool");
        then(res.aiMessage().hasToolExecutionRequests()).isTrue();

        //
        //  toolExecuted
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onToolExecuted");

        Object[] exec = (Object[])args.getRight();
        ToolExecutionRequest request = (ToolExecutionRequest)exec[0];
        String result = (String)exec[1];

        then(request.name()).isEqualTo("dummyTool");
        then(result).isEqualTo("true");

        //
        // chatRequest - tool execution result
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onRequest");

        req = (ChatRequest)args.getRight();
        then(req.messages()).hasSize(4);

        //
        // progress
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onProgress");
        then(args.getRight()).isEqualTo("true");

        //
        // chatResponse
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onResponse");

        req = (ChatRequest)((Object[])args.getRight())[0];
        res = (ChatResponse)((Object[])args.getRight())[1];
        then(req.messages()).hasSize(4); // (System, User, Ai, TioolExecutionResult)
        then(u.singleText()).startsWith("execute tool dummyTool");
        then(res.aiMessage().text()).isEqualTo("true");

        //
        //  chatCompleted
        //
        args = listener.collector.get(i++);

        then(args.getLeft()).isEqualTo("onChatCompleted");

        res = (ChatResponse)args.getRight();
        then(res.aiMessage().text()).isEqualToIgnoringNewLines("true");
    }

    @Test
    public void get_assistant_chat_and_streaming() {
        JeddictBrain brain = new JeddictBrain("dummy", false);

        Assistant a = brain.pairProgrammer(ASSISTANT);

        then(a.chat("use mock 'hello world.txt'")).isEqualToIgnoringNewLines("hello world");

        final DummyJeddictBrainListener listener
            = new DummyJeddictBrainListener();
        brain = new JeddictBrain("dummy", true);
        brain.addListener(listener);
        a = brain.pairProgrammer(ASSISTANT);

        a.chat(listener, "use mock 'hello world.txt'");

        int i = 0;
        then(listener.collector).isNotEmpty().hasSize(5);

        //
        // chatStarted
        //
        //
        // chatStarted
        //
        Pair<String, Object> args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onChatStarted");

        SystemMessage s = (SystemMessage)((Object[])args.getRight())[0];
        UserMessage u = (UserMessage)((Object[])args.getRight())[1];
        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("use mock 'hello world.txt'");

        //
        // chatRequest - prompt
        //
        args = listener.collector.get(i++);

        then(args.getLeft()).isEqualTo("onRequest");

        ChatRequest req = (ChatRequest)args.getRight();
        s = (SystemMessage)req.messages().get(0);
        u = (UserMessage)req.messages().get(1);

        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("use mock 'hello world.txt'");

        //
        // progress
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onProgress");
        then(args.getRight()).isEqualTo("hello world");

        //
        // chatResponse
        //
        args = listener.collector.get(i++);
        then(args.getLeft()).isEqualTo("onResponse");
        req = (ChatRequest)((Object[])args.getRight())[0];
        ChatResponse res = (ChatResponse)((Object[])args.getRight())[1];
        s = (SystemMessage)req.messages().get(0);
        u = (UserMessage)req.messages().get(1);
        then(s.text()).startsWith("You are an expert software developer");
        then(u.singleText()).startsWith("use mock 'hello world.txt'");
        then(res.aiMessage().text()).isEqualToIgnoringNewLines("hello world");

        //
        //  chatCompleted
        //
        args = listener.collector.get(i++);

        then(args.getLeft()).isEqualTo("onChatCompleted");

        res = (ChatResponse)args.getRight();
        then(res.aiMessage().text()).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void with_and_without_memory() {
        final JeddictBrain brain = new JeddictBrain(false);

        //
        // The default is no memory
        //
        then(brain.memorySize()).isEqualTo(0);

        then(brain.withMemory(10)).isEqualTo(brain);
        then(brain.memorySize()).isEqualTo(10);

        thenThrownBy(() -> brain.withMemory(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be greather than 0 (where 0 means no memory)");
    }

    @Test
    public void wrap_tools_if_interactive() {
        final StringBuilder sb = new StringBuilder();
        final DummyTool tools = new DummyTool();
        final Function<String, Boolean> defaultInteraction = (s) -> {
            sb.append(s);  return true;
        };
        JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.INTERACTIVE,
            defaultInteraction,
            List.of(new DummyTool())
        );

        then(brain.tools.get(0).getClass()).isNotEqualTo(tools.getClass());

        brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.AGENT,
            defaultInteraction,
            List.of(new DummyTool())
        );

        then(brain.tools.get(0).getClass()).isEqualTo(tools.getClass());

        brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.ASK,
            defaultInteraction,
            List.of(new DummyTool())
        );

        then(brain.tools).isEmpty();
    }

    @Test
    public void in_interactive_mode_use_default_hitm_if_not_provided() {
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.INTERACTIVE,
            null,
            List.of(new DummyTool())
        );

        final DummyTool tool = (DummyTool)brain.tools.get(0);

        final Hacker h = brain.pairProgrammer(PairProgrammer.Specialist.HACKER);

        //
        // No policy tool
        //
        h.hack("execute tool dummyTool", "", "", "");
        then(tool.executed()).isFalse();

        //
        // Read policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolRead", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Write policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolWrite", "", "", "");
        then(tool.executed()).isFalse();

        //
        // Interactive policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolInteractive", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Unknown policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolUnkown", "", "", "");
        then(tool.executed()).isFalse();
    }

    @Test
    public void in_agent_mode_do_not_use_any_hitm() {
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", false,
            InteractionMode.AGENT,
            (s) -> { throw new ToolExecutionException("simulated exception"); },
            List.of(new DummyTool())
        );

        final DummyTool tool = (DummyTool)brain.tools.get(0);

        final Hacker h = brain.pairProgrammer(PairProgrammer.Specialist.HACKER);

        //
        // No policy tool
        //
        h.hack("execute tool dummyTool", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Read policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolRead", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Write policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolWrite", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Interactive policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolInteractive", "", "", "");
        then(tool.executed()).isTrue();

        //
        // Unknown policy tool
        //
        tool.reset();
        h.hack("execute tool dummyToolUnknown", "", "", "");
        then(tool.executed()).isTrue();
    }

    @Test
    public void toolExecutionErrorHandler_provides_text_message() {
        final JeddictBrain brain = new JeddictBrain(false);

        //
        // If a TargetExecutionException (as it usually wraps the exception
        // thrown by the tool, get the message from the target exception.
        //
        final Throwable error = new InvocationTargetException(new ToolExecutionException("error"));
        then(brain.toolExecutionErrorHandler(error, null).text()).isEqualTo("ToolExecutionException:error");

        then(brain.toolExecutionErrorHandler(
            new InvocationTargetException(new RuntimeException()), null).text()
        ).isEqualTo("RuntimeException:");

        then(brain.toolExecutionErrorHandler(new Exception(), null).text())
            .isEqualTo("Exception:");

        then(brain.toolExecutionErrorHandler(new Exception("unknown error"), null).text())
            .isEqualTo("Exception:unknown error");

    }
}

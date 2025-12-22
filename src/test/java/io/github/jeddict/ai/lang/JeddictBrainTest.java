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

import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.pair.Assistant;
import io.github.jeddict.ai.agent.pair.Hacker;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.ASSISTANT;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.HACKER;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.TEST;
import static io.github.jeddict.ai.lang.JeddictBrain.EventProperty.CHAT_COMPLETED;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.test.DummyPropertyChangeListener;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.test.TestBase;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;


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
        brain = new JeddictBrain(N2, true, JeddictBrain.InteractionMode.AGENT, List.of(D));

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
    public void add_and_remove_listeners() {
        final String N = "jeddict";

        final PropertyChangeListener L1 = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) { }
        },
        L2 = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) { }
        };

        final JeddictBrain brain = new JeddictBrain(N, false);

        then(brain.getSupport().getPropertyChangeListeners()).isEmpty();

        brain.addProgressListener(L1);
        brain.addProgressListener(L2);
        then(brain.getSupport().getPropertyChangeListeners()).containsExactlyInAnyOrder(L1, L2);

        brain.removeProgressListener(L2);
        then(brain.getSupport().getPropertyChangeListeners()).containsExactly(L1);

        brain.removeProgressListener(L1);
        then(brain.getSupport().getPropertyChangeListeners()).isEmpty();
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

    @Test
    public void get_agentic_Hacker() {
        final DummyTool tool = new DummyTool();
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", false,
            JeddictBrain.InteractionMode.AGENT, List.of(tool)
        );

        Hacker h = brain.pairProgrammer(HACKER);

        h.hack("execute tool dummyTool");

        then(tool.executed()).isTrue();
    }

    @Test
    public void get_agentic_Hacker_streaming() {
        final DummyPropertyChangeListener streamListener = new DummyPropertyChangeListener();
        final DummyTool tool = new DummyTool();
        final String[] msg = new String[2];
        final JeddictBrain brain = new JeddictBrain(
            "dummy-with-tools", true,
            JeddictBrain.InteractionMode.AGENT, List.of(tool)
        );

        Hacker h = brain.pairProgrammer(HACKER);

        h.hack(streamListener, "execute tool dummyTool");

        then(tool.executed()).isTrue();

        int i = 0;
        then(streamListener.events).isNotEmpty();
        PropertyChangeEvent e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.CHAT_INTERMEDIATE.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.TOOL_BEFORE_EXECUTION.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.TOOL_EXECUTED.name);
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.CHAT_PARTIAL.name);
        then(e.getNewValue()).isEqualTo("true");
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(CHAT_COMPLETED.name);
        then(((ChatResponse)e.getNewValue()).aiMessage().text()).isEqualTo("true");
    }

    @Test
    public void get_assistant_chat_and_streaming() {
        final String[] msg = new String[1];
        JeddictBrain brain = new JeddictBrain(
            "dummy", false,
            JeddictBrain.InteractionMode.QUERY, List.of()
        );

        Assistant a = brain.pairProgrammer(ASSISTANT);

        then(a.chat("use mock 'hello world.txt'")).isEqualToIgnoringNewLines("hello world");

        final DummyPropertyChangeListener streamListener
            = new DummyPropertyChangeListener();
        brain = new JeddictBrain(
            "dummy", true,
            JeddictBrain.InteractionMode.QUERY, List.of()
        );
        a = brain.pairProgrammer(ASSISTANT);

        a.chat(streamListener, "use mock 'hello world.txt'");

        int i = 0;
        then(streamListener.events).isNotEmpty();
        PropertyChangeEvent e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(JeddictBrain.EventProperty.CHAT_PARTIAL.name);
        then(e.getNewValue()).isEqualTo("hello world");
        e = streamListener.events.get(i++);
        then(e.getPropertyName()).isEqualTo(CHAT_COMPLETED.name);
        then(((ChatResponse)e.getNewValue()).aiMessage().text()).isEqualToIgnoringNewLines("hello world");
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
    /**
     * NOTE: maybe not the best design, it does not look natural. It is good
     * enough for now, it may be reviewed in the future
     *
     */
    public void handle_token_streaming_of_a_response() {
        final JeddictBrain brain = new JeddictBrain(true);
        final List<PropertyChangeEvent> events = new ArrayList();
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                events.add(event);
            }
        };

        brain.addProgressListener(listener);

        //brain.chat();
    }
}

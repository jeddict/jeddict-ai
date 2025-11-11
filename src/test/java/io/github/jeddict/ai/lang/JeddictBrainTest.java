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

import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.Specialist.TEST;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.test.TestBase;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.Test;


/**
 * The JeddictBrainTest class is a test class that extends TestBase.
 * It contains unit tests for the JeddictBrain class, verifying its constructors,
 * listener management, and functionality such as code analysis.
 */
public class JeddictBrainTest extends TestBase {

    @Test
    public void constructors() throws Exception {
        final String N1 = "dummy", N2 = "dummy2";
        final List<AbstractTool> T = List.of();

        PreferencesManager.getInstance().setStreamEnabled(true);

        JeddictBrain brain = new JeddictBrain(N1, true, T);

        then(brain.modelName).isSameAs(N1);
        then(brain.streamingChatModel).isNotEmpty();
        then(brain.chatModel).isEmpty();
        then(brain.tools).isEmpty();

        brain = new JeddictBrain(N2, false, T);

        then(brain.modelName).isSameAs(N2);
        then(brain.streamingChatModel).isEmpty();
        then(brain.chatModel).isNotEmpty();
        then(brain.tools).isEmpty();

        final DummyTool D = new DummyTool();
        brain = new JeddictBrain(N2, true, List.of(D));

        then(brain.modelName).isSameAs(N2);
        then(brain.streamingChatModel).isNotEmpty();
        then(brain.chatModel).isEmpty();
        then(brain.tools).isNotSameAs(T).containsExactly(D);
    }

    @Test
    public void constructors_sanity_check() {
        thenThrownBy(() -> {
            new JeddictBrain(null, false, List.of());
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

        final JeddictBrain brain = new JeddictBrain(N, false, List.of());

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
        final JeddictBrain brain = new JeddictBrain(false);

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
}

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

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.test.DummyStreamHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 */
@CacioTest  // TODO: this shall be removed once JeddictBrain will be free of UI dependency
public class JeddictBrainTest {

    //
    // Settings are currently saved in a file in the user home (see
    // PreferencesManager and FilePreferences). To be able to manipulate them
    // without side effects, we set up a different user home
    //
    @TempDir
    private Path HOME;

    private PreferencesManager preferences;

    @BeforeEach
    public void before() throws Exception {
        Files.copy(Paths.get("src/test/resources/settings/jeddict.json"), HOME.resolve("jeddict.json"));

        //
        // Making sure the singleton is initilazed with a testing configuration
        // file under a temporary directory
        //
        restoreSystemProperties(() -> {
            System.setProperty("user.home", HOME.toAbsolutePath().toString());

            preferences = PreferencesManager.getInstance();
        });
    }

    @Test
    public void constructors() throws Exception {
        final DummyStreamHandler H = new DummyStreamHandler();
        final String N1 = "jeddict", N2 = "jeddict2";

        JeddictBrain brain = new JeddictBrain();
        then(brain.modelName).isNull();
        then(brain.streamHandler).isEmpty();
        then(brain.streamingChatModel).isEmpty();  // TODO: I am not sure this should be exposed
        then(brain.chatModel).isEmpty();

        PreferencesManager.getInstance().setStreamEnabled(true);

        brain = new JeddictBrain(H, N1);

        then(brain.modelName).isSameAs(N1);
        then(brain.streamHandler).hasValue(H);
        then(brain.streamingChatModel).isNotEmpty();
        then(brain.chatModel).isEmpty();

        brain = new JeddictBrain(null, N1);

        then(brain.modelName).isSameAs(N1);
        then(brain.streamHandler).isEmpty();
        then(brain.streamingChatModel).isEmpty();
        then(brain.chatModel).isNotEmpty();

        PreferencesManager.getInstance().setStreamEnabled(false);

        brain = new JeddictBrain(H, N2);

        then(brain.modelName).isSameAs(N2);
        then(brain.streamHandler).isNotEmpty();
        then(brain.streamingChatModel).isEmpty();
        then(brain.chatModel).isNotEmpty();
    }

    @Test
    public void add_and_remove_listeners() {
        final DummyStreamHandler H = new DummyStreamHandler();
        final String N = "jeddict";

        final PropertyChangeListener L1 = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) { }
        },
        L2 = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) { }
        };

        JeddictBrain brain = new JeddictBrain(H, N);

        then(brain.progressListeners.getPropertyChangeListeners()).isEmpty();

        brain.addProgressListener(L1);
        brain.addProgressListener(L2);
        then(brain.progressListeners.getPropertyChangeListeners()).containsExactlyInAnyOrder(L1, L2);

        brain.removeProgressListener(L2);
        then(brain.progressListeners.getPropertyChangeListeners()).containsExactly(L1);

        brain.removeProgressListener(L1);
        then(brain.progressListeners.getPropertyChangeListeners()).isEmpty();
    }

}

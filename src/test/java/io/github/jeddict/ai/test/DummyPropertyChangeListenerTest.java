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
package io.github.jeddict.ai.test;

import java.beans.PropertyChangeEvent;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DummyPropertyChangeListenerTest {

    @Test
    public void collect_emitted_events() {
        final DummyPropertyChangeListener listener = new DummyPropertyChangeListener();

        then(listener.events).isEmpty();

        final PropertyChangeEvent EVENT1 =
            new PropertyChangeEvent(this, "TEST1", "old value 1", "new value 1");
        final PropertyChangeEvent EVENT2 =
            new PropertyChangeEvent(this, "TEST2", "old value 2", "new value 2");

        listener.propertyChange(EVENT1);
        then(listener.events).containsExactly(EVENT1);

        listener.propertyChange(EVENT2);
        then(listener.events).containsExactly(EVENT1, EVENT2);


    }
}

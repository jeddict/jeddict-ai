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
package io.github.jeddict.ai.agent.pair;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.jeddict.ai.models.DummyChatModel;
import io.github.jeddict.ai.test.DummyChatModelListener;
import io.github.jeddict.ai.test.TestBase;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 */
public abstract class PairProgrammerTestBase extends TestBase {

    public final static String TEXT = "use mock 'hello world.txt'";

    protected DummyChatModel model;
    protected DummyChatModelListener listener;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        model = new DummyChatModel();
        listener = new DummyChatModelListener();

        model.addListener(listener);
    }

    protected void thenMessagesMatch(
        final List<ChatMessage> messages, final String system, final String user
    ) {
        boolean systemOK = false, userOK = false;
        int i = 0;

        while (i<messages.size()) {
            final ChatMessage msg = messages.get(i++);
            LOG.info(() -> String.valueOf(msg));
            if (msg.type() == ChatMessageType.SYSTEM) {
                LOG.info(() -> '\n' + String.valueOf(msg) + '\n' + String.valueOf(new SystemMessage(system)));
                systemOK = systemOK || ((SystemMessage)msg).equals(new SystemMessage(system));
            } else if (msg.type() == ChatMessageType.USER) {
                LOG.info(() -> '\n' + String.valueOf(msg) + '\n' + String.valueOf(new UserMessage(user)));
                userOK = userOK || ((UserMessage)msg).equals(new UserMessage(user));
            }
        }

        LOG.info("systemOK: " + systemOK + ", userOK: " + userOK);

        then(systemOK).isTrue();
        then(userOK).isTrue();
    }
}

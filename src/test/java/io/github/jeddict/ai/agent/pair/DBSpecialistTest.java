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

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DBSpecialistTest extends PairProgrammerTestBase {

    final String DESCRIPTION = "additional user provided context";

    private DBSpecialist pair;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(DBSpecialist.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void suggestCommitMessage_AI_provided_response() {
        final String QUERY = "use mock 'hello world.txt'";
        final String METADATA = "this is the metadata";
        final String[] RULES = { "these are some rules", "these are other rules " };

        for (String rule: RULES) {
            final String expectedSystem = DBSpecialist.SYSTEM_MESSAGE
                .replace("{{rules}}", rule);
            final String expectedUser = DBSpecialist.USER_MESSAGE
                .replace("{{prompt}}", QUERY)
                .replace("{{metadata}}", METADATA);

            String answer = pair.assistDbMetadata(QUERY, METADATA, rule);

            final ChatModelRequestContext request = listener.lastRequestContext.get();
            thenMessagesMatch(
                request.chatRequest().messages(), expectedSystem, expectedUser
            );

            then(answer.trim()).isEqualTo("hello world");
        }
    }

}

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
import io.github.jeddict.ai.agent.pair.DiffSpecialist.CodeReviewLevel;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DiffSpecialistTest extends PairProgrammerTestBase {

    final String DESCRIPTION = "additional user provided context";
    final String DIFF = "use mock 'hello world.txt'";

    private DiffSpecialist pair;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(DiffSpecialist.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void suggestCommitMessage_AI_provided_response() {
        final String expectedSystem = DiffSpecialist.SYSTEM_MESSAGE
            .replace("{{format}}", "");
        final String expectedUser = DiffSpecialist.USER_MESSAGE
            .replace("{{prompt}}", DiffSpecialist.USER_MESSAGE_COMMENT.formatted(DESCRIPTION))
            .replace("{{diff}}", DIFF)
            .replace("{{description}}", DESCRIPTION);

        pair.suggestCommitMessages(DIFF, DESCRIPTION);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );
    }

    @Test
    public void suggestCommitMessage_null_description() {
        final String expectedSystem = DiffSpecialist.SYSTEM_MESSAGE
            .replace("{{format}}", "");
        String expectedUser = DiffSpecialist.USER_MESSAGE
            .replace("{{prompt}}", DiffSpecialist.USER_MESSAGE_COMMENT.formatted(""))
            .replace("{{diff}}", DIFF)
            .replace("{{description}}", "");

        pair.suggestCommitMessages(DIFF, null);

        ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        expectedUser = DiffSpecialist.USER_MESSAGE
            .replace("{{prompt}}", DiffSpecialist.USER_MESSAGE_COMMENT.formatted(DESCRIPTION))
            .replace("{{diff}}", "")
            .replace("{{description}}", DESCRIPTION);

        pair.suggestCommitMessages(null, DESCRIPTION);

        request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );
    }

    @Test
    public void reviewChanges_AI_provided_response() {
        final String DIFF = "use mock 'changes review.txt'";
        final String expectedSystem = DiffSpecialist.SYSTEM_MESSAGE
            .replace("{{format}}", DiffSpecialist.OUTPUT_REVIEW);

        for (CodeReviewLevel l: CodeReviewLevel.values()) {
            final String expectedUser = DiffSpecialist.USER_MESSAGE
                .replace("{{prompt}}", l.prompt())
                .replace("{{diff}}", "use mock 'changes review.txt'")
                .replace("{{description}}", DESCRIPTION);

            final String result = pair.reviewChanges(DIFF, l.level, DESCRIPTION);

            final ChatModelRequestContext request = listener.lastRequestContext.get();
            thenMessagesMatch(
                request.chatRequest().messages(), expectedSystem, expectedUser
            );


            then(result).startsWith("- file: src/com/example/MyService.java");
        }
    }

}

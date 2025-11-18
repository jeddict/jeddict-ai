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
import static io.github.jeddict.ai.agent.pair.PairProgrammerTestBase.TEXT;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class RestSpecialistTest extends PairProgrammerTestBase {

    @Test
    public void pair_is_a_PairProgrammer() {
        final RestSpecialist pair = AgenticServices.agentBuilder(RestSpecialist.class)
            .chatModel(model)
            .build();
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void generateEndpointForClass_returns_AI_provided_response_with_and_without_rules() {
        final RestSpecialist pair = AgenticServices.agentBuilder(RestSpecialist.class)
            .chatModel(model)
            .build();

        endpointForClass_returns_AI_provided_response(TEXT, "no rules", "no rules", pair::generateEndpointForClass);
        endpointForClass_returns_AI_provided_response(TEXT, "\n- global rule 1", "no rules", pair::generateEndpointForClass);
        endpointForClass_returns_AI_provided_response(TEXT, "no rules", "\n- project rule 1", pair::generateEndpointForClass);
        endpointForClass_returns_AI_provided_response(TEXT, "\n- global rule 1", "\n- project rule 1", pair::generateEndpointForClass);
    }

    // --------------------------------------------------------- private methods

    @FunctionalInterface
    private interface RestGenerator {
        String apply(String code, String globalRules, String projectRules);
    }

    private void endpointForClass_returns_AI_provided_response(
        final String code,
        final String globalRules,
        final String projectRules,
        final RestGenerator generator
    ) {
        //
        // invoke the agent
        //
        generator.apply(code, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
                request.chatRequest().messages(),
                RestSpecialist.SYSTEM_MESSAGE
                        .replace("{{globalRules}}", (globalRules.trim().isEmpty()) ? "no rules" : globalRules)
                        .replace("{{projectRules}}", (projectRules.trim().isEmpty()) ? "no rules" : projectRules),
                RestSpecialist.USER_MESSAGE
                        .replace("{{code}}", code)
        );
    }


}

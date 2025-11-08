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
import static io.github.jeddict.ai.agent.pair.JavadocSpecialist.ELEMENT_CLASS;
import static io.github.jeddict.ai.agent.pair.JavadocSpecialist.ELEMENT_MEMBER;
import static io.github.jeddict.ai.agent.pair.JavadocSpecialist.ELEMENT_METHOD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class JavadocSpecialistTest extends PairProgrammerTestBase {

    final String JAVADOC = "this is a javadoc comment";
    private JavadocSpecialist pair;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(JavadocSpecialist.class)
            .chatModel(model)
            .build();

    }

    @Test
    public void generateJavadoc_returns_AI_provided_response_with_and_without_rules() {
        // for class
        generateJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, "no rules", "no rules", pair::generateClassJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, "\n- global rule 1", "no rules", pair::generateClassJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, "no rules", "\n- project rule 1", pair::generateClassJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, "\n- global rule 1", "\n- project rule 1", pair::generateClassJavadoc);

        // for method
        generateJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, "no rules", "no rules", pair::generateMethodJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, "\n- global rule 1", "no rules", pair::generateMethodJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, "no rules", "\n- project rule 1", pair::generateMethodJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, "\n- global rule 1", "\n- project rule 1", pair::generateMethodJavadoc);

        // for member
        generateJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, "no rules", "no rules", pair::generateMemberJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, "\n- global rule 1", "no rules", pair::generateMemberJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, "no rules", "\n- project rule 1", pair::generateMemberJavadoc);
        generateJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, "\n- global rule 1", "\n- project rule 1", pair::generateMemberJavadoc);
    }

    @Test
    public void enhanceClassJavadoc_returns_AI_provided_response_with_and_without_rules() {
        // for class
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, JAVADOC, "no rules", "no rules", pair::enhanceClassJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, JAVADOC, "\n- global rule 1", "no rules", pair::enhanceClassJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, JAVADOC, "no rules", "\n- project rule 1", pair::enhanceClassJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_CLASS, TEXT, JAVADOC, "\n- global rule 1", "\n- project rule 1", pair::enhanceClassJavadoc);

        // for method
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, JAVADOC, "no rules", "no rules", pair::enhanceMethodJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, JAVADOC, "\n- global rule 1", "no rules", pair::enhanceMethodJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, JAVADOC, "no rules", "\n- project rule 1", pair::enhanceMethodJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_METHOD, TEXT, JAVADOC, "\n- global rule 1", "\n- project rule 1", pair::enhanceMethodJavadoc);

        // for member
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, JAVADOC, "no rules", "no rules", pair::enhanceMemberJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, JAVADOC, "\n- global rule 1", "no rules", pair::enhanceMemberJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, JAVADOC, "no rules", "\n- project rule 1", pair::enhanceMemberJavadoc);
        enhanceJavadoc_returns_AI_provided_response(ELEMENT_MEMBER, TEXT, JAVADOC, "\n- global rule 1", "\n- project rule 1", pair::enhanceMemberJavadoc);
    }

    // --------------------------------------------------------- private methods

    @FunctionalInterface
    private interface JavadocGenerator {
        String apply(String code, String globalRules, String projectRules);
    }

    @FunctionalInterface
    private interface JavadocEnhancer {
        String apply(String code, String javadoc, String globalRules, String projectRules);
    }

    private void generateJavadoc_returns_AI_provided_response(
        final String element,
        final String code,
        final String globalRules,
        final String projectRules,
        final JavadocGenerator generator
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
                JavadocSpecialist.SYSTEM_MESSAGE
                        .replace("{{globalRules}}", (globalRules.trim().isEmpty()) ? "no rules" : globalRules)
                        .replace("{{projectRules}}", (projectRules.trim().isEmpty()) ? "no rules" : projectRules),
                JavadocSpecialist.USER_MESSAGE
                        .replace("{{element}}", element)
                        .replace("{{code}}", code)
                        .replace("{{javadoc}}", "")
        );
    }

    private void enhanceJavadoc_returns_AI_provided_response(
        final String element,
        final String code,
        final String javadoc,
        final String globalRules,
        final String projectRules,
        final JavadocEnhancer generator
    ) {
        //
        // the model has been invoked and its answer returned
        //
        //
        // invoke the agent
        //
        generator.apply(code, javadoc, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(),
            JavadocSpecialist.SYSTEM_MESSAGE
                .replace("{{globalRules}}", (globalRules.trim().isEmpty()) ? "no rules" : globalRules)
                .replace("{{projectRules}}", (projectRules.trim().isEmpty()) ? "no rules" : projectRules),
            JavadocSpecialist.USER_MESSAGE
                .replace("{{element}}", element)
                .replace("{{code}}", code)
                .replace("{{javadoc}}", javadoc)
        );
    }

}

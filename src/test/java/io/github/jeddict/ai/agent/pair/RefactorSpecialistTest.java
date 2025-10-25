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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class RefactorSpecialistTest extends PairProgrammerTestBase {

    private RefactorSpecialist pair;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(RefactorSpecialist.class)
            .chatModel(model)
            .build();

    }

    @Test
    public void updateMethodFromDevQuery_returns_AI_provided_response_with_and_without_rules() {
        final String USER_PROMPT = "This is the user request...";

        updateMethodFromDevQuery_returns_AI_provided_response(USER_PROMPT, "the class", "the method", "no rules", "no rules", pair::updateMethodFromDevQuery);
        updateMethodFromDevQuery_returns_AI_provided_response(USER_PROMPT, "the class", "the method", "\n- global rule 1", "no rules", pair::updateMethodFromDevQuery);
        updateMethodFromDevQuery_returns_AI_provided_response(USER_PROMPT, "the class", "the method", "no rules", "\n- project rule 1", pair::updateMethodFromDevQuery);
        updateMethodFromDevQuery_returns_AI_provided_response(USER_PROMPT, "the class", "the method", "\n- global rule 1", "\n- project rule 1", pair::updateMethodFromDevQuery);
    }

    @Test
    public void enhanceMethodFromMethodContent_returns_AI_provided_response_with_and_without_rules() {
        enhanceMethodFromMethodContent_returns_AI_provided_response("the class", "the method", "no rules", "no rules", pair::enhanceMethodFromMethodContent);
        enhanceMethodFromMethodContent_returns_AI_provided_response("the class", "the method", "\n- global rule 1", "no rules", pair::enhanceMethodFromMethodContent);
        enhanceMethodFromMethodContent_returns_AI_provided_response("the class", "the method", "no rules", "\n- project rule 1", pair::enhanceMethodFromMethodContent);
        enhanceMethodFromMethodContent_returns_AI_provided_response("the class", "the method", "\n- global rule 1", "\n- project rule 1", pair::enhanceMethodFromMethodContent);
    }

    @Test
    public void fixMethodCompilationError_returns_AI_provided_response_with_and_without_rules() {
        fixMethodCompilationError_returns_AI_provided_response("the class", "the method", "no rules", "no rules", pair::fixMethodCompilationError);
        fixMethodCompilationError_returns_AI_provided_response("the class", "the method", "\n- global rule 1", "no rules", pair::fixMethodCompilationError);
        fixMethodCompilationError_returns_AI_provided_response("the class", "the method", "no rules", "\n- project rule 1", pair::fixMethodCompilationError);
        fixMethodCompilationError_returns_AI_provided_response("the class", "the method", "\n- global rule 1", "\n- project rule 1", pair::fixMethodCompilationError);
    }

    @Test
    public void fixVariableError_returns_AI_provided_response_with_and_without_rules() {
        fixVariableError_returns_AI_provided_response("the source", "the error", "no rules", "no rules", pair::fixVariableError);
        fixVariableError_returns_AI_provided_response("the source", "the error", "\n- global rule 1", "no rules", pair::fixVariableError);
        fixVariableError_returns_AI_provided_response("the source", "the error", "no rules", "\n- project rule 1", pair::fixVariableError);
        fixVariableError_returns_AI_provided_response("the source", "the error", "\n- global rule 1", "\n- project rule 1", pair::fixVariableError);
    }

    @Test
    public void enhanceVariableName_returns_AI_provided_response_with_and_without_rules() {
        enhanceVariableName_returns_AI_provided_response("currentVariableName", "the method content", "the class content", "no rules", "no rules", pair::enhanceVariableName);
        enhanceVariableName_returns_AI_provided_response("currentVariableName", "the method content", "the class content", "\n- global rule 1", "no rules", pair::enhanceVariableName);
        enhanceVariableName_returns_AI_provided_response("currentVariableName", "the method content", "the class content", "no rules", "\n- project rule 1", pair::enhanceVariableName);
        enhanceVariableName_returns_AI_provided_response("currentVariableName", "the method content", "the class content", "\n- global rule 1", "\n- project rule 1", pair::enhanceVariableName);
    }

    // --------------------------------------------------------- private methods


    @FunctionalInterface
    protected static interface CodeGenerator3 {
        String apply(String arg1, String arg2, String arg3, String globalRules, String projectRules);
    }
    @FunctionalInterface
    protected static interface CodeGenerator2 {
        String apply(String arg1, String arg2, String globalRules, String projectRules);
    }

    protected void updateMethodFromDevQuery_returns_AI_provided_response(
        final String prompt,
        final String code,
        final String method,
        final String globalRules,
        final String projectRules,
        final CodeGenerator3 generator
    ) {
        //
        // invoke the agent
        //
        generator.apply(prompt, code, method, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        updateMethod_returns_AI_provided_response(prompt, code, method, globalRules, projectRules);
    }

    protected void fixMethodCompilationError_returns_AI_provided_response(
        final String code,
        final String method,
        final String globalRules,
        final String projectRules,
        final CodeGenerator3 generator
    ) {
        final String ERROR = "the error";

        //
        // invoke the agent
        //
        generator.apply(ERROR, code, method, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        updateMethod_returns_AI_provided_response(
            String.format(RefactorSpecialist.FIX_COMPILATION_ERROR, ERROR),
            code, method, globalRules, projectRules
        );
    }

    protected void fixVariableError_returns_AI_provided_response(
        final String source,
        final String error,
        final String globalRules,
        final String projectRules,
        final CodeGenerator2 generator
    ) {
        //
        // invoke the agent
        //
        generator.apply(source, error, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        updateMethod_returns_AI_provided_response(
            String.format(RefactorSpecialist.FIX_COMPILATION_ERROR, error),
            source, "", globalRules, projectRules
        );
    }

    protected void enhanceMethodFromMethodContent_returns_AI_provided_response(
        final String code,
        final String method,
        final String globalRules,
        final String projectRules,
        final CodeGenerator2 generator
    ) {
        //
        // invoke the agent
        //
        generator.apply(code, method, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        updateMethod_returns_AI_provided_response(RefactorSpecialist.PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE, code, method, globalRules, projectRules);
    }

    protected void enhanceVariableName_returns_AI_provided_response(
        final String variableContext,
        final String methodContent,
        final String classContent,
        final String globalRules,
        final String projectRules,
        final CodeGenerator3 generator
    ) {
        //
        // invoke the agent
        //
        generator.apply(variableContext, methodContent, classContent, globalRules, projectRules);

        //
        // proper prompt messages has been generated and provided
        //
        updateMethod_returns_AI_provided_response(
            String.format(RefactorSpecialist.PROMPT_ENHANCE_VARIABLE_NAME, variableContext),
            classContent, methodContent, globalRules, projectRules
        );
    }

    protected void updateMethod_returns_AI_provided_response(
        final String msg,
        final String code,
        final String method,
        final String globalRules,
        final String projectRules
    ) {
        final ChatModelRequestContext request = listener.lastRequestContext.get();
        final String expectedSystem =
            RefactorSpecialist.SYSTEM_MESSAGE
                .replace("{{globalRules}}", (globalRules.trim().isEmpty()) ? "no rules" : globalRules)
                .replace("{{projectRules}}", (projectRules.trim().isEmpty()) ? "no rules" : projectRules);
        String expectedUser = (msg != null)
                            ?  RefactorSpecialist.USER_MESSAGE.replace("{{userRequest}}", msg)
                            : RefactorSpecialist.USER_MESSAGE;
        expectedUser = expectedUser.replace("{{code}}", code).replace("{{methodCode}}", method);
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );
    }

}

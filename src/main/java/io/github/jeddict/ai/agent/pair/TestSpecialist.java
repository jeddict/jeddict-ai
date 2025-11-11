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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import io.github.jeddict.ai.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;


/**
 * Interface defining a provider for generating JAX-RS REST endpoints from Java class source code.
 * This interface is intended to be implemented by specialized components that analyze a class and
 * produce syntactically correct, import-inclusive JAX-RS endpoint method declarations with
 * basic method implementations.
 *
 * <p>The generated output is a well-formatted JSON object containing an array of required imports
 * and the annotated JAX-RS method declarations as a text string.</p>
 */
public interface TestSpecialist extends PairProgrammer {
        public static final String SYSTEM_MESSAGE = """
You are an experienced programmer specialized writing unit tests based on the
provided project code, for the provided class and/or method code and accordingly
to the rules below.
Rules:
{{rules}}
- Test cases must be well-structured and functional.
- Use the following testing frameworks: {{frameworks}}
""";
    public static final String USER_MESSAGE = """
{{prompt}}
{{query}}
--
The project classes are:
{{project}}
--
The class to test is:
{{class}}
--
The method to test is:
{{method}}
""";
    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate Unit Test for the given class or method")
    String generateUnitTest(
        @V("query") final String query,
        @V("project") final String allClasses,
        @V("class") final String classCode,
        @V("method") final String methodCode,
        @V("prompt") final String prompt,
        @V("frameworks") final List<String> framework,
        @V("rules") final String sessionRules
    );

    default String generateTestCase(
        final String query,
        final String allClasses,
        final String classCode,
        final String methodCode,
        final String prompt,
        final String sessionRules,
        final List<Response> history
    ) {
        LOG.finest(() -> "\nquery: %s\nproject: %s\nclass: %s\nmethod: %s\nsessionRules: %s\nprompt: %s\n%s history".formatted(
            StringUtils.abbreviate(query, 80),
            StringUtils.abbreviate(allClasses, 80),
            StringUtils.abbreviate(classCode, 80),
            StringUtils.abbreviate(methodCode, 80),
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(sessionRules, 80),
            (((history != null) && !history.isEmpty()) ? "with" : "without")
        ));

        List<String> testCaseTypes = new ArrayList<>(); // preserve the order
        if (query != null) {
            final String lowerQuery = query.toLowerCase();

            //
            // If we have a valid query, let's check if any testing framework
            // has been mentioned to reinforce the prompt
            //

            //
            // If we have a valid query, let's check if any testing framework
            // has been mentioned to reinforce the prompt
            //
            //
            if (lowerQuery.contains("junit5")) {
                testCaseTypes.add("JUnit5");
            } else if (lowerQuery.contains("junit")) {
                testCaseTypes.add("JUnit");
            }

            if (lowerQuery.contains("testng")) {
                testCaseTypes.add("TestNG");
            }

            if (lowerQuery.contains("mockito")) {
                testCaseTypes.add("Mockito");
            }

            if (lowerQuery.contains("spock")) {
                testCaseTypes.add("Spock");
            }

            if (lowerQuery.contains("assertj")) {
                testCaseTypes.add("AssertJ");
            }

            if (lowerQuery.contains("hamcrest")) {
                testCaseTypes.add("Hamcrest");
            }

            if (lowerQuery.contains("powermock")) {
                testCaseTypes.add("PowerMock");
            }

            if (lowerQuery.contains("cucumber")) {
                testCaseTypes.add("Cucumber");
            }

            if (lowerQuery.contains("spring test")) {
                testCaseTypes.add("Spring Test");
            }

            if (lowerQuery.contains("arquillian")) {
                testCaseTypes.add("Arquillian Test");
            }
        }

        return generateUnitTest(
            StringUtils.defaultString(query),
            StringUtils.defaultString(allClasses),
            StringUtils.defaultString(classCode),
            StringUtils.defaultString(methodCode),
            prompt, testCaseTypes, sessionRules
        );
    }

    default String generateTestCase(
        final String query,
        final String allClasses,
        final String classCode,
        final String methodCode,
        final String prompt,
        final String sessionRules
    ) {
        return generateTestCase(
            query, allClasses, classCode, methodCode, prompt, sessionRules, List.of()
        );
    }
}

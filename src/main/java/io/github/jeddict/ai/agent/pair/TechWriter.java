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
import io.github.jeddict.ai.util.AgentUtil;


/**
 * The JavadocSpecialist interface provides a structured approach to generating
 * and enhancing Javadoc comments for Java elements such as classes, methods, and members.
 *
 *
 * Core functionality includes:
 * <ul>
 *   <li>Generating new Javadoc comments for classes, methods, and members</li>
 *   <li>Enhancing existing Javadoc comments while preserving original content</li>
 *   <li>Context-aware processing that incorporates both global coding standards and project-specific documentation rules</li>
 *   <li>Rule normalization through {@link AgentUtil} to ensure consistent processing</li>
 * </ul>
 *
 * Implementation notes:
 * <ul>
 *   <li>All methods return Javadoc content wrapped in Javadoc comment boundaries</li>
 *   <li>Empty strings are used when generating new Javadoc (enhancement methods expect existing content)</li>
 * </ul>
 *
 * Typical usage pattern:
 * <pre>
 *   JavadocSpecialist programmer = AgenticServices.agentBuilder(PairProgrammer.Specialist.JAVADOC)
 *                              ...
 *                              .build();
 *   String text = programmer.generate[Class/Method/Member]Javadoc(classCode, globalRules, projectRules);
 *   String text = programmer.enhance[Class/Method/Member]Javadoc(methodCode, existingJavadoc, globalRules, projectRules);
 * </pre>
 *
 */
public interface TechWriter extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
You are an expert technical writer that, based on user request can:
- write Javadoc comments for the provided code
- describe existing code, either classes, methods or snippets
When requested to write javadoc, the folloing rules apply:
- generate completely new Javadoc or enahance the existing Javadoc based on user request
- generate the Javadoc wrapped with in /** ${javadoc} **/
- generate javadoc only for the element (class, methods or members) requested by the user
- do not provide any additional text or explanation
When Requested to describe code, the following rules apply:
- write an explenation of the code without adding javadoc
Take into account the general rules: {{globalRules}}
Take into account the project rules: {{projectRules}}
Take into account the session rules: {{sessionRules}}
""";
    public static final String USER_MESSAGE = """
{{prompt}}
The code is: {{code}}
The Javadoc is: {{javadoc}}
""";

    public static final String USER_MESSAGE_JAVADOC = "Provide javadoc for the %s";
    public static final String USER_MESSAGE_DESCRIBE = "Describe the following code";

    public static final String ELEMENT_CLASS = "class";
    public static final String ELEMENT_METHOD = "method";
    public static final String ELEMENT_MEMBER = "member";


    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate or enhance javadoc comments or describe existing code")
    String writing(
        @V("prompt") final String prompt,
        @V("code") final String code,
        @V("javadoc") final String javadoc,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules,
        @V("sessionRules") final String sessionRules
    );

    default String generateClassJavadoc(final String code, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_CLASS), code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String generateMethodJavadoc(final String code, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_METHOD), code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String generateMemberJavadoc(final String code, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_MEMBER), code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String enhanceClassJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_CLASS), code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String enhanceMethodJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_METHOD), code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String enhanceMemberJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return writing(
            USER_MESSAGE_JAVADOC.formatted(ELEMENT_MEMBER), code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules),
            "no rules"
        );
    }

    default String describeCode(final String code, final String sessionRules) {
        return writing(
            USER_MESSAGE_DESCRIBE, code, "", "no rules", "no rules", AgentUtil.normalizeRules(sessionRules)
        );
    }

}

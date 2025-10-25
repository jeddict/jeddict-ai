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
public interface JavadocSpecialist extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
You are a programmer that writes only with Javadoc comments for the provided code accordingly to the rules:
- Generate completely new Javadoc or enahance the existing Javadoc based on user request
- Generate the Javadoc wrapped with in /** ${javadoc} **/
- Generate javadoc only for the element (class, methods or members) requested by the user
- Do not provide any additional text or explanation"
Take into account the following general rules: {{globalRules}}
Take into account the following project rules: {{projectRules}}
""";
    public static final String USER_MESSAGE = """
Provide javadoc for the {{element}}
The code is: {{code}}
The Javadoc is: {{javadoc}}
""";

    public static final String ELEMENT_CLASS = "class";
    public static final String ELEMENT_METHOD = "method";
    public static final String ELEMENT_MEMBER = "member";


    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate or enhance javadoccomments based on the class or class member code")
    String javadoc(
        @V("element") final String element,
        @V("code") final String sourece,
        @V("javadoc") final String javadoc,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    default String generateClassJavadoc(final String code, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_CLASS, code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }

    default String generateMethodJavadoc(final String code, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_METHOD, code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }

    default String generateMemberJavadoc(final String code, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_MEMBER, code, "",
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }

    default String enhanceClassJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_CLASS, code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }

    default String enhanceMethodJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_METHOD, code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }
    default String enhanceMemberJavadoc(final String code, final String javadocContent, final String globalRules, final String projectRules) {
        return javadoc(
            ELEMENT_MEMBER, code, javadocContent,
            AgentUtil.normalizeRules(globalRules),
            AgentUtil.normalizeRules(projectRules)
        );
    }

}

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



/**
 * Provides an interface for AI-powered Java code refactoring utilities. This
 * specialist adapts or enhances methods, variable names, and fixes compilation
 * errors based on specific user requests and project rules.<p>
 *
 * The interface defines prompts, message constants, and processing methods to
 * allow Java code modification and generation workflows. Returned results are
 * formatted as JSON objects containing necessary imports and code content.<p>
 *
 * Responsibilities include:
 * <ul>
 *   <li>Generating new method code or enhancing existing methods</li>
 *   <li>Fixing compilation errors in method implementations</li>
 *   <li>Refining variable names for improved clarity</li>
 *   <li>Integrating user, global, and project-specific requirements</li>
 * </ul>
 *
 * All enhancement and refactoring logic is ultimately handled by
 * {@link #updateMethodFromDevQuery(String, String, String, String, String, String)},
 * which takes custom prompt, source, and context arguments.<p>
 *
 * The interface also provides default convenience methods to streamline common
 * refactoring and code enhancement tasks.<p>
 *
 */
public interface RefactorSpecialist extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
You are a programmer specialized in writing Java code. Base on user request, you will:
- Write new or enhancing existing code
- Incorporate any specific details or requirements mentioned by the user.
- Include all necessary imports relevant to the enhanced or newly created method.
- Return only the Java code and its necessary imports, without including any class declarations, constructors, or other boilerplate code.
- Not include any description or explanation beside the code
- Format the output as a JSON object with two fields: 'imports' as array (list of necessary imports) and 'content' as text.
  Example output:
  {
    "imports": [
      "io.github.jeddict.ai.agent.pair.RestSpecialits",
      "io.github.jeddict.ai.agent.pair.CodeSpecialits",
      "io.github.jeddict.ai.ReturnType"
    ],
    "content": "public ReturnType method() {\\n // implementation \\n}"
  }
- Ensure the generated methods are unique and not duplicates of existing methods in the class content.
Take into account the following general rules: {{globalRules}}
Take into account the following project rules: {{projectRules}}
""";
    public static final String USER_MESSAGE = """
{{userRequest}}
The class is: {{code}}
The current method code is: {{methodCode}}
""";
    public static final String PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE =
        "Given the following Java class content and Java method content, modify and enhance the method accordingly.";
    public static final String PROMPT_ENHANCE_VARIABLE_NAME =
        "given the current variable name \n%s\n, provide a better name for the variable; make sure to return just the variable name only";
    public static final String FIX_COMPILATION_ERROR =
        "Given the compilation error %s and the following Java class and method content, fix the method so it compiles properly";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate or enhance a method based on the class code and method code")
    String updateMethodFromDevQuery(
        @V("userRequest") final String userRequest,
        @V("code") final String source,
        @V("methodCode") final String methodSource,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    /**
     * Enhances the content of a Java method by applying both global and project-specific rules.
     *
     * This default method receives the source code representing the entire context,
     * the source of the target method, and two sets of rule definitions: global
     * and project-scoped. It delegates enhancement logic to {@code updateMethodFromDevQuery}
     * using the defined prompt {@code PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE}.</
     *
     * @param source       the source code containing the overall context in which the method resides
     * @param methodSource the source code of the specific method to enhance
     * @param globalRules  the global rules to apply during enhancement
     * @param projectRules the project-specific rules to apply during enhancement
     *
     * @return the enhanced method source code as a {@code String}
     */
    default String enhanceMethodFromMethodContent(
        final String source, final String methodSource, final String globalRules, final String projectRules
    ) {
        return updateMethodFromDevQuery(
            PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE, source, methodSource, globalRules, projectRules
        );
    }

    /**
     * Fixes a compilation error in the provided method source using contextual code and rules.
     *
     * This method attempts to resolve the specified compilation error by updating
     * the method source code. It utilizes the original source, method source,
     * global rules, and project-specific rules to generate a fixed version of the method.
     * It delegates the fix to {@link #updateMethodFromDevQuery(String, String,
     * String, String, String)} using a formatted query parameter.
     *
     * @param error the description or message of the compilation error encountered
     * @param source the complete source code context in which the method resides (the class plus all referenced classes)
     * @param methodSource the source code of the affected method
     * @param globalRules general rules to be applied for fixing method errors
     * @param projectRules project-specific rules to be applied for fixing method errors
     *
     * @return a {@code String} containing the updated method source with the compilation error addressed
     *
     * @implSpec
     **/
    default String fixMethodCompilationError(
        final String error, final String source, final String methodSource, final String globalRules, final String projectRules
    ) {
        return updateMethodFromDevQuery(
            String.format(FIX_COMPILATION_ERROR, error), source, methodSource, globalRules, projectRules
        );
    }

    /**
     * Attempts to fix a compilation error related to variables in the given source code
     * using the provided global and project rules.
     *
     * This method delegates to {@code updateMethodFromDevQuery} with a specific error
     * formatting, aiming to automatically apply the necessary fix for the described error.
     *
     * @param source      the original source code where the error was detected
     * @param error       a description or message of the variable-related compilation error
     * @param globalRules a set of global rules to guide the fixing logic
     * @param projectRules a set of project-specific rules to further guide the fixing logic
     *
     * @return a new source code string with the variable error fixed according to the rules,
     *         or the original if no applicable fix is found
     */
    default String fixVariableError(
        final String source, final String error, final String globalRules, final String projectRules
    ) {
        return updateMethodFromDevQuery(
            String.format(FIX_COMPILATION_ERROR, error), source, "", globalRules, projectRules
        );
    }

    /**
     * Enhances the given variable name based on the specified method content, class content,
     * global rules, and project rules. This method formats a prompt for variable name
     * enhancement and delegates the operation to {@code updateMethodFromDevQuery}.
     *
     * @param currentName    the current name of the variable to be enhanced
     * @param methodContent  the content of the method where the variable is used
     * @param classContent   the content of the class containing the variable
     * @param globalRules    global naming and formatting rules to consider
     * @param projectRules   project-specific naming and formatting rules to consider
     * @return               the enhanced variable name as determined by the prompt and rules
     */
    default String enhanceVariableName(
        final String currentName, final String methodContent, final String classContent, final String globalRules, final String projectRules
    ) {
        return updateMethodFromDevQuery(
            String.format(PROMPT_ENHANCE_VARIABLE_NAME, currentName),
            classContent, methodContent, globalRules, projectRules
        );
    }
}

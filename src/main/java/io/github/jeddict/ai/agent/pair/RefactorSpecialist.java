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
import io.github.jeddict.ai.util.JSONUtil;

/**
 * The {@code RefactorSpecialist} interface extends the functionality of a {@code PairProgrammer} to specialize in refactoring Java code based on detailed user requests. This interface provides methods to execute various refactoring tasks such as modifying code, fixing compilation errors, and enhancing expressions or variable names. It includes a series of prompts and system configuration constants that guide the refactoring processes.
 * <p>
 * All methods in this interface ultimately rely on {@link #refactor(String, String, String, String, String)} to process different types of refactoring tasks by feeding in specific prompts and required code snippets. This makes the refactoring process flexible and tailored to specific coding contexts provided by user input.
 * </p>
 * <h3>Usage Examples:</h3>
 * <ul>
 * <li>Enhancing the clarity or efficiency of a method given its content.</li>
 * <li>Fixing compilation errors in methods with detailed error messages.</li>
 * <li>Improving variable naming to better reflect its purpose and usage.</li>
 * </ul>
 *
 * @see PairProgrammer
 */
/**
 * Interface defining the operations of a specialized Java programmer for refactoring code.
 * It extends the capability of a PairProgrammer with additional responsibilities and specialized methods
 * to enhance or fix Java code snippets based on different prompts or guidelines provided to the method.
 * It also encapsulates rules and specifications for altering Java coding through several utility methods
 * that each handle specific types of code enhancement and error fixing operations.
 *
 * Additionally, it includes constant string messages to drive the behavior of methods providing system responses,
 * user instructions, or fixed prompts for code enhancements.
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
- Ensure any generated method is unique and does not duplicates of existing methods in the class.
Take into account the following general rules: {{globalRules}}
Take into account the following project rules: {{projectRules}}
""";
    public static final String USER_MESSAGE = """
{{message}}
The class is: {{code}}
The snippet: {{snippet}}
""";
    public static final String PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE =
        "Given the following Java class content and Java method content, modify and enhance the method accordingly.";
    public static final String PROMPT_ENHANCE_VARIABLE_NAME =
        "given the current variable name \n%s\n, provide a better name for the variable; make sure to return just the variable name only";
    public static final String PROMPT_FIX_COMPILATION_ERROR =
        "Given the compilation error %s and the following Java class and method content, fix the method so it compiles properly";
    public static final String PROMPT_ENHANCE_EXPRESSION =
        "Given the parent context\n'%s'\nenhance the expression provided in the snippet to be more efficient, clearer or following best practices.";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate or enhance source code based on the class code and the snippet to work on")
    String refactor(
        @V("message") final String message,            // prompt details
        @V("code") final String source,                // the source code (e.g. the entire class)
        @V("snippet") final String snippet,            // the portion of the code to work on
        @V("globalRules") final String globalRules,    // configured global rules
        @V("projectRules") final String projectRules   // configured project rules
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
        return refactor(
            PROMPT_ENHANCE_METHOD_FROM_METHOD_CODE, source, methodSource, globalRules, projectRules
        );
    }

    /**
     * Fixes a compilation error in the provided method source using contextual code and rules.
     *
     * This method attempts to resolve the specified compilation error by updating
     * the method source code. It utilizes the original source, method source,
     * global rules, and project-specific rules to generate a fixed version of the method.
     * It delegates the fix to {@link #refactor(String, String, String, String, String)} using a formatted query parameter.
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
        return refactor(PROMPT_FIX_COMPILATION_ERROR.formatted(error), source, methodSource, globalRules, projectRules
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
        return refactor(PROMPT_FIX_COMPILATION_ERROR.formatted(error), source, "", globalRules, projectRules
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
        return refactor(
            PROMPT_ENHANCE_VARIABLE_NAME.formatted(currentName),
            classContent, methodContent, globalRules, projectRules
        );
    }

    /**
     * Enhances the given expression statement given the the parent content and class content.
     *
     * @param classContent the content of the class in which the expression statement is located
     * @param parentContent the content of the parent element of the expression statement
     * @param expressionContent the content of the expression statement to be enhanced
     * @return the enhanced expression statement as a JSON string
     */
    default String enhanceExpressionStatement(
        final String classContent,
        final String parentContent,
        final String expressionContent
    ) {
        return JSONUtil.extractContent(
            refactor(PROMPT_ENHANCE_EXPRESSION.formatted(parentContent), classContent, expressionContent, "", "")
        );
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import org.apache.commons.lang3.StringUtils;



/**
 * Provides an interface for suggesting improved variable names based on the context of Java code.
 *
 * Implementations of this interface analyze a given line of code, its containing class,
 * and all relevant project classes to generate a list of up to three descriptive names
 * suitable for a variable according to Java naming conventions and best practices.
 *
 * Example usage:
 *   List<String> suggestions = codeAdvisor.suggestVariableNames(line, classSource, allClasses);
 *
 */
public interface CodeAdvisor extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
"You are an expert programmer that can suggest code based on the context of the
program and best practices to write good quality code. Based on user request you will:
- Suggest multiple meaningful and descriptive names for a variable in a given Java class.
- Suggest multiple meaningful and descriptive string literals for the given context in a given Java class.
- Suggest multiple appropriate method invocations for the given context in a given Java class.
- For each suggestion, just provide the text of the suggestion.
- Provide a list with up to 3 suggestions and nothing else as plain lines of text.
""";
    public static final String USER_MESSAGE = """
Based on the below line of code, the class it belowngs to and project classes data,
suggest a list of improved elelement to replace the placeholder ${SUGGESTION} in Java Class.
The element is a: {{element}}
The line of code is: {{line}}
The code is: {{code}}
The project classes are: {{classes}}
Project info: {{project}}
""";

    static final Pattern JAVA_ID_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    //
    // NOTE: given the agent returns a List, langchain4j adds to the prompt:
    // \nYou must put every item on a separate line.
    //
    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Suggest up to 3 names for a variable, method or other elements")
    List<String> suggest(
        @V("element") final String element,    // variable, method, method invocation
        @V("classes") final String classes,    // related classes and method signatures
        @V("code") final String code,          // the class code or other code
        @V("line") final String line,          // the current line of code
        @V("project") final String projectInfo // general project info
    );

    default List<String> cleanNames(final List<String> in) {
        final List<String> out = new ArrayList<>();

        in.forEach((name) -> {
            if (!name.isEmpty()
                    && JAVA_ID_PATTERN.matcher(name).matches()
                    && !SourceVersion.isKeyword(name)) {
                out.add(name);
            }
        });

        return out;
    }

    /**
     * Suggests a list of valid Java method names based on provided source line,
     * code snippet, and relevant class names.
     *
     * This method analyzes the input line, code, and classes to generate
     * candidate method names, filters out empty names, names that do not match
     * Java identifier rules, and reserved Java keywords.
     *
     * @param line the source code line where the method is to be inserted
     * @param code the surrounding source code context used for name suggestion
     * @param classes the related class names or definitions as context
     *
     * @return a list of filtered method names valid in Java, excluding keywords
     */
    default List<String> suggestMethodNames(
        final String classes,
        final String code,
        final String line
    ) {
        log(line, code, classes);
        return cleanNames(suggest("method", classes, code, line, ""));
    }

    /**
     * Suggests a list of valid Java variable names based on provided source line,
     * code snippet, and relevant class names.
     *
     * This method analyzes the input line, code, and classes to generate candidate
     * variable names, filters out empty names, names that do not match Java identifier
     * rules, and reserved Java keywords.
     *
     * @param line the source code line where the variable is to be inserted
     * @param code the surrounding source code context used for name suggestion
     * @param classes the related class names or definitions as context
     *
     * @return a list of filtered variable names valid in Java, excluding keywords
     */
    default List<String> suggestVariableNames(
        final String classes,
        final String code,
        final String line
    ) {
        log(line, code, classes);
        return cleanNames(suggest("variable", classes, code, line, ""));
    }

    /**
     * Suggests string literals based on the provided line of code and context.
     *
     * @param line The line of code for which to suggest string literals.
     * @param code The entire code context in which the line is located.
     * @param classes The classes available in the code context.
     *
     * @return A list of suggested string literals.
     */
    default List<String> suggestStringLiterals (
        final String classes,
        final String code,
        final String line
    ) {
        log(line, code, classes);
        return suggest("string literals", classes, code, line, "");
    }

    /**
     * Suggests method invocations based on the provided line of code and context.
     *
     * @param line The line of code for which to suggest string literals.
     * @param code The entire code context in which the line is located.
     * @param classes The classes available in the code context.
     *
     * @return A list of suggested string literals.
     */
    default List<String> suggestMethodInvocations (
        final String projectInfo,
        final String classes,
        final String code,
        final String line
    ) {
        log(line, code, classes);
        return suggest("method invocation", classes, code, line, projectInfo);
    }


    /**
     * Logs the provided line, code, and classes information at the FINEST level.
     * Note that it returns Void because void methods are not supported in
     * agents by lanchain4j.
     *
     * @param line The line information to be logged.
     * @param code The code information to be logged.
     * @param classes The classes information to be logged.
     *
     * @return Void always returns null.
     */
    default Void log(final String line, final String code, final String classes) {
        LOG.finest(() -> "\n"
            + "line: " + StringUtils.abbreviate(line, 80) + "\n"
            + "code: " + StringUtils.abbreviate(code, 80) + "\n"
            + "classes: " + StringUtils.abbreviate(classes, 80) + "\n"
        );

        return null;
    }
}

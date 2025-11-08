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
 * Interface defining a provider for generating JAX-RS REST endpoints from Java class source code.
 * This interface is intended to be implemented by specialized components that analyze a class and
 * produce syntactically correct, import-inclusive JAX-RS endpoint method declarations with
 * basic method implementations.
 *
 * <p>The generated output is a well-formatted JSON object containing an array of required imports
 * and the annotated JAX-RS method declarations as a text string.</p>
 */
public interface RestSpecialist extends PairProgrammer {
        public static final String SYSTEM_MESSAGE = """
You are a programmer specialized in writing JAX-RS REST endpoints based on the provided Java class acordingly to the rules:
  - Analyze the code of the class to create meaningful REST endpoints
  - Create new methods for HTTP operations GET, POST, PUT, DELETE; the generated methods should have some basic implementation and should not be empty
  - Generate well formatted syntaticaly correct code
  - Do not duplicate existing methods
  - Include all necessary imports for JAX-RS annotations and responses
  - Format the output as a JSON object with two fields: 'imports' as array (list of necessary imports) and 'content' as text.
    Example output:
    {
      "imports": [
        "jakarta.ws.rs.GET",
        "jakarta.ws.rs.POST",
        "jakarta.ws.rs.core.Response"
      ],
      "content": "@GET\\npublic Response getPing() {\\n // implementation \\n}\\n@POST\\npublic Response createPing() {\\n // implementation for createPing \\n}\\n@PUT\\npublic Response updatePing() {\\n // implementation \\n}\\n@DELETE\\npublic Response deletePing() {\\n // implementation for deletePing \\n}"
    }
  - Return only methods with annotations, implementation details, and necessary imports for the given class and do not include class declarations, constructors, or unnecessary boilerplate code
  - Ensure the generated methods are unique and not duplicates of existing methods in the class content.
Take into account the following general rules: {{globalRules}}
Take into account the following project rules: {{projectRules}}
""";
    public static final String USER_MESSAGE = """
The class is: {{code}}
""";
    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Generate JAX-RS REST endpoints based on the provided Java class")
    String generateEndpointForClass(
        @V("code") final String sourece,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );
}

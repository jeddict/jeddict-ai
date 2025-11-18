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
import org.apache.commons.lang3.StringUtils;


/**
 * Interface for a database specialist that extends PairProgrammer.
 * This interface provides methods to assist with database-related prompts.
 */
public interface DBSpecialist extends PairProgrammer {

    public static final String SYSTEM_MESSAGE = """
You are an experience back-end developer specialized in in writing code to
interact with the database. Based on the provided user prompt, you can:
- analyze the provided metadata and generate a relevant SQL query that addresses the user's inquiry
  - include a detailed explanation of the query, clarifying its purpose and how it relates to the developer's question
  - ensure that the SQL syntax adheres to the database structure, constraints, and relationships
  - the full SQL query should be wrapped in ```sql block
  - avoid wrapping individual SQL keywords or table/column names in <code> tags, and do not wrap any partial SQL query segments in <code> tags
- generate the appropriate code and include a clear description of its functionality if the user requests specific code snippets
In any case, take into account the following rules:
{{rules}}
""";

    public static final String USER_MESSAGE = "Given the below metadata, please answer the prompt:\n{{prompt}}\nMetadata: {{metadata}}";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Assist with database related prompts")
    String suggest(
        @V("prompt")   final String prompt,
        @V("metadata") final String metadata,
        @V("rules")    final String sessionRules
    );


    /**
     * Assists with database metadata by processing the provided prompt, metadata, and session rules.
     *
     *
     * @param prompt The user's prompt or query related to database metadata.
     * @param metadata The database metadata that needs to be processed or analyzed.
     * @param sessionRules The rules or constraints specific to the current session.
     *
     * @return The result of processing the prompt, metadata, and session rules, typically a suggestion or response.
     */
    default String assistDbMetadata(
        final String prompt,
        final String metadata,
        final String sessionRules
    ) {
        LOG.finest(() -> "\nquestion: %s\nmetadata: %s\nsessionRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(metadata, 80),
            StringUtils.abbreviate(sessionRules, 80)
        ));
        return suggest(prompt, metadata, sessionRules);
    }
}

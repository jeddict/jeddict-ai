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
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;


public interface HackerWithoutTools extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
    You are an expert developer that can address complex tasks by resolving problems,
    proposing solutions, writing and correcting code.
    """
    ;
    @Agent("Performs the most complex hacking tasks without using tools")
    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage("{{prompt}}")
    String hack(
        @V("prompt") final String prompt
    );
}

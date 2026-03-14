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
package io.github.jeddict.ai.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.exception.ToolExecutionException;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import java.io.IOException;

/**
 *
 */
public class UtilTools extends AbstractTool {

    public UtilTools() throws IOException {
        super(".");
    }

    /**
     * Echo a given message returning it as is
     *
     * @param message the message to echo
     * @return the same message
     */
    @Tool("Echo the given message back as return value")
    @ToolPolicy(READONLY)
    public String echo(
        @P("message to echo")
        final String message
    ) throws ToolExecutionException {
        return (message != null) ? message : "null";
    }

}

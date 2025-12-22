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

import dev.langchain4j.agent.tool.Tool;
import java.util.UUID;

/**
 *
 */
public class ToolsProbingTool {
    //
    // Create a random string to be used to check if the tool is properly
    // executed and its value properly handled.
    //
    public final String probeText = UUID.randomUUID().toString();

    @Tool("""
        Tool to probe if the model supports tools/functions.
        This tool does not accept any arguments and returns a random value
    """)
    public String probeToolsSupport() {
        return probeText;
    }
}
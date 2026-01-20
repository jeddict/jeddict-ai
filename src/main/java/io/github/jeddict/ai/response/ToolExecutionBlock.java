/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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
package io.github.jeddict.ai.response;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

/**
 *
 */
public class ToolExecutionBlock extends TextBlock {

    public static final String BLOCK_TYPE = "tooling";

    public final ToolExecutionRequest execution;

    public ToolExecutionBlock(final ToolExecutionRequest execution, final String result) {
        super(BLOCK_TYPE, result);
        this.execution = execution;
    }

    @Override
    public String toString() {
        return "Type: " + type + "\nExecution:\n" + execution + "\n";
    }
}

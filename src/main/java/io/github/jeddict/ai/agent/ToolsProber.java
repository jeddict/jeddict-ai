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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.tool.ToolExecution;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;


public interface ToolsProber {

    final Logger LOG = Logger.getLogger(ToolsProber.class.getCanonicalName());

    public static final String SYSTEM_MESSAGE = """
        You are an assistant to probe if a model supports tools.
    """;
    public static final String USER_MESSAGE = """
        Execute the tool probeToolsSupport and return the result of the execution.
        If you do not know probeToolsSupport return "unsupported".
    """;

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Probe if the model supports tools")
    Result<String> probe();

    default boolean probe(final String probeText) {
        if (StringUtils.isBlank(probeText)) {
            throw new IllegalArgumentException("probeText can not be null or blank");
        }
        try {
            final Result result = probe();
            final List<ToolExecution> toolExecutions = result.toolExecutions();
            if (toolExecutions.isEmpty()) {
                return false;
            }
            final ToolExecution toolExecution = toolExecutions.get(0);
            return probeText.equals(toolExecution.result());
        } catch (Throwable t) {
            LOG.finest(() -> t.getMessage());
            return false;
        }
    }
}

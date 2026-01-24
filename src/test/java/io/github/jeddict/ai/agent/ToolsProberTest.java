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

import io.github.jeddict.ai.agent.pair.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.request.ToolChoice;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ToolsProberTest extends PairProgrammerTestBase {

    final ToolsProbingTool tool = new ToolsProbingTool();

    ToolsProber prober;

    @BeforeEach
    public void before_each() {
        prober = AgenticServices.agentBuilder(ToolsProber.class)
        .chatModel(model)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(2))
        .tools(tool)
        .build();
    }

    @Test
    public void probe_agents_messages() {
        prober.probe("some text");
        final ChatModelRequestContext request = listener.lastRequestContext.get();

        //
        // In the case of tool execution with dummy model we have only the
        // system message
        //
        then((SystemMessage)request.chatRequest().messages().get(0))
            .isEqualTo(new SystemMessage(ToolsProber.SYSTEM_MESSAGE));
    }

    @Test
    public void probe_detects_tools_are_supported() {
        model.toolChoice = ToolChoice.REQUIRED;
        then(prober.probe(tool.probeText)).isTrue();
    }

    @Test
    public void probe_detects_tools_are_not_supported() {
        model.toolChoice = ToolChoice.NONE;
        then(prober.probe(tool.probeText)).isFalse();
    }

    @Test
    public void probe_detects_tools_are_not_supported_with_error() {
        model.toolChoice = ToolChoice.REQUIRED;
        model.error = new RuntimeException("I do not support tools!");
        then(prober.probe(tool.probeText)).isFalse();
    }

    @Test
    public void probing_illegal_values_throws_error() {
        for(String B: new String[] {null, "", "   ", " \n", "\t"}) {
            thenThrownBy( () -> prober.probe(B) )
                .isInstanceOf(UndeclaredThrowableException.class)
                .cause()
                    .isInstanceOf(InvocationTargetException.class)
                    .cause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("probeText can not be null or blank");
        }
    }

    @Test
    public void log_if_model_throws_error() {
        model.toolChoice = ToolChoice.REQUIRED;
        model.error = new RuntimeException("I do not support tools!");
        prober.probe(tool.probeText);
        then(logHandler.getMessages(Level.FINEST)).contains("I do not support tools!");
    }
}

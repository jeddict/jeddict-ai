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

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class FileWizardTest extends PairProgrammerTestBase {

    private FileWizard pair;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(FileWizard.class)
            .chatModel(model)
            .build();
    }

    @Test
    public void pair_is_a_PairProgrammer() {
        then(pair).isInstanceOf(PairProgrammer.class);
    }

    @Test
    public void newFile_with_all_parameters() {
        final String content = pair.newFile(
            "use mock 'hello world.txt'", "the context",
            "/tmp/filename.txt", "the content", "JDK 21",
            "global rules", "project rules"
        );

        final ChatModelRequestContext request = listener.lastRequestContext.get();

        then(((SystemMessage)request.chatRequest().messages().get(0)).text())
            .isEqualTo(
                FileWizard.SYSTEM_MESSAGE.replace("{{globalRules}}", "global rules")
                    .replace("{{projectRules}}", "project rules")
            );

        then(((UserMessage)request.chatRequest().messages().get(1)).singleText())
            .contains("the context")
            .contains("/tmp/filename.txt")
            .contains("the content")
            .contains("project info: JDK 21");

        then(content).isEqualToIgnoringNewLines("hello world");
    }

    @Test
    public void newFile_with_empty_and_null_parameters() {
        String content = pair.newFile(
            "", "use mock 'hello world.txt'", null, null, null, null, null
        );

        ChatModelRequestContext request = listener.lastRequestContext.get();

        then(((SystemMessage)request.chatRequest().messages().get(0)).text())
            .isEqualTo(
                FileWizard.SYSTEM_MESSAGE.replace("{{globalRules}}", "")
                    .replace("{{projectRules}}", "")
            );

        then(((UserMessage)request.chatRequest().messages().get(1)).singleText())
            .contains(FileWizard.USER_MESSAGE_DEFAULT)
            .contains("filename: \n")
            .contains("context: use mock 'hello world.txt'\n")
            .contains("content:\n```\n\n```\n");

        then(content).isEqualToIgnoringNewLines("hello world");

        content = pair.newFile(
            "use mock 'hello world.txt'",null , "", "  ", "\n", "", ""
        );
        request = listener.lastRequestContext.get();

        then(((SystemMessage)request.chatRequest().messages().get(0)).text())
            .isEqualTo(
                FileWizard.SYSTEM_MESSAGE.replace("{{globalRules}}", "")
                    .replace("{{projectRules}}", "")
            );

        then(((UserMessage)request.chatRequest().messages().get(1)).singleText())
            .contains("filename: \n")
            .contains("context: \n")
            .contains("content:\n```\n\n```\n")
            .contains("project info: \n");

        then(content).isEqualToIgnoringNewLines("hello world");

    }

}

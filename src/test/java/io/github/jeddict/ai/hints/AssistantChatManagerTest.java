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
package io.github.jeddict.ai.hints;

import io.github.jeddict.ai.test.TestBase;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AssistantChatManager}.
 *
 * These tests focus on the logic that ensures the project file tree is sent
 * only on the first query in a chat session and not repeated on subsequent
 * queries in the same window.
 */
public class AssistantChatManagerTest extends TestBase {

    private static final String PROJECT_INFO = "- name: myapp\n- folder: /workspace/myapp\n- type: maven";
    private static final String FILE_TREE = "pom.xml\nsrc/\n  main/\n    java/";

    @Test
    public void buildAgentProjectInfo_includes_file_tree_when_provided() {
        //
        // On the first query hacker is null, so fileTree is obtained from
        // ProjectMetadataInfo.getFileTree() and passed in non-blank.
        // Verify that the file tree section is appended to the project info.
        //
        final String result = AssistantChatManager.buildAgentProjectInfo(PROJECT_INFO, FILE_TREE);

        then(result)
            .contains(PROJECT_INFO)
            .contains("- File Tree:")
            .contains(FILE_TREE);
    }

    @Test
    public void buildAgentProjectInfo_excludes_file_tree_and_separator_when_empty() {
        //
        // On subsequent queries hacker is already cached (non-null), so an
        // empty string is passed for fileTree. Neither the file tree content
        // nor the "\n- File Tree:\n" separator must be appended to projectInfo.
        //
        final String result = AssistantChatManager.buildAgentProjectInfo(PROJECT_INFO, "");

        then(result)
            .isEqualTo(PROJECT_INFO)
            .doesNotContain("- File Tree:");
    }

    @Test
    public void buildAgentProjectInfo_excludes_file_tree_and_separator_when_blank() {
        //
        // Guard against whitespace-only file tree strings: neither the file
        // tree content nor the "\n- File Tree:\n" separator must be appended.
        //
        final String result = AssistantChatManager.buildAgentProjectInfo(PROJECT_INFO, "   ");

        then(result)
            .isEqualTo(PROJECT_INFO)
            .doesNotContain("- File Tree:");
    }
}

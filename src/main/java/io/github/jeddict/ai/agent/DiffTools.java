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
import dev.langchain4j.exception.ToolExecutionException;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.INTERACTIVE;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.components.diff.DiffPane;
import io.github.jeddict.ai.components.diff.DiffPaneController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A tool for performing diff operations.
 */
public class DiffTools extends AbstractTool {

    private final AssistantChat assistantChat;

    public DiffTools(String basedir, AssistantChat assistantChat) {
        super(basedir);
        this.assistantChat = assistantChat;
    }

    @Tool("""
    Show the differences between the proposed new content of a file and its current content
    to facilitate review and acceptance via a diff viewer interface.

    Inputs:
    - 'path': the filesystem path of the file to be changed; this path can point to a file that does not yet exist.
    - 'content': the proposed new content for the file.

    Outputs:
    - If the changes are accepted by the user, returns the string:
      "ACCEPTED:\n<final content>"
    - If the changes are rejected by the user, returns the string:
      "REJECTED"
    """
    )
    @ToolPolicy(INTERACTIVE)
    public String diff(final String path, final String content)
    throws ToolExecutionException {
        progress("∆ Performing diff on " + path + " with proposed content " + StringUtils.abbreviate(content, 40));

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> newContent = new AtomicReference();
        final AtomicBoolean accepted = new AtomicBoolean(true);
        SwingUtilities.invokeLater(() -> {
            final DiffPane diffPane = assistantChat.createDiffPane(path, content);
            diffPane.onDone((action) -> {
                if (action == DiffPaneController.UserAction.ACCEPT) {
                    newContent.set(diffPane.ctrl.content());
                } else {
                    accepted.set(false);
                }
                done.countDown();
            });
        });

        try {
            done.await();

            log.finest(() -> "changes %s".formatted((accepted.get()) ? "accepted" : "rejected"));
            //
            // if the content was accepted, return it to the LM, otherwise
            // throw a ToolExecutionException
            //
            if (accepted.get()) {
                return "ACCEPTED:\n"+newContent.get();
            }

            return "REJECTED";
        } catch (InterruptedException x) {
            throw new ToolExecutionException("error in getting the content: " + x.getMessage());
        }
    }
}
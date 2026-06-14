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
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.INTERACTIVE;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.components.diff.DiffPane;
import io.github.jeddict.ai.components.diff.DiffPaneController;
import io.github.jeddict.ai.lang.InteractionMode;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A tool for performing diff operations.
 */
public class InteractiveFileEditor extends AbstractTool {

    private final AssistantChat assistantChat;

    public InteractiveFileEditor(String basedir, AssistantChat assistantChat) throws IOException {
        super(basedir);
        this.assistantChat = assistantChat;
    }

    @Tool("""
    Interactive editor to create and edit text content and source code.

    Inputs:
    - 'path': the filesystem path of the file to be changed; this path may be for
      a file that does not yet exist.
    - 'content': the proposed new content for the file.

    Output:
    - The final content as accepted by the user
    - The string 'REJECTED' if the user rejected the changes
    """
    )
    @ToolPolicy(INTERACTIVE)
    public String editFile(
        @P("the filesystem path of the file to be changed")
        final String path,
        @P("the proposed new content for the file")
        final String content
    ) throws ToolExecutionException {
        if (StringUtils.isBlank(path)) {
            throw new ToolExecutionException("path can not be null or empty");
        }
        if (content == null) {
            throw new ToolExecutionException("path can not be null");
        }

        //
        // If the tool is invoched in iteraction modes different than INTERACTIVE,
        // create the file right away with the createBinaryTool. The design is
        // not great but it is what we can do given current Jeddict and langchain4j
        // design.
        //
        if (interaction != InteractionMode.INTERACTIVE) {
            try {
                final FileSystemTools delegate = new FileSystemTools(basedir);
                return delegate.createBinaryFile(path, content.getBytes());
            } catch (IOException x) {
                throw new ToolExecutionException("error in getting the content: " + x.getMessage());
            }
        }

        progress("∆ Editing " + path);

        checkPath(path);

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> newContent = new AtomicReference();
        final AtomicBoolean accepted = new AtomicBoolean(true);
        SwingUtilities.invokeLater(() -> {
            final DiffPane diffPane = assistantChat.createDiffPane(path, content);
            diffPane.onDone((action) -> {
                if (action == DiffPaneController.UserAction.ACCEPT) {
                    newContent.set(diffPane.ctrl.modified());
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
            // if the modified was accepted, return it to the LM, otherwise
            // throw a ToolExecutionException
            //
            if (accepted.get()) {
                return newContent.get();
            }

            throw new ToolExecutionRejected();
        } catch (InterruptedException x) {
            throw new ToolExecutionException("error in getting the content: " + x.getMessage());
        }
    }
}

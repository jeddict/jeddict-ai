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
package io.github.jeddict.ai.lang;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.response.TokenHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
 */
public abstract class JeddictStreamHandler
    implements StreamingChatResponseHandler, PropertyChangeListener
{

    private final AssistantChat topComponent;
    private boolean init = true;
    private JTextArea textArea;
    private ProgressHandle handle;
    private boolean complete;
    protected final StringBuilder toolingResponse = new StringBuilder();

    private static final Logger LOG = Logger.getLogger(JeddictStreamHandler.class.getName());

    public JeddictStreamHandler(AssistantChat topComponent) {
        this.topComponent = topComponent;

        handle = ProgressHandle.createHandle(NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", 0));
        handle.start();
    }

    public ProgressHandle getProgressHandle() {
        return handle;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        LOG.finest(() -> String.valueOf(e));
        final String name = e.getPropertyName();
        if (JeddictBrain.PROPERTY_TOKENS.equals(name)) {
            final String progress = NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", (int)e.getNewValue());
            handle.progress(progress);
            handle.setDisplayName(progress);
        } else if (AbstractTool.PROPERTY_MESSAGE.equals(name)) {
            final String msg = (String)e.getNewValue();
            toolingResponse.append(msg).append('\n');
            onPartialResponse(msg);
        }
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        LOG.finest(() -> "partial response: " + partialResponse);
        if (init) {
            topComponent.clear();
            textArea = topComponent.createTextAreaPane();
            textArea.setText(partialResponse);
            init = false;
        } else {
            textArea.append(partialResponse);
        }
    }

    public boolean isComplete() {
        return complete;
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.finest(() -> "error received: " + throwable);
        complete = true;
        // Log the error with timestamp and thread info
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String threadName = Thread.currentThread().getName();
        LOG.log(Level.SEVERE, "Error occurred at {0} on thread [{1}]", new Object[] { timestamp, threadName });
        LOG.log(Level.SEVERE, "Exception in JeddictStreamHandler", throwable);

        //
        // Build the error message
        //
        final StringWriter w = new StringWriter();
            w.append("An error occurred: " + throwable.getMessage());
            w.append("\n\nMore details:\n\n");
            throwable.printStackTrace(new PrintWriter(w));
        final String error = w.toString();

        // Update UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (textArea != null) {
                textArea.append("\n\n" + error);
            } else {
                // If textArea not yet initialized, clear and create one to show error
                topComponent.clear();
                JTextArea errorArea = topComponent.createTextAreaPane();
                errorArea.setText(error);
                textArea = errorArea;
            }

            onCompleteResponse(
                ChatResponse.builder().aiMessage(new AiMessage(error)).build()
            );
        });
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        LOG.finest(() -> "complete response received: " + completeResponse);
        complete = true;
        SwingUtilities.invokeLater(() -> {
            String response = completeResponse.aiMessage().text();
            if (response != null && !response.isEmpty()) {
                CompletableFuture.runAsync(() -> TokenHandler.saveOutputToken(response));
            }
            handle.finish();
        });
    }

}

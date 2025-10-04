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
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.components.AssistantChat;
import static io.github.jeddict.ai.lang.JeddictChatModelBuilder.pm;
import io.github.jeddict.ai.response.TokenHandler;
import io.github.jeddict.ai.util.Utilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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

        switch (name) {
            case JeddictBrain.PROPERTY_TOKENS: {
                final String progress = NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", (int)e.getNewValue());
                handle.progress(progress);
                handle.setDisplayName(progress);
            }; break;
            case JeddictBrain.PROPERTY_ERROR: {
                onError((Exception)e.getNewValue());
            }; break;
            case AbstractTool.PROPERTY_MESSAGE: {
                final String msg = (String)e.getNewValue();
                toolingResponse.append(msg).append('\n');
                onPartialResponse(msg);
            }; break;
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
    public void onError(final Throwable throwable) {
        LOG.finest(() -> "error received: " + throwable);

        complete = true;
        // Log the error with timestamp and thread info
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String threadName = Thread.currentThread().getName();
        LOG.log(Level.SEVERE, "Error occurred at {0} on thread [{1}]", new Object[] { timestamp, threadName });
        LOG.log(Level.SEVERE, "Exception in JeddictStreamHandler", throwable);

        final String error = Utilities.errorHTMLBlock(throwable);
        // Update UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            onCompleteResponse(
                ChatResponse.builder().aiMessage(new AiMessage(error)).build()
            );
        });

        if (throwable instanceof AuthenticationException) {
            confirmApiKey();
        } else if (throwable instanceof ModelNotFoundException) {
            showError("Invalid model, check assistant settings.");
        } else {
            showError(throwable.getMessage());
        }

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

    private void confirmApiKey() {
        JTextField apiKeyField = new JTextField(20);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Set layout to BoxLayout

        panel.add(new JLabel("Incorrect API key. Please enter a new key:"));
        panel.add(Box.createVerticalStrut(10)); // Add space between label and text field
        panel.add(apiKeyField);

        int option = JOptionPane.showConfirmDialog(
            null, panel,
            pm.getProvider().name() + " API Key Required",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION) {
            pm.setApiKey(apiKeyField.getText().trim());
        }
    }

    private void showError(final String msg) {
        JOptionPane.showMessageDialog(
            null,
            "<html>AI assistant failed to generate the requested response" +
            ((msg != null) ? (": " + msg) : "") +
            "<br>See the chat for details.",
            "Error in AI Assistant",
            JOptionPane.ERROR_MESSAGE
        );
    }
}

package io.github.jeddict.ai.lang;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.model.chat.response.ChatResponse;
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
 * Purpose of this class is to receive events emitted by the JeddictBrain system
 * and react accordingly in the NetBeans platform. It is intended to be used for
 * any kind of models,  streaming and not-streaming, tooling and not tooling,
 * Q&A and agentic.
 *
 *
 * @author Shiwani Gupta
 */
public class JeddictBrainListener
    implements PropertyChangeListener
{

    protected final AssistantChat topComponent;
    protected boolean init = true;
    protected JTextArea textArea;
    protected final ProgressHandle handle;
    protected final StringBuilder toolingResponse = new StringBuilder();

    private static final Logger LOG = Logger.getLogger(JeddictBrainListener.class.getName());

    public JeddictBrainListener(AssistantChat topComponent) {
        this.topComponent = topComponent;

        handle = ProgressHandle.createHandle(NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", 0));
    }

    public ProgressHandle getProgressHandle() {
        return handle;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        LOG.finest(() -> String.valueOf(e));
        final String name = e.getPropertyName();

        if (name.equals(JeddictBrain.EventProperty.CHAT_TOKENS.name)) {
            SwingUtilities.invokeLater(() -> {
                final String progress = NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", (int)e.getNewValue());
                handle.start();
                handle.progress(progress);
                handle.setDisplayName(progress);
            });
        } else if (name.equals(JeddictBrain.EventProperty.CHAT_PARTIAL.name)) {
            onPartialResponse((String)e.getNewValue());
        } else if (name.equals(JeddictBrain.EventProperty.CHAT_COMPLETED.name)) {
            SwingUtilities.invokeLater(() -> {
                handle.finish();
            });
            onCompleteResponse((ChatResponse)e.getNewValue());
        } else if (name.equals(JeddictBrain.EventProperty.CHAT_ERROR.name)) {
            SwingUtilities.invokeLater(() -> {
                handle.finish();
            });
            onError((Exception)e.getNewValue());
        } else if (name.equals(AbstractTool.PROPERTY_MESSAGE)) {
            final String msg = (String)e.getNewValue() + '\n';
            toolingResponse.append(msg);
            onPartialResponse(msg);
        }
    }

    public void onPartialResponse(String partialResponse) {
        LOG.finest(() -> "partial response: " + partialResponse);
        SwingUtilities.invokeLater(() -> {
            if (init) {
                topComponent.clear();
                textArea = topComponent.createTextAreaPane();
                textArea.setText(partialResponse);
                init = false;
            } else {
                textArea.append(partialResponse);
            }
        });
    }

    public void onCompleteResponse(ChatResponse completeResponse) {
        LOG.finest(() -> "complete response received: " + completeResponse);

        String response = completeResponse.aiMessage().text();
        if (response != null && !response.isEmpty()) {
            CompletableFuture.runAsync(() -> TokenHandler.saveOutputToken(response));
        }
    }

    public void onError(final Throwable throwable) {
        LOG.finest(() -> "error received: " + throwable);

        // Log the error with timestamp and thread info
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String threadName = Thread.currentThread().getName();
        LOG.log(Level.SEVERE, "Error occurred at {0} on thread [{1}]", new Object[] { timestamp, threadName });
        LOG.log(Level.SEVERE, "Exception in JeddictStreamHandler", throwable);

        final String error = Utilities.errorHTMLBlock(throwable);
        onCompleteResponse(
            ChatResponse.builder().aiMessage(new AiMessage(error)).build()
        );

        if (throwable instanceof AuthenticationException) {
            confirmApiKey();
        } else if (throwable instanceof ModelNotFoundException) {
            showError("Invalid model, check assistant settings.");
        } else {
            showError(throwable.getMessage());
        }
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

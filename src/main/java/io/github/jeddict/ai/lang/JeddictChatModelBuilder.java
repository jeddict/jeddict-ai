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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.lang.impl.AnthropicBuilder;
import io.github.jeddict.ai.lang.impl.AnthropicStreamingBuilder;
import io.github.jeddict.ai.lang.impl.GoogleBuilder;
import io.github.jeddict.ai.lang.impl.GoogleStreamingBuilder;
import io.github.jeddict.ai.lang.impl.LMStudioBuilder;
import io.github.jeddict.ai.lang.impl.LocalAiBuilder;
import io.github.jeddict.ai.lang.impl.LocalAiStreamingBuilder;
import io.github.jeddict.ai.lang.impl.MistralBuilder;
import io.github.jeddict.ai.lang.impl.MistralStreamingBuilder;
import io.github.jeddict.ai.lang.impl.OllamaBuilder;
import io.github.jeddict.ai.lang.impl.OllamaStreamingBuilder;
import io.github.jeddict.ai.lang.impl.OpenAiBuilder;
import io.github.jeddict.ai.lang.impl.OpenAiStreamingBuilder;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.response.TokenHandler;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import static io.github.jeddict.ai.settings.GenAIProvider.ANTHROPIC;
import static io.github.jeddict.ai.settings.GenAIProvider.CUSTOM_OPEN_AI;
import static io.github.jeddict.ai.settings.GenAIProvider.DEEPINFRA;
import static io.github.jeddict.ai.settings.GenAIProvider.DEEPSEEK;
import static io.github.jeddict.ai.settings.GenAIProvider.GOOGLE;
import static io.github.jeddict.ai.settings.GenAIProvider.GPT4ALL;
import static io.github.jeddict.ai.settings.GenAIProvider.GROQ;
import static io.github.jeddict.ai.settings.GenAIProvider.LM_STUDIO;
import static io.github.jeddict.ai.settings.GenAIProvider.MISTRAL;
import static io.github.jeddict.ai.settings.GenAIProvider.OLLAMA;
import static io.github.jeddict.ai.settings.GenAIProvider.OPEN_AI;
import static io.github.jeddict.ai.settings.GenAIProvider.PERPLEXITY;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.json.JSONObject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class JeddictChatModelBuilder {

    private ChatModel model;
    private StreamingChatModel streamModel;
    protected static PreferencesManager pm = PreferencesManager.getInstance();
    private JeddictStreamHandler handler;

    public JeddictChatModelBuilder() {
        this(null);
    }

    public JeddictChatModelBuilder(JeddictStreamHandler handler) {
        this(handler, pm.getModel());
    }

    public JeddictChatModelBuilder(JeddictStreamHandler handler, String modelName) {
        this.handler = handler;

        if (null != modelName) {
            if (pm.isStreamEnabled() && handler != null) {
                switch (pm.getProvider()) {

                    case GOOGLE -> {
                        streamModel = buildModel(new GoogleStreamingBuilder(), modelName);
                    }
                    case OPEN_AI, DEEPINFRA, DEEPSEEK, GROQ, CUSTOM_OPEN_AI, COPILOT_PROXY, PERPLEXITY -> {
                        streamModel = buildModel(new OpenAiStreamingBuilder(), modelName);
                    }
                    case MISTRAL -> {
                        streamModel = buildModel(new MistralStreamingBuilder(), modelName);
                    }
                    case ANTHROPIC -> {
                        streamModel = buildModel(new AnthropicStreamingBuilder(), modelName);
                    }
                    case OLLAMA -> {
                        streamModel = buildModel(new OllamaStreamingBuilder(), modelName);
                    }
                    case LM_STUDIO -> {
                        model = buildModel(new LMStudioBuilder(), modelName);
                    }
                    case GPT4ALL -> {
                        streamModel = buildModel(new LocalAiStreamingBuilder(), modelName);
                    }
                }
            } else {
                switch (pm.getProvider()) {

                    case GOOGLE -> {
                        model = buildModel(new GoogleBuilder(), modelName);
                    }
                    case OPEN_AI, DEEPINFRA, DEEPSEEK, GROQ, CUSTOM_OPEN_AI, COPILOT_PROXY, PERPLEXITY -> {
                        model = buildModel(new OpenAiBuilder(), modelName);
                    }
                    case MISTRAL -> {
                        model = buildModel(new MistralBuilder(), modelName);
                    }
                    case ANTHROPIC -> {
                        model = buildModel(new AnthropicBuilder(), modelName);
                    }
                    case OLLAMA -> {
                        model = buildModel(new OllamaBuilder(), modelName);
                    }
                    case LM_STUDIO -> {
                        model = buildModel(new LMStudioBuilder(), modelName);
                    }
                    case GPT4ALL -> {
                        model = buildModel(new LocalAiBuilder(), modelName);
                    }
                }
            }
        }
    }

    private <T> void setIfValid(final Consumer<T> setter, final T value, final T invalidValue) {
        if (value != null && !value.equals(invalidValue)) {
            setter.accept(value);
        }
    }

    private <T> void setIfPredicate(final Consumer<T> setter, final T value, final Predicate<T> predicate) {
        if (value != null && !predicate.test(value)) {
            setter.accept(value);
        }
    }

    private <T> ChatModelBaseBuilder<T> builderModel(final ChatModelBaseBuilder<T> builder, String modelName) {
        setIfPredicate(builder::baseUrl, pm.getProviderLocation(), String::isEmpty);
        setIfPredicate(builder::customHeaders, pm.getCustomHeaders(), Map::isEmpty);
        boolean headless = pm.getProviderLocation() != null;
        builder
                .apiKey(pm.getApiKey(headless))
                .modelName(modelName);

        setIfValid(builder::temperature, pm.getTemperature(), Double.MIN_VALUE);
        setIfValid(value -> builder.timeout(Duration.ofSeconds(value)), pm.getTimeout(), Integer.MIN_VALUE);
        if (builder instanceof ChatModelBuilder) {
            setIfValid(((ChatModelBuilder)builder)::maxRetries, pm.getMaxRetries(), Integer.MIN_VALUE);
        }
        setIfValid(builder::maxOutputTokens, pm.getMaxOutputTokens(), Integer.MIN_VALUE);
        setIfValid(builder::repeatPenalty, pm.getRepeatPenalty(), Double.MIN_VALUE);
        setIfValid(builder::seed, pm.getSeed(), Integer.MIN_VALUE);
        setIfValid(builder::maxTokens, pm.getMaxTokens(), Integer.MIN_VALUE);
        setIfValid(builder::maxCompletionTokens, pm.getMaxCompletionTokens(), Integer.MIN_VALUE);
        setIfValid(builder::topK, pm.getTopK(), Integer.MIN_VALUE);
        setIfValid(builder::presencePenalty, pm.getPresencePenalty(), Double.MIN_VALUE);
        setIfValid(builder::frequencyPenalty, pm.getFrequencyPenalty(), Double.MIN_VALUE);
        setIfPredicate(builder::organizationId, pm.getOrganizationId(), String::isEmpty);

        builder.logRequestsResponses(pm.isLogRequestsEnabled(), pm.isLogResponsesEnabled())
                .includeCodeExecutionOutput(pm.isIncludeCodeExecutionOutput())
                .allowCodeExecution(pm.isAllowCodeExecution());

        return builder;
    }

    private <T> T buildModel(final ChatModelBaseBuilder<T> builder, String modelName) {
        return builderModel(builder, modelName).build();
    }

    public String generate(final Project project, final String prompt) {
        return generateInternal(project, prompt, null, null);
    }

    public String generate(final Project project, final String prompt, List<String> images, List<Response> responseHistory) {
        return generateInternal(project, prompt, images, responseHistory);
    }

    public UserMessage buildUserMessage(String prompt, List<String> imageBase64Urls) {
        List<Content> parts = new ArrayList<>();

        // Add the prompt text
        parts.add(new TextContent(prompt));

        // Add each image as ImageContent
        for (String imageUrl : imageBase64Urls) {
            parts.add(new ImageContent(imageUrl));
        }

        // Convert list to varargs
        return UserMessage.from(parts.toArray(new Content[0]));
    }

    private String generateInternal(Project project, String prompt, List<String> images, List<Response> responseHistory) {
        if (model == null && handler == null) {
            JOptionPane.showMessageDialog(null,
                    "AI assistance model not intitalized.",
                    "Error in AI Assistance",
                    JOptionPane.ERROR_MESSAGE);
        }
        if (project != null) {
            prompt = prompt + "\n" + ProjectMetadataInfo.get(project);
        }
        String systemMessage = null;
        String globalRules = PreferencesManager.getInstance().getGlobalRules();
        if (globalRules != null) {
            systemMessage = globalRules;
        }
        if (project != null) {
            String projectRules = PreferencesManager.getInstance().getProjectRules(project);
            if (projectRules != null) {
                systemMessage = systemMessage + '\n' + projectRules;
            }
        }
        List<ChatMessage> messages = new ArrayList<>();
        if (systemMessage != null && !systemMessage.trim().isEmpty()) {
            messages.add(SystemMessage.from(systemMessage));
        }

        // add conversation history (multiple responses)
        if (responseHistory != null && !responseHistory.isEmpty()) {
            for (Response res : responseHistory) {
                messages.add(UserMessage.from(res.getQuery()));
                messages.add(AiMessage.from(res.toString()));
            }
        }

        if (images != null && !images.isEmpty()) {
            messages.add(buildUserMessage(prompt, images));
        } else {
            messages.add(UserMessage.from(prompt));
        }
        int tokenCount = TokenHandler.saveInputToken(messages);
        String handleMessage = NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", tokenCount);
        ProgressHandle handle = ProgressHandle.createHandle(handleMessage);
        handle.start();

        try {
            if (streamModel != null) {
                handler.setHandle(handle);
                streamModel.chat(messages, handler);
            } else {
                String response = model.chat(messages).aiMessage().text();
                CompletableFuture.runAsync(() -> TokenHandler.saveOutputToken(response));
                handle.finish();
                return response;
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                //
                // let's pretend it is a JSON object, if not, ignore it
                //
                try {
                    JSONObject jsonObject = new JSONObject(e.getCause().getMessage());
                    if (jsonObject.has("error") && jsonObject.getJSONObject("error").has("message")) {
                        errorMessage = jsonObject.getJSONObject("error").getString("message");
                    }
                } catch (Throwable x) {
                    //
                    // It was not a proper JSON
                    //
                }
            }
            if (errorMessage != null
                    && errorMessage.toLowerCase().contains("incorrect api key")) {
                JTextField apiKeyField = new JTextField(20);
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Set layout to BoxLayout

                panel.add(new JLabel("Incorrect API key. Please enter a new key:"));
                panel.add(Box.createVerticalStrut(10)); // Add space between label and text field
                panel.add(apiKeyField);

                int option = JOptionPane.showConfirmDialog(null, panel,
                        pm.getProvider().name() + " API Key Required", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    pm.setApiKey(apiKeyField.getText().trim());
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "AI assistance failed to generate the requested response: " + errorMessage,
                        "Error in AI Assistance",
                        JOptionPane.ERROR_MESSAGE);
                handler.onError(e);
            }
            handle.finish();
        }
        return null;
    }

}

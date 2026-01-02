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

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import io.github.jeddict.ai.lang.impl.AnthropicBuilder;
import io.github.jeddict.ai.lang.impl.AnthropicStreamingBuilder;
import io.github.jeddict.ai.lang.impl.GoogleBuilder;
import io.github.jeddict.ai.lang.impl.GoogleStreamingBuilder;
import io.github.jeddict.ai.lang.impl.LMStudioBuilder;
import io.github.jeddict.ai.lang.impl.LMStudioStreamingBuilder;
import io.github.jeddict.ai.lang.impl.LocalAiBuilder;
import io.github.jeddict.ai.lang.impl.LocalAiStreamingBuilder;
import io.github.jeddict.ai.lang.impl.MistralBuilder;
import io.github.jeddict.ai.lang.impl.MistralStreamingBuilder;
import io.github.jeddict.ai.lang.impl.OllamaBuilder;
import io.github.jeddict.ai.lang.impl.OllamaStreamingBuilder;
import io.github.jeddict.ai.lang.impl.OpenAiBuilder;
import io.github.jeddict.ai.lang.impl.OpenAiStreamingBuilder;
import static io.github.jeddict.ai.models.registry.GenAIProvider.ANTHROPIC;
import static io.github.jeddict.ai.models.registry.GenAIProvider.COPILOT_PROXY;
import static io.github.jeddict.ai.models.registry.GenAIProvider.CUSTOM_OPEN_AI;
import static io.github.jeddict.ai.models.registry.GenAIProvider.DEEPINFRA;
import static io.github.jeddict.ai.models.registry.GenAIProvider.DEEPSEEK;
import static io.github.jeddict.ai.models.registry.GenAIProvider.GOOGLE;
import static io.github.jeddict.ai.models.registry.GenAIProvider.GPT4ALL;
import static io.github.jeddict.ai.models.registry.GenAIProvider.GROQ;
import static io.github.jeddict.ai.models.registry.GenAIProvider.LM_STUDIO;
import static io.github.jeddict.ai.models.registry.GenAIProvider.MISTRAL;
import static io.github.jeddict.ai.models.registry.GenAIProvider.OLLAMA;
import static io.github.jeddict.ai.models.registry.GenAIProvider.OPEN_AI;
import static io.github.jeddict.ai.models.registry.GenAIProvider.PERPLEXITY;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 *
 * @author Gaurav Gupta
 */
public class JeddictChatModelBuilder {

    public final Logger LOG = Logger.getLogger(JeddictChatModelBuilder.class.getCanonicalName());

    protected static PreferencesManager pm = PreferencesManager.getInstance();
    private String modelName;

    private final ChatModelListener listener;

    public JeddictChatModelBuilder() {
        this(null);
    }

    public JeddictChatModelBuilder(final String modelName) {
        this(modelName, null); // P2 - TODO: can this be null?
    }

    public JeddictChatModelBuilder(
        final String modelName, final ChatModelListener listener
    ) {
        this.modelName = modelName; // P2 - TODO: can this be null?
        this.listener = listener;
    }

    public ChatModel build() {
        LOG.finest(() -> "Building model " + modelName);

        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }

        return switch (pm.getProvider()) {
            case GOOGLE -> buildModel(new GoogleBuilder(), modelName);
            case OPEN_AI, DEEPINFRA, DEEPSEEK, GROQ, CUSTOM_OPEN_AI, COPILOT_PROXY, PERPLEXITY ->
                buildModel(new OpenAiBuilder(), modelName);
            case MISTRAL -> buildModel(new MistralBuilder(), modelName);
            case ANTHROPIC -> buildModel(new AnthropicBuilder(), modelName);
            case OLLAMA -> buildModel(new OllamaBuilder(), modelName);
            case LM_STUDIO -> buildModel(new LMStudioBuilder(), modelName);
            case GPT4ALL -> buildModel(new LocalAiBuilder(), modelName);
            default -> {
                final String msg = String.format("No model %s found for provider %s, this is most likely a bug",
                        modelName, pm.getProvider());

                LOG.severe(msg);
                throw new IllegalArgumentException(msg);
            }
        };
    }

    public StreamingChatModel buildStreaming() {
        LOG.finest(() -> "Building streaming model " + modelName);

        return switch (pm.getProvider()) {
            case GOOGLE -> buildModel(new GoogleStreamingBuilder(), modelName);
            case OPEN_AI, DEEPINFRA, DEEPSEEK, GROQ, CUSTOM_OPEN_AI, COPILOT_PROXY, PERPLEXITY ->
                buildModel(new OpenAiStreamingBuilder(), modelName);
            case MISTRAL -> buildModel(new MistralStreamingBuilder(), modelName);
            case ANTHROPIC -> buildModel(new AnthropicStreamingBuilder(), modelName);
            case OLLAMA -> buildModel(new OllamaStreamingBuilder(), modelName);
            case LM_STUDIO -> buildModel(new LMStudioStreamingBuilder(), modelName);
            case GPT4ALL -> buildModel(new LocalAiStreamingBuilder(), modelName);
            default -> {
                final String msg = String.format("No streaming model %s found for provider %s", modelName,
                        pm.getProvider());

                LOG.severe(msg);
                throw new IllegalArgumentException(msg);
            }
        };
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
            setIfValid(((ChatModelBuilder) builder)::maxRetries, pm.getMaxRetries(), Integer.MIN_VALUE);
        }
        setIfValid(builder::maxOutputTokens, pm.getMaxOutputTokens(), Integer.MIN_VALUE);
        setIfValid(builder::repeatPenalty, pm.getRepeatPenalty(), Double.MIN_VALUE);
        setIfValid(builder::seed, pm.getSeed(), Integer.MIN_VALUE);
        setIfValid(builder::maxTokens, pm.getMaxTokens(), Integer.MIN_VALUE);
        setIfValid(builder::maxCompletionTokens, pm.getMaxCompletionTokens(), Integer.MIN_VALUE);
        setIfValid(builder::topK, pm.getTopK(), Integer.MIN_VALUE);
        setIfValid(builder::presencePenalty, pm.getPresencePenalty(), Double.MIN_VALUE);
        setIfValid(builder::frequencyPenalty, pm.getFrequencyPenalty(), Double.MIN_VALUE);
        setIfValid(builder::listeners, List.of(listener), List.of());
        setIfPredicate(builder::organizationId, pm.getOrganizationId(), String::isEmpty);

        builder.logRequestsResponses(pm.isLogRequestsEnabled(), pm.isLogResponsesEnabled())
                .includeCodeExecutionOutput(pm.isIncludeCodeExecutionOutput())
                .allowCodeExecution(pm.isAllowCodeExecution());

        return builder;
    }

    private <T> T buildModel(final ChatModelBaseBuilder<T> builder, String modelName) {
        return builderModel(builder, modelName).build();
    }
}

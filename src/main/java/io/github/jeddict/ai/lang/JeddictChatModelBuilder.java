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
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
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
import static io.github.jeddict.ai.settings.GenAIProvider.ANTHROPIC;
import static io.github.jeddict.ai.settings.GenAIProvider.COPILOT_PROXY;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 *
 * @author Gaurav Gupta
 */
public class JeddictChatModelBuilder {

    protected static PreferencesManager pm = PreferencesManager.getInstance();
    private StreamingChatResponseHandler handler;
    private String modelName;

    public JeddictChatModelBuilder() {
        this(null);
    }

    public JeddictChatModelBuilder(StreamingChatResponseHandler handler) {
        this(handler, pm.getModel());
    }

    public JeddictChatModelBuilder(StreamingChatResponseHandler handler, String modelName) {
        this.handler = handler;
        this.modelName = modelName; // P2 - TODO: can this be null?
    }

    public ChatModel build() {
        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }

        ChatModel model = null;
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

        // TODO: P2 - what if none of those?

        return model;
    }

    public StreamingChatModel buildStreaming() {
        // TODO: P3 - if (!pm.isStreamEnabled() || handler != null) throw IllegalStateException

        StreamingChatModel model = null;
        switch (pm.getProvider()) {
            case GOOGLE -> {
                model = buildModel(new GoogleStreamingBuilder(), modelName);
            }
            case OPEN_AI, DEEPINFRA, DEEPSEEK, GROQ, CUSTOM_OPEN_AI, COPILOT_PROXY, PERPLEXITY -> {
                model = buildModel(new OpenAiStreamingBuilder(), modelName);
            }
            case MISTRAL -> {
                model = buildModel(new MistralStreamingBuilder(), modelName);
            }
            case ANTHROPIC -> {
                model = buildModel(new AnthropicStreamingBuilder(), modelName);
            }
            case OLLAMA -> {
                model = buildModel(new OllamaStreamingBuilder(), modelName);
            }
            case GPT4ALL -> {
                model = buildModel(new LocalAiStreamingBuilder(), modelName);
            }
        }

        // TODO: what if none of those?

        return model;

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
}

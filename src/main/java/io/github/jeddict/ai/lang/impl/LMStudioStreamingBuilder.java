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
package io.github.jeddict.ai.lang.impl;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.github.jeddict.ai.lang.ChatModelStreamingBuilder;
import java.time.Duration;
import java.util.Map;

/**
 *
 */
public class LMStudioStreamingBuilder implements ChatModelStreamingBuilder {

    protected final OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder;

    public LMStudioStreamingBuilder() {
        builder = OpenAiStreamingChatModel.builder()
                .baseUrl("http://localhost:1234/v1/");
    }

    @Override
    public ChatModelStreamingBuilder baseUrl(final String baseUrl) {
        builder.baseUrl(baseUrl);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder customHeaders(final Map<String, String> customHeaders) {
        builder.customHeaders(customHeaders);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder apiKey(final String apiKey) {
        builder.apiKey(apiKey);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder modelName(final String modelName) {
        builder.modelName(modelName);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder temperature(final Double temperature) {
        builder.temperature(temperature);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder timeout(final Duration timeout) {
        builder.timeout(timeout);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder topP(final Double topP) {
        builder.topP(topP);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxOutputTokens(final Integer maxOutputTokens) {
        // NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder repeatPenalty(final Double repeatPenalty) {
        // NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder seed(final Integer seed) {
        builder.seed(seed);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxTokens(final Integer maxTokens) {
        builder.maxTokens(maxTokens);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxCompletionTokens(final Integer maxCompletionTokens) {
        builder.maxCompletionTokens(maxCompletionTokens);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder topK(final Integer topK) {
        // NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder presencePenalty(final Double presencePenalty) {
        builder.presencePenalty(presencePenalty);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder frequencyPenalty(final Double frequencyPenalty) {
        builder.frequencyPenalty(frequencyPenalty);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder organizationId(final String organizationId) {
        builder.organizationId(organizationId);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder logRequestsResponses(final boolean logRequests, final boolean logResponses) {
        builder.logRequests(logRequests)
                .logResponses(logResponses);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder includeCodeExecutionOutput(final boolean includeCodeExecutionOutput) {
        // NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder allowCodeExecution(final boolean allowCodeExecution) {
        // NOOP
        return this;
    }

    @Override
    public StreamingChatModel build() {
        return builder.build();
    }
}

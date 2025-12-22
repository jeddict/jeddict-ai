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
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import io.github.jeddict.ai.lang.ChatModelStreamingBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Francois Steyn
 */
public class MistralStreamingBuilder implements ChatModelStreamingBuilder {

    private final MistralAiStreamingChatModel.MistralAiStreamingChatModelBuilder builder;

    public MistralStreamingBuilder() {
        builder = MistralAiStreamingChatModel.builder();
    }

    @Override
    public ChatModelStreamingBuilder baseUrl(final String baseUrl) {
        builder.baseUrl(baseUrl);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder customHeaders(final Map<String, String> customHeaders) {
        //NOOP
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
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder repeatPenalty(final Double repeatPenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder seed(final Integer seed) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxTokens(final Integer maxTokens) {
        builder.maxTokens(maxTokens);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxCompletionTokens(final Integer maxCompletionTokens) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder topK(final Integer topK) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder presencePenalty(final Double presencePenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder frequencyPenalty(final Double frequencyPenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder organizationId(final String organizationId) {
        //NOOP
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
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder allowCodeExecution(final boolean allowCodeExecution) {
        //NOOP
        return this;
    }
    
    @Override
    public ChatModelStreamingBuilder listeners(List<ChatModelListener> listeners) {
        builder.listeners(listeners); return this;
    }

    @Override
    public StreamingChatModel build() {
        return builder.build();
    }
}

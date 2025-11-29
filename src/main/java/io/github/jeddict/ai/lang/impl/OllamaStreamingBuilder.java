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

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import io.github.jeddict.ai.lang.ChatModelStreamingBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 *
 * @author Francois Steyn
 */
public class OllamaStreamingBuilder implements ChatModelStreamingBuilder {

    private final OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder;

    public OllamaStreamingBuilder() {
        /**
         * Note: there is a known issue of LMStudio supporting HTTP 1.1 only,
         * while langchain4j uses HTTP 2.0 by default. The workaround is to make
         * sure to customize the HTTP client used by langchain4j to use HTTP 1.1
         */
        final JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
            .httpClientBuilder(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            );
        builder = OllamaStreamingChatModel.builder();
        builder.httpClientBuilder(jdkHttpClientBuilder);
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
        //NOOP
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
        builder.repeatPenalty(repeatPenalty);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder seed(final Integer seed) {
        builder.seed(seed);
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxTokens(final Integer maxTokens) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder maxCompletionTokens(final Integer maxCompletionTokens) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelStreamingBuilder topK(final Integer topK) {
        builder.topK(topK);
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
    public StreamingChatModel build() {
        return builder.build();
    }
}

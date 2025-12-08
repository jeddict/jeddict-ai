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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.jeddict.ai.lang.ChatModelBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 *
 * @author Francois Steyn
 */
public class LMStudioBuilder implements ChatModelBuilder {

    protected final OpenAiChatModel.OpenAiChatModelBuilder builder;

    public LMStudioBuilder() {
        /**
         * Note: there is a known issue of LMStudio supporting HTTP 1.1 only,
         * while langchain4j uses HTTP 2.0 by default. The workaround is to make
         * sure to customize the HTTP client used by langchain4j to use HTTP 1.1
         */
        final JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
            .httpClientBuilder(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            );

        builder = OpenAiChatModel.builder().baseUrl("http://localhost:1234/v1/");
        builder.httpClientBuilder(jdkHttpClientBuilder);
    }

    @Override
    public ChatModelBuilder baseUrl(final String baseUrl) {
        builder.baseUrl(baseUrl);
        return this;
    }

    @Override
    public ChatModelBuilder customHeaders(final Map<String, String> customHeaders) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder apiKey(final String apiKey) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder modelName(final String modelName) {
        builder.modelName(modelName);
        return this;
    }

    @Override
    public ChatModelBuilder temperature(final Double temperature) {
        builder.temperature(temperature);
        return this;
    }

    @Override
    public ChatModelBuilder timeout(final Duration timeout) {
        builder.timeout(timeout);
        return this;
    }

    @Override
    public ChatModelBuilder topP(final Double topP) {
        builder.topP(topP);
        return this;
    }

    @Override
    public ChatModelBuilder maxOutputTokens(final Integer maxOutputTokens) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder repeatPenalty(final Double repeatPenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder seed(final Integer seed) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder maxTokens(final Integer maxTokens) {
        builder.maxTokens(maxTokens);
        return this;
    }

    @Override
    public ChatModelBuilder maxCompletionTokens(final Integer maxCompletionTokens) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder topK(final Integer topK) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder presencePenalty(final Double presencePenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder frequencyPenalty(final Double frequencyPenalty) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder organizationId(final String organizationId) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder logRequestsResponses(final boolean logRequests, final boolean logResponses) {
        builder.logRequests(logRequests)
               .logResponses(logResponses);
        return this;
    }

    @Override
    public ChatModelBuilder includeCodeExecutionOutput(final boolean includeCodeExecutionOutput) {
        //NOOP
        return this;
    }

    @Override
    public ChatModelBuilder allowCodeExecution(final boolean allowCodeExecution) {
        //NOOP
        return this;
    }

    @Override
    public ChatModel build() {
        return builder.build();
    }

    @Override
    @Deprecated(since="3.3", forRemoval = true)
    /**
     * langchain4j does not support maxRetries at this level any more
     */
    public ChatModelBuilder maxRetries(Integer maxRetries) {
        // NOOP
        return this;
    }
}

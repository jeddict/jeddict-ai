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

import java.time.Duration;
import java.util.Map;

/**
 * Builder interface for configuring chat model parameters. This interface
 * provides methods to set various configuration options for chat models.
 *
 * @author Francois Steyn
 * @param <T> The type of chat model being built
 */
public interface ChatModelBaseBuilder<T> {

    /**
     * Sets the base URL for the chat model API.
     *
     * @param baseUrl The base URL of the API endpoint
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> baseUrl(final String baseUrl);

    /**
     * Sets the custom headers for accessing the service.
     *
     * @param customHeaders The custom headers for accessing the service
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> customHeaders(final Map<String, String> customHeaders);

    /**
     * Sets the API key for authentication.
     *
     * @param apiKey The API key for accessing the service
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> apiKey(final String apiKey);

    /**
     * Sets the name of the model to be used.
     *
     * @param modelName The name of the model
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> modelName(final String modelName);

    /**
     * Sets the temperature parameter for response generation.
     *
     * @param temperature Controls randomness in the output (0.0 to 1.0)
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> temperature(final Double temperature);

    /**
     * Sets the timeout duration for API calls.
     *
     * @param timeout The duration after which the request times out
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> timeout(final Duration timeout);

    /**
     * Sets the top P parameter for response generation.
     *
     * @param topP Controls diversity via nucleus sampling
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> topP(final Double topP);

    /**
     * Sets the maximum number of tokens in the output.
     *
     * @param maxOutputTokens The maximum number of output tokens
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> maxOutputTokens(final Integer maxOutputTokens);

    /**
     * Sets the repeat penalty for response generation.
     *
     * @param repeatPenalty The penalty for repeated content
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> repeatPenalty(final Double repeatPenalty);

    /**
     * Sets the random seed for reproducible results.
     *
     * @param seed The random seed value
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> seed(final Integer seed);

    /**
     * Sets the maximum total tokens (input + output).
     *
     * @param maxTokens The maximum number of total tokens
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> maxTokens(final Integer maxTokens);

    /**
     * Sets the maximum number of tokens for completion.
     *
     * @param maxCompletionTokens The maximum completion tokens
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> maxCompletionTokens(final Integer maxCompletionTokens);

    /**
     * Sets the top K parameter for response generation.
     *
     * @param topK The number of highest probability vocabulary tokens
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> topK(final Integer topK);

    /**
     * Sets the presence penalty for response generation.
     *
     * @param presencePenalty The penalty for token presence
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> presencePenalty(final Double presencePenalty);

    /**
     * Sets the frequency penalty for response generation.
     *
     * @param frequencyPenalty The penalty for token frequency
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> frequencyPenalty(final Double frequencyPenalty);

    /**
     * Sets the organization ID for API requests.
     *
     * @param organizationId The organization identifier
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> organizationId(final String organizationId);

    /**
     * Configures logging for requests and responses.
     *
     * @param logRequests Whether to log requests
     * @param logResponses Whether to log responses
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> logRequestsResponses(final boolean logRequests, final boolean logResponses);

    /**
     * Sets whether to include code execution output.
     *
     * @param includeCodeExecutionOutput Whether to include code execution output
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> includeCodeExecutionOutput(final boolean includeCodeExecutionOutput);

    /**
     * Sets whether to allow code execution.
     *
     * @param allowCodeExecution Whether to allow code execution
     * @return The builder instance
     */
    ChatModelBaseBuilder<T> allowCodeExecution(final boolean allowCodeExecution);

    /**
     * Builds and returns the configured chat model instance.
     *
     * @return The configured chat model instance
     */
    T build();
}

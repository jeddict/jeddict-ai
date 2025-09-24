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
import java.time.Duration;
import java.util.Map;

/**
 * A specialized builder interface for constructing ChatLanguageModel instances.
 * This interface extends ChatModelBaseBuilder to provide specific building
 * capabilities for chat-based language models. It inherits all configuration
 * methods from the base builder while specifically targeting ChatLanguageModel
 * as the build output type.
 *
 * @author Francois Steyn
 * @see ChatModel
 * @see ChatModelBaseBuilder
 */
public interface ChatModelBuilder extends ChatModelBaseBuilder<ChatModel> {

    @Override
    ChatModelBuilder baseUrl(final String baseUrl);

    @Override
    ChatModelBuilder customHeaders(final Map<String, String> customHeaders);

    @Override
    ChatModelBuilder apiKey(final String apiKey);

    @Override
    ChatModelBuilder modelName(final String modelName);

    @Override
    ChatModelBuilder temperature(final Double temperature);

    @Override
    ChatModelBuilder timeout(final Duration timeout);

    @Override
    ChatModelBuilder topP(final Double topP);

    @Override
    ChatModelBuilder maxOutputTokens(final Integer maxOutputTokens);

    @Override
    ChatModelBuilder repeatPenalty(final Double repeatPenalty);

    @Override
    ChatModelBuilder seed(final Integer seed);

    @Override
    ChatModelBuilder maxTokens(final Integer maxTokens);

    @Override
    ChatModelBuilder maxCompletionTokens(final Integer maxCompletionTokens);

    @Override
    ChatModelBuilder topK(final Integer topK);

    @Override
    ChatModelBuilder presencePenalty(final Double presencePenalty);

    @Override
    ChatModelBuilder frequencyPenalty(final Double frequencyPenalty);

    @Override
    ChatModelBuilder organizationId(final String organizationId);

    @Override
    ChatModelBuilder logRequestsResponses(final boolean logRequests, final boolean logResponses);

    @Override
    ChatModelBuilder includeCodeExecutionOutput(final boolean includeCodeExecutionOutput);

    @Override
    ChatModelBuilder allowCodeExecution(final boolean allowCodeExecution);

    @Override
    ChatModel build();

    ChatModelBuilder maxRetries(final Integer maxRetries);

}

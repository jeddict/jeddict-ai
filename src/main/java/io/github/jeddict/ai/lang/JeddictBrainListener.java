/*
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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

public interface JeddictBrainListener {

    //
    // TODO: the below is based on langchat4j AiServiceStarted event, which provides
    //       the messages; use the model event listened by ChatModelListener can be
    //       useful as well becase it intercepts the full ChatRequest
    //
    /**
     * On the start of a new chat
     *
     * @param system
     * @param userMessage
     */
    default void onChatStarted(final SystemMessage system, final UserMessage userMessage) {
    }

    /**
     * On sending a new request to the LLLM
     * @param request
     */
    default void onRequest(final ChatRequest request) {
    }

    /**
     * On response received from the LLM
     *
     * @param request
     * @param response
     */
    default void onResponse(final ChatRequest request, final ChatResponse response) {
    }

    /**
     * On the completion of a chat
     *
     * @param response
     */
    default void onChatCompleted(final ChatResponse response) {
    }

    /**
     * On the execution of a tool (once done)
     *
     * @param request
     * @param result
     */
    default void onToolExecuted(final ToolExecutionRequest request, final String result) {
    }

    /**
     * On any error
     *
     * @param error
     */
    default void onError(final Throwable error) {
    }

    /**
     * On any progress from a tool (or any other emitter)
     *
     * @param progress
     */
    default void onProgress(final String progress) {
    }
}

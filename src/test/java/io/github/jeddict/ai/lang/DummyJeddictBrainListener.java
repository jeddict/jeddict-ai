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
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DummyJeddictBrainListener implements JeddictBrainListener {

    public final List collector = new ArrayList();

    @Override
    public void onChatStarted(final SystemMessage system, final UserMessage user) {
        collector.add(new Object[] { system, user });
    }

    @Override
    public void onRequest(final ChatRequest request) {
        collector.add(request);
    }

    @Override
    public void onResponse(final ChatRequest request, final ChatResponse response) {
        collector.add(new Object[] {request, response});
    }

    @Override
    public void onChatCompleted(final ChatResponse result) {
        collector.add(result);
    }

    @Override
    public void onToolExecuted(final ToolExecutionRequest request, final String result) {
        collector.add(new Object[] {request, result});
    }

    @Override
    public void onError(final Throwable error) {
        collector.add(error);
    }

    @Override
    public void onProgress(final String progress) {
        collector.add(progress);
    }
}

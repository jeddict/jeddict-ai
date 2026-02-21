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
package io.github.jeddict.ai.test;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

import java.util.Optional;

public class DummyChatModelListener implements ChatModelListener {
    public Optional<ChatModelRequestContext> lastRequestContext = Optional.empty();
    public Optional<ChatModelResponseContext> lastResponseContext = Optional.empty();

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        this.lastRequestContext = Optional.of(requestContext);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        this.lastResponseContext = Optional.of(responseContext);
    }
}

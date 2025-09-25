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

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.Optional;

public class DummyStreamHandler implements StreamingChatResponseHandler {

    private final StringBuilder content = new StringBuilder();

    public Optional<ChatResponse> response = Optional.empty();
    public Optional<Throwable> error = Optional.empty();

    @Override
    public void onPartialResponse(String partialResponse) {
        content.append(partialResponse);
    }

    @Override
    public void onError(Throwable error) {
        this.error = Optional.of(error);
    }

    @Override
    public String toString() {
        return content.toString();
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        this.response = Optional.of(response);
    }
}
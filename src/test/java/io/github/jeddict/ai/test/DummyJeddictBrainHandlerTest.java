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
package io.github.jeddict.ai.test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

public class DummyJeddictBrainHandlerTest {

    @Test
    public void onPartialResponse_accumulates_content() {
        final DummyJeddictBrainHandler H = new DummyJeddictBrainHandler();

        H.onPartialResponse("Hello, ");
        H.onPartialResponse("world!");

        then(H.toString()).isEqualTo("Hello, world!");
        then(H.response).isEmpty();
        then(H.error).isEmpty();
    }

    @Test
    public void onComplete_records_the_message() {
        final AiMessage M = AiMessage.from("hi");
        final DummyJeddictBrainHandler H = new DummyJeddictBrainHandler();
        final ChatResponse R = ChatResponse.builder().aiMessage(M).build();

        H.onCompleteResponse(R);
        then(H.response).hasValue(R);
        then(H.error).isEmpty();
    }

    @Test
    public void onError_records_the_exception() {
        final DummyJeddictBrainHandler H = new DummyJeddictBrainHandler();
        final Throwable T = new Throwable();

        H.onPartialResponse("Hello, ");
        H.onPartialResponse("World");
        H.onError(T);

        then(H.toString()).isEqualTo("Hello, World");
        then(H.error).hasValue(T);
        then(H.response).isEmpty();
    }

}

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
package io.github.jeddict.ai.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import java.util.List;

/**
 *
 * @author Gaurav Gupta
 */
public interface Assistant {

    // simple one-shot request → response
    String chat(String input);

    // full conversation style (structured)
     Response<AiMessage> chat(List<ChatMessage> messages);

    // streaming (one-shot)
    TokenStream stream(String input);

    // streaming (structured messages)
    TokenStream stream(List<ChatMessage> messages);
}

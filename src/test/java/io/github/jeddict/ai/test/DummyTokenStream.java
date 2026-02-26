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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import java.util.List;
import java.util.function.Consumer;
import static ste.lloop.Loop.on;

/**
 * This is a dummy implementation of a TokenStream. Please note it does not try
 * in any way to create tokens like a language models does. It just breaks a
 * string in words.
 */
public class DummyTokenStream implements TokenStream {

    public final String response;
    public final String[] tokens;

    private int index = 0;

    public DummyTokenStream(final String response) {
        this.response = response;
        //
        // This regex uses lookahead (?=\\W) and lookbehind (?<=\\W) to split
        // before and after non-word characters while keeping them.
        //
        this.tokens = response.split("(?<=\\W)|(?=\\W)"); // (...) makes return the separators too
    }

    private Consumer<String> partialResponse = null;
    private Consumer<ChatResponse> completeResponse = null;

    @Override
    public TokenStream onPartialResponse(final Consumer<String> c) {
        this.partialResponse = c; return this;
    }

    @Override
    public TokenStream onToolExecuted(final Consumer<ToolExecution> c) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public TokenStream onCompleteResponse(final Consumer<ChatResponse> c) {
        this.completeResponse = c; return this;
    }

    @Override
    public TokenStream onError(final Consumer<Throwable> c) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public TokenStream ignoreErrors() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void start() {
        index = 0;
        on(tokens).loop((element) ->{
            partialResponse.accept(element);
        });
        if (completeResponse != null) {
            completeResponse.accept(
                ChatResponse.builder().aiMessage(AiMessage.from(response)).build()
            );
        }
    }

    @Override
    public TokenStream onRetrieved(Consumer<List<Content>> cnsmr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}

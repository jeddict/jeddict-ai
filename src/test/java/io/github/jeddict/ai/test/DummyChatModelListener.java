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

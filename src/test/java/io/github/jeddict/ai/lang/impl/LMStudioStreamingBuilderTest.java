package io.github.jeddict.ai.lang.impl;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.BDDAssertions.then;

public class LMStudioStreamingBuilderTest {
    @Test
    public void build_shouldReturnOpenAiStreamingChatModel() {
        LMStudioStreamingBuilder builder = new LMStudioStreamingBuilder();
        StreamingChatModel model = builder
                .baseUrl("http://localhost:9999/v1/")
                .modelName("custom-model")
                .build();

        then(model).isInstanceOf(OpenAiStreamingChatModel.class);
    }
}

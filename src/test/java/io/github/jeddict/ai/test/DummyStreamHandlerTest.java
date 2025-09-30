
package io.github.jeddict.ai.test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

public class DummyStreamHandlerTest {

    @Test
    public void onPartialResponse_accumulates_content() {
        final DummyStreamHandler H = new DummyStreamHandler();

        H.onPartialResponse("Hello, ");
        H.onPartialResponse("world!");

        then(H.toString()).isEqualTo("Hello, world!");
        then(H.response).isEmpty();
        then(H.error).isEmpty();
    }

    @Test
    public void onComplete_records_the_message() {
        final AiMessage M = AiMessage.from("hi");
        final DummyStreamHandler H = new DummyStreamHandler();
        final ChatResponse R = ChatResponse.builder().aiMessage(M).build();

        H.onCompleteResponse(R);
        then(H.response).hasValue(R);
        then(H.error).isEmpty();
    }

    @Test
    public void onError_records_the_exception() {
        final DummyStreamHandler H = new DummyStreamHandler();
        final Throwable T = new Throwable();

        H.onPartialResponse("Hello, ");
        H.onPartialResponse("World");
        H.onError(T);

        then(H.toString()).isEqualTo("Hello, World");
        then(H.error).hasValue(T);
        then(H.response).isEmpty();
    }

}

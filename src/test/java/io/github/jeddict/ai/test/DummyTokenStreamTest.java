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
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DummyTokenStreamTest {

    @Test
    public void stream_from_a_string() {
        final String message = "Mirror, mirror on the wall: who’s the fairest of them all?";
        final DummyTokenStream s = new DummyTokenStream(message);

        final List<String> words = new ArrayList();

        s.onPartialResponse((w) -> words.add(w)).start();

        then(words).containsExactly(
            "Mirror", ",", " ", "mirror", " ", "on", " ", "the", " ", "wall",
            ":", " ", "who", "’", "s", " ", "the", " ", "fairest", " ",
            "of", " ", "them", " ", "all", "?"
        );

        final ChatResponse[] response = new ChatResponse[1];
        s.onCompleteResponse((r) -> response[0] = r).start();
        then(response[0].aiMessage().text()).isEqualTo(message);
    }
}

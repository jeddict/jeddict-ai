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
package io.github.jeddict.ai.models.registry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Gaurav Gupta
 */
class GenAIProviderTest {

    @Test
    void fromModelId_resolvesProvidersCorrectly() {
        assertEquals(GenAIProvider.OPEN_AI,
                GenAIProvider.fromModelId("openai/gpt-5"));

        assertEquals(GenAIProvider.GOOGLE,
                GenAIProvider.fromModelId("google/gemini-2.5-pro"));

        assertEquals(GenAIProvider.ANTHROPIC,
                GenAIProvider.fromModelId("claude-3-opus"));

        assertEquals(GenAIProvider.MISTRAL,
                GenAIProvider.fromModelId("mistralai/mistral-large"));

        assertEquals(GenAIProvider.DEEPINFRA,
                GenAIProvider.fromModelId("meta-llama/Llama-3-70B"));

        assertEquals(GenAIProvider.OTHER,
                GenAIProvider.fromModelId(null));
    }
}

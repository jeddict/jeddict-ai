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

import static org.assertj.core.api.BDDAssertions.then;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Gaurav Gupta
 */
class GenAIModelTest {

    @Test
    void get_models_returns_cached_models_when_cache_is_valid() throws Exception {
        Map<String, GenAIModel> fakeCache = Map.of(
                "gpt-5-nano",
                new GenAIModel(
                        GenAIProvider.OPEN_AI,
                        "gpt-5-nano",
                        "",
                        0,
                        0
                )
        );

        Field cacheField = GenAIModelRegistry.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, fakeCache);

        Field lastLoaded = GenAIModelRegistry.class.getDeclaredField("lastLoaded");
        lastLoaded.setAccessible(true);
        lastLoaded.setLong(null, System.currentTimeMillis());

        Map<String, GenAIModel> result = GenAIModelRegistry.getModels();

        then(result).hasSize(1);
        then(result).containsKey("gpt-5-nano");
    }

    @Test
    void to_string_returns_model_name_only() {
        GenAIModel model = new GenAIModel(
                GenAIProvider.OPEN_AI,
                "openai/gpt-5",
                "desc",
                0,
                0
        );

        then(model.toString()).isEqualTo("gpt-5");
    }

    @Test
    void constructor_normalizes_model_name() {
        GenAIModel model = new GenAIModel(
                GenAIProvider.OPEN_AI,
                "openai/gpt-5-nano",
                "desc",
                1.0,
                2.0
        );

        then(model.getName()).isEqualTo("gpt-5-nano");
    }
}

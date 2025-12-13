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
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Gaurav Gupta
 */
class OpenRouterModelParserTest {

    @Test
    void parse_validJson_createsModelsWithCorrectPricingAndProvider() {
        String json = """
        {
          "data": [
            {
              "id": "openai/gpt-5-nano",
              "description": "Fast model",
              "pricing": {       
                      "prompt": 0.00005,
                      "completion": 0.0004
              }
            }
          ]
        }
        """;

        Map<String, GenAIModel> models = OpenRouterModelParser.parse(json);

        assertEquals(1, models.size());

        GenAIModel model = models.get("openai/gpt-5-nano");
        assertNotNull(model);

        assertEquals("gpt-5-nano", model.getName());
        assertEquals(GenAIProvider.OPEN_AI, model.getProvider());
        assertEquals(50.0, model.getInputPrice(), 0.0001);
        assertEquals(400.0, model.getOutputPrice(), 0.0001);

    }

    @Test
    void parse_modelWithoutPricing_defaultsPricesToZero() {
        String json = """
        {
          "data": [
            {
              "id": "openai/gpt-5-mini",
              "description": "No pricing info"
            }
          ]
        }
        """;

        Map<String, GenAIModel> models = OpenRouterModelParser.parse(json);
        GenAIModel model = models.get("openai/gpt-5-mini");

        assertNotNull(model);
        assertEquals(0.0, model.getInputPrice());
        assertEquals(0.0, model.getOutputPrice());
    }
}

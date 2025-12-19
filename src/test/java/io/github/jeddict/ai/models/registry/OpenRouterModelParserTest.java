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

import java.util.Map;
import static org.assertj.core.api.BDDAssertions.within;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Gaurav Gupta
 */
class OpenRouterModelParserTest {

    @Test
    void parse_valid_json_creates_models_with_correct_pricing_and_provider() {
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

        then(models).hasSize(1);

        GenAIModel model = models.get("openai/gpt-5-nano");
        then(model).isNotNull();

        then(model.getName()).isEqualTo("gpt-5-nano");
        then(model.getProvider()).isEqualTo(GenAIProvider.OPEN_AI);
        then(model.getInputPrice()).isCloseTo(50.0, within(0.0001));
        then(model.getOutputPrice()).isCloseTo(400.0, within(0.0001));
    }

    @Test
    void parse_model_without_pricing_defaults_prices_to_zero() {
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

        then(model).isNotNull();
        then(model.getInputPrice()).isZero();
        then(model.getOutputPrice()).isZero();
    }
}

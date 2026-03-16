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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author Gaurav Gupta
 */
class OpenRouterModelParser {

    /**
     * Parses model data from the given JSON string.
     *
     * @param json JSON string containing model data
     * @return map of model id to {@link GenAIModel}
     */
    Map<String, GenAIModel> parse(String json) {
        return parseRoot(new JSONObject(json));
    }

    /**
     * Parses model data directly from an input stream, avoiding the need to
     * buffer the entire response as a string first.
     *
     * @param is input stream with JSON content
     * @return map of model id to {@link GenAIModel}
     */
    Map<String, GenAIModel> parse(InputStream is) {
        return parseRoot(new JSONObject(new JSONTokener(is)));
    }

    private Map<String, GenAIModel> parseRoot(JSONObject root) {
        Map<String, GenAIModel> models = new HashMap<>();

        JSONArray data = root.getJSONArray("data");

        for (int i = 0; i < data.length(); i++) {
            JSONObject m = data.getJSONObject(i);

            String id = m.getString("id");
            String desc = m.optString("description", "");

            JSONObject pricing = m.optJSONObject("pricing");
            double input = pricing != null
                    ? pricing.optDouble("prompt", 0.0) * 1_000_000
                    : 0.0;
            double output = pricing != null
                    ? pricing.optDouble("completion", 0.0) * 1_000_000
                    : 0.0;

            GenAIProvider provider = GenAIProvider.fromModelId(id);

            models.put(
                    id,
                    new GenAIModel(provider, id, desc, input, output)
            );
        }
        return models;
    }
}

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author Shiwani Gupta
 * @author Gaurav Gupta
 */
public class GenAIModelRegistry {

    private static final String API_URL = "https://openrouter.ai/api/v1";

    private static final long CACHE_TTL_MS = Duration.ofMinutes(30).toMillis();

    private static Map<String, GenAIModel> CACHE = new HashMap<>();
    private static long lastLoaded = 0;

    /** Package-private to allow test subclasses to override {@link #getAPIUrl()}. */
    static GenAIModelRegistry REGISTRY_INSTANCE = new GenAIModelRegistry();

    public String getAPIUrl() {
        return API_URL;
    }

    public List<String> fetchModelNames(String apiUrl) {
        List<String> modelNames = new ArrayList<>();

        try {
            URLConnection connection = new URL(apiUrl + "/models").openConnection();
            connection.setRequestProperty("Accept", "application/json");

            if (connection instanceof HttpURLConnection http) {
                http.setRequestMethod("GET");
                int responseCode = http.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    System.err.println("GET request failed. Response Code: " + responseCode);
                    return modelNames;
                }
            }

            try (InputStream is = connection.getInputStream()) {
                JSONObject jsonResponse = new JSONObject(new JSONTokener(is));
                JSONArray models = jsonResponse.getJSONArray("data");

                // Sort the list by "created" in descending order
                List<JSONObject> modelList = new ArrayList<>();
                for (int i = 0; i < models.length(); i++) {
                    modelList.add(models.getJSONObject(i));
                }
                modelList.sort((obj1, obj2) -> Long.compare(obj2.getLong("created"), obj1.getLong("created")));

                for (JSONObject model : modelList) {
                    modelNames.add(model.getString("id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return modelNames;
    }

    /**
     * Fetches model info and creates a map as in GenAIModel.MODELS
     *
     * @param apiUrl GPT4All API url
     * @return Map of model name to GenAIModel
     */
    public LinkedHashMap<String, GenAIModel> fetchGenAIModels(String apiUrl) {
        LinkedHashMap<String, GenAIModel> modelsMap = new LinkedHashMap<>();
        try {
            URLConnection connection = new URL(apiUrl + "/models").openConnection();
            connection.setRequestProperty("Accept", "application/json");

            if (connection instanceof HttpURLConnection http) {
                http.setRequestMethod("GET");
                int responseCode = http.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    System.err.println("GET request failed. Response Code: " + responseCode);
                    return modelsMap;
                }
            }

            try (InputStream is = connection.getInputStream()) {
                JSONObject jsonResponse = new JSONObject(new JSONTokener(is));
                JSONArray models = jsonResponse.getJSONArray("data");

                for (int i = 0; i < models.length(); i++) {
                    JSONObject model = models.getJSONObject(i);
                    String name = model.getString("id");
                    String description = model.has("description") ? model.getString("description") : "";

                    double inputPrice = 0.0;
                    double outputPrice = 0.0;

                    if (model.has("pricing")) {
                        JSONObject pricing = model.getJSONObject("pricing");
                        if (pricing.has("prompt")) {
                            inputPrice = pricing.optDouble("prompt", 0.0);
                        }
                        if (pricing.has("completion")) {
                            outputPrice = pricing.optDouble("completion", 0.0);
                        }
                    }

                    GenAIModel genAIModel = new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, name, description, inputPrice, outputPrice);
                    modelsMap.put(name, genAIModel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelsMap;
    }

    public static synchronized Map<String, GenAIModel> getModels() {
        if (isCacheValid()) {
            return CACHE;
        }

        try {
            Map<String, GenAIModel> loaded = loadFromHttp();
            CACHE = loaded;
            lastLoaded = System.currentTimeMillis();
            return CACHE;
        } catch (Exception ex) {
            // Fallback: keep old cache or empty map
            return CACHE.isEmpty()
                    ? Collections.emptyMap()
                    : CACHE;
        }
    }

    public static GenAIModel findByName(String name) {
        return getModels().get(name);
    }

    private static boolean isCacheValid() {
        return !CACHE.isEmpty()
                && (System.currentTimeMillis() - lastLoaded) < CACHE_TTL_MS;
    }

    // --------------------------------------------
    // HTTP + JSON parsing (lightweight)
    // --------------------------------------------
    private static Map<String, GenAIModel> loadFromHttp() throws Exception {
        HttpURLConnection conn
                = (HttpURLConnection) new URL(REGISTRY_INSTANCE.getAPIUrl() + "/models").openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("Failed to load models");
        }

        try (InputStream is = conn.getInputStream()) {
            return new OpenRouterModelParser().parse(is);
        }
    }
}

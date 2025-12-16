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
package io.github.jeddict.ai.models;

import io.github.jeddict.ai.settings.GenAIModel;
import io.github.jeddict.ai.settings.GenAIProvider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Shiwani Gupta
 */
public class OpenAIModelFetcher {

    private static final String API_URL = "https://openrouter.ai/api/v1";

    public String getAPIUrl() {
        return API_URL;
    }

    public List<String> fetchModelNames(String apiUrl) {
        List<String> modelNames = new ArrayList<>();

        try {
            URL url = new URL(apiUrl + "/models");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray models = jsonResponse.getJSONArray("data");
                
                // Converti il JSONArray in una lista per ordinamento
                List<JSONObject> modelList = new ArrayList<>();
                for (int i = 0; i < models.length(); i++) {
                    modelList.add(models.getJSONObject(i));
                }

                // Ordina la lista per il campo "created" in ordine decrescente
                modelList.sort((obj1, obj2) -> {
                    long created1 = obj1.getLong("created");
                    long created2 = obj2.getLong("created");
                    return Long.compare(created2, created1); // Ordine decrescente
                });
                
                for (int i = 0; i < modelList.size(); i++) {
                    JSONObject model = modelList.get(i);
                    String name = model.getString("id");  // Assuming 'id' holds the model name
                    modelNames.add(name);
                }
            } else {
                System.err.println("GET request failed. Response Code: " + responseCode);
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
            URL url = new URL(apiUrl + "/models");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray models = jsonResponse.getJSONArray("data");
                
                // Converti il JSONArray in una lista per ordinamento
                List<JSONObject> modelList = new ArrayList<>();
                for (int i = 0; i < models.length(); i++) {
                    modelList.add(models.getJSONObject(i));
                }

                // Ordina la lista per il campo "created" in ordine decrescente
                /*
                modelList.sort((obj1, obj2) -> {
                    long created1 = obj1.getLong("created");
                    long created2 = obj2.getLong("created");
                    return Long.compare(created2, created1); // Ordine decrescente
                });
                */
                /*
                JSONArray sortedModels = new JSONArray();
                StreamSupport.stream(models.spliterator(), false)
                    .map(obj -> (JSONObject) obj)
                    .sorted((obj1, obj2) -> Long.compare(obj2.getLong("created"), obj1.getLong("created")))
                    .forEach(sortedModels::put);
                */
                for (int i = 0; i < modelList.size(); i++) {
                    JSONObject model = modelList.get(i);
                    String name = model.getString("id");
                    String description = model.has("description") ? model.getString("description") : "";
                    // Ak máš v API info o cene, môžeš ich tiež vytiahnuť, inak nastav na 0:
                    
                    double inputPrice = 0.0;
                    double outputPrice = 0.0;
                    
                    if(model.has("pricing")) {
                        JSONObject pricing = model.getJSONObject("pricing");
                        if(pricing.has("prompt"))
                            inputPrice = ((pricing.optDouble("prompt", 0.0)));
                        if(pricing.has("completion"))
                            outputPrice = ((pricing.optDouble("completion", 0.0)));
                    }

                    // Pozor: musíš pridať GPT4ALL do tvojho GenAIProvider enum, napr.:
                    // public enum GenAIProvider { OPEN_AI, GOOGLE, ..., GPT4ALL }
                    GenAIModel genAIModel = new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, name, description, inputPrice, outputPrice);
                    modelsMap.put(name, genAIModel);
                }
            } else {
                System.err.println("GET request failed. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelsMap;
    }

    public static void main(String[] args) {
        OpenAIModelFetcher fetcher = new OpenAIModelFetcher();
        List<String> names = fetcher.fetchModelNames(API_URL);
        System.out.println("Model Names: " + names);
    }
}

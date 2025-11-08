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

package io.github.jeddict.ai.util;

import io.github.jeddict.ai.lang.Snippet;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class JSONUtil {

    public static List<Snippet> jsonToSnippets(String jsonResponse) {
        if (jsonResponse == null) {
            return Collections.EMPTY_LIST;
        }
        List<Snippet> snippets = new ArrayList<>();

        JSONArray jsonArray;

        if (jsonResponse.contains("```json")) {
            int index = jsonResponse.indexOf("```json") + 7;
            jsonResponse = jsonResponse.substring(index, jsonResponse.indexOf("```", index)).trim();
        } else {
            jsonResponse = removeCodeBlockMarkers(jsonResponse);
        }
        try {
            // Parse the JSON response
            jsonArray = new JSONArray(jsonResponse);
        } catch (org.json.JSONException jsone) {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            jsonArray = new JSONArray();
            jsonArray.put(jsonObject);
        }

        // Loop through each element in the JSON array
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            List<String> importsList = new ArrayList<>();
            if (jsonObject.has("imports")) {
                // Extract the "imports" array
                JSONArray importsJsonArray = jsonObject.getJSONArray("imports");
                for (int j = 0; j < importsJsonArray.length(); j++) {
                    importsList.add(importsJsonArray.getString(j));
                }
            }

            // Extract the "snippet" field
            String snippet = jsonObject.getString("snippet");
            if (jsonObject.has("description")) {
                String descripion = jsonObject.getString("description");
                Snippet snippetObj = new Snippet(snippet, descripion, importsList);
                snippets.add(snippetObj);
            } else {
                Snippet snippetObj = new Snippet(snippet, importsList);
                snippets.add(snippetObj);
            }
        }

        return snippets;
    }

    public static List<String> jsonToList(String json) {
        List<String> variableNames = new ArrayList<>();
        try {
            // Use JSONArray to parse the JSON array string
            JSONArray jsonArray = new JSONArray(removeCodeBlockMarkers(json));
            boolean split = false;
            int docCount = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                variableNames.add(jsonArray.getString(i));
                String line = jsonArray.getString(i).trim();
                if (line.startsWith("}")) {
                    split = true;
                }
                if (line.trim().startsWith("*")) {
                    docCount++;
                }
            }
            if (split || jsonArray.length() - 1 == docCount) {
                return Collections.singletonList(String.join("\n", variableNames));
            }
        } catch (Exception e) {
            return jsonToListWithSplit(removeCodeBlockMarkers(json));
        }
        return variableNames;
    }

    public static List<String> jsonToListWithSplit(String json) {
        List<String> variableNames = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return variableNames;
        }

        json = removeCodeBlockMarkers(json).trim();
        String newjson = json;
        if (json.startsWith("[") && json.endsWith("]")) {
            newjson = json.substring(1, json.length() - 1).trim();
        }
        // Remove square brackets and split by new lines
        String[] lines = newjson.split("\\n");

        if (lines.length > 1) {
            for (String line : lines) {
                // Trim each line and add to the list if it's not empty
                line = line.trim();
                if (line.startsWith("\"")) {
                    if (line.endsWith("\"")) {
                        line = line.substring(1, line.length() - 1).trim();
                    } else if (line.endsWith("\",")) {
                        line = line.substring(1, line.length() - 2).trim();
                    }
                }
                if (!line.isEmpty()) {
                    variableNames.add(line);
                }
            }
        } else {
            //
            // TODO: this is a circular dependendency that smells...
            //
            variableNames = jsonToList(json);
        }

        return variableNames;
    }
}

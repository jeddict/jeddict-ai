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
package io.github.jeddict.ai.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Gaurav Gupta
 */
public class FilePreferences {

    private final Path PREFS_PATH;

    private JSONObject data;

    public FilePreferences() {
        PREFS_PATH = Paths.get(System.getProperty("user.home"), "jeddict.json");
        load();
    }

    private void load() {
        try {
            if (Files.exists(PREFS_PATH)) {
                String content = Files.readString(PREFS_PATH);
                data = new JSONObject(content);
            } else {
                data = new JSONObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load preferences", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(PREFS_PATH.getParent());
            Files.writeString(PREFS_PATH, data.toString(2));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save preferences", e);
        }
    }

    public void remove(String key) {
        data.remove(key);
        save();
    }

    /**
     * Export the entire preferences (including project metadata) to the
     * specified JSON file.
     *
     * @param filePath path to the JSON file to write
     * @throws IOException if there is an IO error during export
     */
    public void exportPreferences(String filePath) throws IOException {
        Path exportPath = Paths.get(filePath);
        Files.createDirectories(exportPath.getParent());
        Files.writeString(exportPath, data.toString(2));
    }

    /**
     * Import preferences from the specified JSON file. This will overwrite
     * existing preferences with imported values.
     *
     * @param filePath path to the JSON file to read
     * @throws IOException if there is an IO error during import
     */
    public void importPreferences(String filePath) throws IOException {
        Path importPath = Paths.get(filePath);
        if (!Files.exists(importPath)) {
            throw new IOException("Import file does not exist: " + filePath);
        }
        String content = Files.readString(importPath);
        JSONObject importedData = new JSONObject(content);

        // Overwrite the current data with imported data
        this.data = importedData;
        save();
    }

    public String get(String key, String def) {
        return data.optString(key, def);
    }

    public void put(String key, String value) {
        data.put(key, value);
        save();
    }

    public boolean getBoolean(String key, boolean def) {
        return data.optBoolean(key, def);
    }

    public void putBoolean(String key, boolean value) {
        data.put(key, value);
        save();
    }

// new int methods
    public int getInt(String key, int def) {
        return data.optInt(key, def);
    }

    public void putInt(String key, int value) {
        data.put(key, value);
        save();
    }

// new double methods
    public double getDouble(String key, double def) {
        return data.optDouble(key, def);
    }

    public void putDouble(String key, double value) {
        data.put(key, value);
        save();
    }

    public void putChild(String nodeKey, String key, String value) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node == null) {
            node = new JSONObject();
            data.put(nodeKey, node);
        }
        node.put(key, value);
        save();
    }

    public String getChild(String nodeKey, String key, String def) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node != null) {
            return node.optString(key, def);
        }
        return def;
    }

     public JSONObject getChild(String nodeKey) {
        JSONObject node = data.optJSONObject(nodeKey);
        if(node == null) {
            node = new JSONObject();
            data.put(nodeKey, node);
        }
        return node;
    }

    public void setChild(String nodeKey, JSONObject metadata) {
        data.put(nodeKey, metadata);
        save();
    }

     public JSONArray getChildArray(String nodeKey, List<String> defaultValues) {
        JSONArray node = data.optJSONArray(nodeKey);
        if(node == null) {
            node = new JSONArray();
            for (String value : defaultValues) {
                node.put(value);
            }
            data.put(nodeKey, node);
        }
        return node;
    }

    public List<String> getChildList(String nodeKey, List<String> defaultValues) {
        return jsonArrayToList(getChildArray(nodeKey, defaultValues));
    }

    protected List<String> jsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }

    public void putChildBoolean(String nodeKey, String key, boolean value) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node == null) {
            node = new JSONObject();
            data.put(nodeKey, node);
        }
        node.put(key, value);
        save();
    }

    public boolean getChildBoolean(String nodeKey, String key, boolean def) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node != null) {
            return node.optBoolean(key, def);
        }
        return def;
    }

    public void putChildInt(String nodeKey, String key, int value) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node == null) {
            node = new JSONObject();
            data.put(nodeKey, node);
        }
        node.put(key, value);
        save();
    }

    public int getChildInt(String nodeKey, String key, int def) {
        JSONObject node = data.optJSONObject(nodeKey);
        if (node != null) {
            return node.optInt(key, def);
        }
        return def;
    }


}

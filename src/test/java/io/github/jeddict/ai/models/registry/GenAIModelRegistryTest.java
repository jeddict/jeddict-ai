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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GenAIModelRegistry}, covering both reading and caching
 * functionality. Uses file:// URLs to mock HTTP requests without external
 * dependencies.
 *
 * @author Gaurav Gupta
 */
class GenAIModelRegistryTest {

    private static final String MODELS_FILE = "/mocks/registry/models";

    /** Reset the static cache before each test to ensure isolation. */
    @BeforeEach
    void resetCache() throws Exception {
        Field cacheField = GenAIModelRegistry.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, new HashMap<>());

        Field lastLoaded = GenAIModelRegistry.class.getDeclaredField("lastLoaded");
        lastLoaded.setAccessible(true);
        lastLoaded.setLong(null, 0L);
    }

    /**
     * Returns a file:// base URL such that appending "/models" points to the
     * test JSON fixture file.
     */
    private String fileUrl() {
        String fullUrl = GenAIModelRegistryTest.class.getResource(MODELS_FILE).toString();
        // Strip the trailing "/models" filename so the registry can append it
        return fullUrl.substring(0, fullUrl.lastIndexOf("/models"));
    }

    @Test
    void fetch_model_names_returns_models_sorted_by_created_descending() {
        GenAIModelRegistry registry = new GenAIModelRegistry();
        java.util.List<String> names = registry.fetchModelNames(fileUrl());

        // models.json has 3 entries; sorted by created desc:
        // openai/gpt-5-nano (created=1700000002) first
        then(names).hasSize(3);
        then(names.get(0)).isEqualTo("openai/gpt-5-nano");
        then(names.get(1)).isEqualTo("google/gemini-2-flash");
        then(names.get(2)).isEqualTo("anthropic/claude-opus-4");
    }

    @Test
    void fetch_gen_ai_models_returns_map_with_correct_entries() {
        GenAIModelRegistry registry = new GenAIModelRegistry();
        Map<String, GenAIModel> models = registry.fetchGenAIModels(fileUrl());

        then(models).hasSize(3);
        then(models).containsKey("openai/gpt-5-nano");
        then(models).containsKey("google/gemini-2-flash");
        then(models).containsKey("anthropic/claude-opus-4");
    }

    @Test
    void get_models_returns_empty_map_when_cache_is_empty_and_url_is_invalid() throws Exception {
        // Temporarily point the registry at a guaranteed-invalid URL so that
        // loadFromHttp() throws, the cache is empty, and getModels() falls back
        // to an empty map — without depending on external network availability.
        Field instanceField = GenAIModelRegistry.class.getDeclaredField("REGISTRY_INSTANCE");
        instanceField.setAccessible(true);
        GenAIModelRegistry original = (GenAIModelRegistry) instanceField.get(null);

        try {
            instanceField.set(null, new GenAIModelRegistry() {
                @Override
                public String getAPIUrl() {
                    return "file:///nonexistent/path/that/does/not/exist";
                }
            });

            Map<String, GenAIModel> result = GenAIModelRegistry.getModels();
            then(result).isEmpty();
        } finally {
            instanceField.set(null, original);
        }
    }

    @Test
    void get_models_returns_cached_result_without_reloading() throws Exception {
        // Pre-populate the cache
        Map<String, GenAIModel> fakeCache = new HashMap<>();
        fakeCache.put("gpt-5-nano", new GenAIModel(GenAIProvider.OPEN_AI, "gpt-5-nano", "desc", 1.0, 2.0));

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
    void find_by_name_returns_model_from_cache() throws Exception {
        Map<String, GenAIModel> fakeCache = new HashMap<>();
        GenAIModel expected = new GenAIModel(GenAIProvider.OPEN_AI, "gpt-5-nano", "desc", 1.0, 2.0);
        fakeCache.put("gpt-5-nano", expected);

        Field cacheField = GenAIModelRegistry.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, fakeCache);

        Field lastLoaded = GenAIModelRegistry.class.getDeclaredField("lastLoaded");
        lastLoaded.setAccessible(true);
        lastLoaded.setLong(null, System.currentTimeMillis());

        GenAIModel found = GenAIModelRegistry.findByName("gpt-5-nano");
        then(found).isEqualTo(expected);
    }

    @Test
    void find_by_name_returns_null_when_not_in_registry() throws Exception {
        // "nonexistent-model" is not present in any known registry, so null is expected.
        GenAIModel found = GenAIModelRegistry.findByName("nonexistent-model");
        then(found).isNull();
    }
}

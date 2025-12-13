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

import io.github.jeddict.ai.models.registry.GenAIModelRegistry;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * Author: Shiwani Gupta
 */
public enum GenAIProvider {

    OPEN_AI(
        "https://platform.openai.com/docs/models",
        "https://platform.openai.com/api-keys"
    ),
    CUSTOM_OPEN_AI("", ""),
    COPILOT_PROXY("", ""),
    GOOGLE(
        "https://ai.google.dev/gemini-api/docs/models/gemini",
        "https://console.cloud.google.com/apis/credentials"
    ),
    DEEPINFRA(
        "https://deepinfra.com/models",
        "https://deepinfra.com/dash/api_keys"
    ),
    DEEPSEEK(
        "https://api-docs.deepseek.com/quick_start/pricing",
        "https://platform.deepseek.com/api_keys"
    ),
    GROQ(
        "https://console.groq.com/docs/models",
        "https://console.groq.com/keys"
    ),
    MISTRAL(
        "https://docs.mistral.ai/getting-started/models/models_overview/",
        "https://console.mistral.ai/api-keys/"
    ),
    OLLAMA(
        "https://ollama.com/models",
        ""
    ),
    ANTHROPIC(
        "https://docs.anthropic.com/en/docs/about-claude/models",
        "https://console.anthropic.com/settings/keys"
    ),
    PERPLEXITY(
        "https://docs.perplexity.ai/getting-started/models",
        "https://www.perplexity.ai/account/api/keys"
    ),
    LM_STUDIO(
        "https://lmstudio.ai/models",
        ""
    ),
    GPT4ALL(
        "https://docs.gpt4all.io/gpt4all_desktop/models.html",
        ""
    ),
    OTHER("", "");

    private final String modelInfoUrl;
    private final String apiKeyUrl;

    GenAIProvider(String modelInfoUrl, String apiKeyUrl) {
        this.modelInfoUrl = modelInfoUrl;
        this.apiKeyUrl = apiKeyUrl;
    }

    public String getModelInfoUrl() {
        return modelInfoUrl;
    }

    public String getApiKeyUrl() {
        return apiKeyUrl;
    }

    /**
     * Returns providers that have either API key or local model location configured.
     */
    public static List<GenAIProvider> getConfiguredGenAIProviders() {
        PreferencesManager pm = PreferencesManager.getInstance();
        List<GenAIProvider> providers = new ArrayList<>();

        for (GenAIProvider provider : values()) {
            if (pm.getApiKey(provider) != null
                    || pm.getProviderLocation(provider) != null) {
                providers.add(provider);
            }
        }
        return providers;
    }

    /**
     * Returns model → provider map (only configured providers).
     */
    public static Map<String, GenAIProvider> getModelsByProvider() {
        Map<String, GenAIProvider> modelsByProvider = new HashMap<>();
        Map<String, GenAIModel> models = GenAIModelRegistry.getModels();

        for (GenAIProvider provider : getConfiguredGenAIProviders()) {
            for (GenAIModel model : models.values()) {
                if (model.getProvider() == provider) {
                    modelsByProvider.put(model.getName(), provider);
                }
            }
        }
        return modelsByProvider;
    }

    /**
     * Returns models for a specific provider.
     * Preference override > HTTP registry fallback.
     */
    public static Set<String> getModelsByProvider(GenAIProvider provider) {

        // 1️⃣ Preference override (manual / pinned models)
        List<GenAIModel> preferred =
                PreferencesManager.getInstance()
                        .getGenAIModelList(provider.name());

        if (preferred != null && !preferred.isEmpty()) {
            Set<String> models = new TreeSet<>();
            for (GenAIModel model : preferred) {
                models.add(model.getName());
            }
            return models;
        }

        // 2️⃣ Registry fallback (HTTP cached)
        Set<String> models = new TreeSet<>();
        for (GenAIModel model : GenAIModelRegistry.getModels().values()) {
            if (model.getProvider() == provider) {
                models.add(model.getName());
            }
        }
        return models;
    }

    /**
     * Alphabetically sorted providers (excluding OTHER).
     */
    public static GenAIProvider[] sortedValues() {
        return Arrays.stream(values())
                .filter(p -> p != OTHER)
                .sorted(Comparator.comparing(Enum::name))
                .toArray(GenAIProvider[]::new);
    }

    /**
     * Provider resolution from model ID.
     * Used by OpenRouter / HTTP registry.
     */
    public static GenAIProvider fromModelId(String modelId) {

        if (modelId == null) {
            return OTHER;
        }

        if (modelId.startsWith("openai/") || modelId.startsWith("gpt-")) {
            return OPEN_AI;
        }
        if (modelId.startsWith("google/") || modelId.startsWith("gemini")) {
            return GOOGLE;
        }
        if (modelId.startsWith("anthropic/") || modelId.startsWith("claude")) {
            return ANTHROPIC;
        }
        if (modelId.startsWith("mistralai/") || modelId.startsWith("mistral")) {
            return MISTRAL;
        }
        if (modelId.startsWith("perplexity/") || modelId.startsWith("sonar")) {
            return PERPLEXITY;
        }
        if (modelId.startsWith("deepseek/")) {
            return DEEPSEEK;
        }
        if (modelId.contains("/")) {
            return DEEPINFRA;
        }
        return OTHER;
    }
}

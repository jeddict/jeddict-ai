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

    OPEN_AI("https://platform.openai.com/docs/models", "https://platform.openai.com/api-keys"),
    CUSTOM_OPEN_AI("", ""),
    COPILOT_PROXY("", ""),
    GOOGLE("https://ai.google.dev/gemini-api/docs/models/gemini", "https://console.cloud.google.com/apis/credentials"),
    DEEPINFRA("https://deepinfra.com/models", "https://deepinfra.com/dash/api_keys"),
    DEEPSEEK("https://api-docs.deepseek.com/quick_start/pricing", "https://platform.deepseek.com/api_keys"),
    GROQ("https://console.groq.com/docs/models", "https://console.groq.com/keys"),
    MISTRAL("https://docs.mistral.ai/getting-started/models/models_overview/", "https://console.mistral.ai/api-keys/"),
    OLLAMA("https://ollama.com/models", ""),
    ANTHROPIC("https://docs.anthropic.com/en/docs/about-claude/models", "https://console.anthropic.com/settings/keys"),
    PERPLEXITY("https://docs.perplexity.ai/getting-started/models", "https://www.perplexity.ai/account/api/keys"),
    LM_STUDIO("https://lmstudio.ai/models", ""),
    GPT4ALL("https://docs.gpt4all.io/gpt4all_desktop/models.html", "");

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

    public static List<GenAIProvider> getConfiguredGenAIProviders() {
        PreferencesManager pm = PreferencesManager.getInstance();
        List<GenAIProvider> providers = new ArrayList<>();
        for (GenAIProvider value : GenAIProvider.values()) {
            if (pm.getApiKey(value) != null || pm.getProviderLocation(value) != null) {
                providers.add(value);
            }
        }
        return providers;
    }

    public static Map<String, GenAIProvider> getModelsByProvider() {
        List<GenAIProvider> configuredProviders = GenAIProvider.getConfiguredGenAIProviders();
        Map<String, GenAIProvider> modelsByProvider = new HashMap<>();

        for (GenAIProvider provider : configuredProviders) {
            for (Map.Entry<String, GenAIModel> entry : GenAIModel.MODELS.entrySet()) {
                if (entry.getValue().getProvider() == provider) {
                    modelsByProvider.put(entry.getKey(), provider);
                }
            }
        }

        return modelsByProvider;
    }

    public static Set<String> getModelsByProvider(GenAIProvider provider) {
        
        List<GenAIModel> iModels = PreferencesManager.getInstance().getGenAIModelList(provider.name());
        
        if(iModels != null && !iModels.isEmpty()) {
            Set<String> models = new TreeSet<>();
            iModels.forEach((m) -> {
                models.add(m.getName());
            });
            
            return models;
        }
        
        Set<String> models = new TreeSet<>();
        for (Map.Entry<String, GenAIModel> entry : GenAIModel.MODELS.entrySet()) {
            if (entry.getValue().getProvider() == provider) {
                models.add(entry.getKey());
            }
        }
        return models;
    }

    public static GenAIProvider[] sortedValues() {
        GenAIProvider[] providers = values();
        Arrays.sort(providers, Comparator.comparing(GenAIProvider::name));
        return providers;
    }

}

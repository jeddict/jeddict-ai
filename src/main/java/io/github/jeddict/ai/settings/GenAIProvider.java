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

/**
 *
 * Author: Shiwani Gupta
 */
public enum GenAIProvider {

    OPEN_AI("https://platform.openai.com/docs/models", "https://platform.openai.com/api-keys"),
    CUSTOM_OPEN_AI("", ""),
    GOOGLE("https://ai.google.dev/gemini-api/docs/models/gemini", "https://console.cloud.google.com/apis/credentials"),
    DEEPINFRA("https://deepinfra.com/models", "https://deepinfra.com/dash/api_keys"),
    DEEPSEEK("https://api-docs.deepseek.com/quick_start/pricing", "https://platform.deepseek.com/api_keys"),
    GROQ("https://console.groq.com/docs/models", "https://console.groq.com/keys"),
    MISTRAL("https://docs.mistral.ai/getting-started/models/models_overview/", "https://console.mistral.ai/api-keys/"),
    OLLAMA("https://ollama.com/models", ""),
    ANTHROPIC("https://docs.anthropic.com/en/docs/about-claude/models", "https://console.anthropic.com/settings/keys"),
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
}

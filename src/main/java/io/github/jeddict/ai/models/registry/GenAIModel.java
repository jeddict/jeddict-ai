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

/**
 * Class representing GenAI models used in AI analysis.
 *
 * <p>
 * Notes:
 * <ul>
 * <li>Prices are USD per 1M tokens unless stated otherwise.</li>
 * <li>Last verified: 2025-08-20. Some vendors change pricing frequently; use
 * these as defaults and allow override in settings.</li>
 * <li>When pricing was unclear on official pages at the time of writing, values
 * are set to <code>0.0</code> and described accordingly.</li>
 * </ul>
 *
 * Author: Shiwani Gupta, Gaurav Gupta
 */
public class GenAIModel {

    public static String DEFAULT_MODEL = "gpt-5-nano";
    private final GenAIProvider provider;
    private final String name;
    private final String description;
    private final double inputPrice;
    private final double outputPrice;

    public GenAIModel(
            GenAIProvider provider,
            String name,
            String description,
            double inputPrice,
            double outputPrice
    ) {
        this.provider = provider;
        this.name = normalizeName(name);
        this.description = description;
        this.inputPrice = inputPrice;
        this.outputPrice = outputPrice;
    }

    /**
     * Ensures model name never contains provider prefix. Examples: -
     * openai/gpt-5-nano → gpt-5-nano - mistralai/devstral-2512 → devstral-2512
     */
    private static String normalizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        int idx = rawName.lastIndexOf('/');
        return idx >= 0 ? rawName.substring(idx + 1) : rawName;
    }

    public GenAIProvider getProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getInputPrice() {
        return inputPrice;
    }

    public double getOutputPrice() {
        return outputPrice;
    }

    @Override
    public String toString() {
        return name;
    }
}

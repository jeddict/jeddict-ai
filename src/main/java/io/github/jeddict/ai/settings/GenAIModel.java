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

import static io.github.jeddict.ai.settings.GenAIProvider.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing GenAI models used in AI analysis.
 *
 * <p>Notes:
 * <ul>
 *   <li>Prices are USD per 1M tokens unless stated otherwise.</li>
 *   <li>Last verified: 2025-08-20. Some vendors change pricing frequently; use these as defaults and allow override in settings.</li>
 *   <li>When pricing was unclear on official pages at the time of writing, values are set to <code>0.0</code> and described accordingly.</li>
 * </ul>
 *
 * Author: Shiwani Gupta, Gaurav Gupta
 */
public class GenAIModel {

    public static String DEFAULT_MODEL = "gpt-5.1-codex-mini";
    public static Map<String, GenAIModel> MODELS = new HashMap<>();

    static {
        // -----------------------------
        // Google Gemini (Vertex/AI Studio)
        // -----------------------------
        MODELS.put("gemini-2.5-flash-lite", new GenAIModel(GOOGLE, "gemini-2.5-flash-lite",
                "Ultra-light Gemini 2.5 for very low latency and cost.", 0.0375, 0.15));
        MODELS.put("gemini-2.5-flash", new GenAIModel(GOOGLE, "gemini-2.5-flash",
                "Fast, multimodal model optimized for low cost/latency.", 0.10, 0.40));
        MODELS.put("gemini-2.5-pro", new GenAIModel(GOOGLE, "gemini-2.5-pro",
                "Most capable Gemini for advanced reasoning and long context.", 3.50, 10.00));
        // New 2025 model
        MODELS.put("gemini-3-pro", new GenAIModel(GOOGLE, "gemini-3-pro",
                "Gemini 3 Pro — next-gen Google multimodal model (2025).", 2.00, 12.00));

        // -----------------------------
        // OpenAI GPT Family
        // -----------------------------
        
        // GPT-5.1 Codex models
        MODELS.put("gpt-5.1-codex", new GenAIModel(OPEN_AI, "gpt-5.1-codex",
                "GPT-5.1 Codex — optimized for coding and software engineering tasks.", 1.25, 10.00));
        MODELS.put("gpt-5.1-codex-mini", new GenAIModel(OPEN_AI, "gpt-5.1-codex-mini",
                "Smaller Codex variant for cost-efficient code tasks.", 0.25, 2.00));
        MODELS.put("gpt-5.1-codex-max", new GenAIModel(OPEN_AI, "gpt-5.1-codex-max",
                "Frontier Codex model — long-horizon coding projects.", 1.25, 10.00));

        // GPT‑5 family (added; pricing frequently updated—left 0.0 intentionally)
        MODELS.put("gpt-5-mini", new GenAIModel(OPEN_AI, "gpt-5-mini",
                "Fast & capable general‑purpose model.",
                0.250, 2.000));
        MODELS.put("gpt-5-nano", new GenAIModel(OPEN_AI, "gpt-5-nano",
                "Ultra‑light, low‑latency model.",
                0.050, 0.400));
        MODELS.put("gpt-5", new GenAIModel(OPEN_AI, "gpt-5",
                "Most capable GPT model (text & image).",
                1.250, 10.0));
        // 4.1 family retained for compatibility
        MODELS.put("gpt-4.1-nano", new GenAIModel(OPEN_AI, "gpt-4.1-nano",
                "Fastest, most cost‑effective GPT‑4.1 model.",
                0.10, 0.40));
        MODELS.put("gpt-4.1-mini", new GenAIModel(OPEN_AI, "gpt-4.1-mini",
                "Balanced for intelligence, speed, and cost.",
                0.40, 1.60));
        MODELS.put("gpt-4.1", new GenAIModel(OPEN_AI, "gpt-4.1",
                "Fast, intelligent, flexible GPT model.", 2.00, 8.00));
        
        // -----------------------------
        // Anthropic Claude
        // -----------------------------
        MODELS.put("claude-opus-4-1-20250805", new GenAIModel(ANTHROPIC, "claude-opus-4-1-20250805",
                "Claude Opus 4.1 — frontier reasoning & coding.", 15.00, 75.00));
        MODELS.put("claude-sonnet-4.5", new GenAIModel(ANTHROPIC, "claude-sonnet-4.5",
                "Claude Sonnet 4.5 — upgraded reasoning & coding (Sept 2025).", 3.00, 15.00));
        MODELS.put("claude-haiku-4.5", new GenAIModel(ANTHROPIC, "claude-haiku-4.5",
                "Claude Haiku 4.5 — fast, cost-effective model (Oct 2025).", 1.00, 5.00));

        // -----------------------------
        // Mistral AI
        // -----------------------------
        MODELS.put("mistral-large-latest", new GenAIModel(MISTRAL, "mistral-large-latest",
                "Top-tier reasoning for high-complexity tasks.", 2.00, 6.00));
        MODELS.put("mistral-medium-3", new GenAIModel(MISTRAL, "mistral-medium-3",
                "Mistral Medium 3 — high performance per cost (May 2025).", 0.40, 2.00));
        MODELS.put("mistral-small-latest", new GenAIModel(MISTRAL, "mistral-small-latest",
                "Cost‑efficient, fast, and reliable for translation/summarization.", 0.20, 0.60));
        MODELS.put("codestral-latest", new GenAIModel(MISTRAL, "codestral-latest",
                "SOTA Mistral model trained specifically for code tasks.", 0.20, 0.60));
        MODELS.put("mistral-embed", new GenAIModel(MISTRAL, "mistral-embed",
                "Semantic embedding model (input‑only pricing).", 0.10, 0.00));
        MODELS.put("ministral-3b-latest", new GenAIModel(MISTRAL, "ministral-3b-latest",
                "Most efficient edge model.", 0.04, 0.04));
        MODELS.put("ministral-8b-latest", new GenAIModel(MISTRAL, "ministral-8b-latest",
                "Powerful model for on‑device use cases.", 0.10, 0.10));
        MODELS.put("mistral-nemo-latest", new GenAIModel(MISTRAL, "mistral-nemo-latest",
                "12B model with 128k context, built with NVIDIA.", 1.50, 4.50));
        MODELS.put("pixtral-large-latest", new GenAIModel(MISTRAL, "pixtral-large-latest",
                "Frontier‑class multimodal model for image+text understanding.", 2.50, 7.50));
        // Additional/older named variants retained
        MODELS.put("open-codestral-mamba", new GenAIModel(MISTRAL, "open-codestral-mamba",
                "The first Mamba 2 open‑source model, ideal for diverse tasks.", 0.0, 0.0));
        MODELS.put("pixtral-12b", new GenAIModel(MISTRAL, "pixtral-12b",
                "Vision‑capable small model.", 0.15, 0.15));
        MODELS.put("mistral-nemo", new GenAIModel(MISTRAL, "mistral-nemo",
                "Mistral model trained specifically for code tasks.", 0.15, 0.15));
        MODELS.put("pixtral-12b-2409", new GenAIModel(MISTRAL, "pixtral-12b-2409",
                "12B model with image understanding in addition to text.", 0.0, 0.0));
        MODELS.put("open-mistral-nemo", new GenAIModel(MISTRAL, "open-mistral-nemo",
                "Multilingual open‑source model released July 2024.", 0.0, 0.0));
        MODELS.put("mistral-saba-latest", new GenAIModel(MISTRAL, "mistral-saba-latest",
                "Efficient model optimized for languages from the Middle East and South Asia.", 1.00, 3.00));

        // -----------------------------
        // DeepInfra
        // -----------------------------
        MODELS.put("meta-llama/Llama-3.2-3B-Instruct", new GenAIModel(DEEPINFRA, "meta-llama/Llama-3.2-3B-Instruct",
                "3B instruct model by Meta for instructional tasks.", 0.15, 0.45));
        MODELS.put("Qwen/Qwen2.5-72B-Instruct", new GenAIModel(DEEPINFRA, "Qwen/Qwen2.5-72B-Instruct",
                "Large Qwen instruct model for various applications.", 0.20, 0.50));
        MODELS.put("google/gemma-2-9b-it", new GenAIModel(DEEPINFRA, "google/gemma-2-9b-it",
                "Gemma 2 IT model focused on performance.", 0.10, 0.30));
        MODELS.put("microsoft/WizardLM-2-8x22B", new GenAIModel(DEEPINFRA, "microsoft/WizardLM-2-8x22B",
                "8x22B model for advanced conversational applications.", 0.25, 0.75));
        MODELS.put("mistralai/Mistral-7B-Instruct-v0.3", new GenAIModel(DEEPINFRA, "mistralai/Mistral-7B-Instruct-v0.3",
                "7B instruct model optimized for general tasks.", 0.15, 0.45));

        // -----------------------------
        // DeepSeek (official API)
        // -----------------------------
        MODELS.put("deepseek-chat", new GenAIModel(DEEPSEEK, "deepseek-chat",
                "DeepSeek‑V3 (chat) — standard pricing shown; off‑peak discounts available.",
                0.27, 1.10)); // input (cache miss), output per 1M tokens
        MODELS.put("deepseek-reasoner", new GenAIModel(DEEPSEEK, "deepseek-reasoner",
                "DeepSeek‑R1 (reasoner) — hybrid CoT output; standard pricing shown.",
                0.55, 2.19)); // input (cache miss), output per 1M tokens

        // -----------------------------
        // Perplexity
        // -----------------------------
        MODELS.put(
            "sonar",
            new GenAIModel(
                PERPLEXITY,
                "sonar",
                "Lightweight, cost-effective search model (quick facts, news updates, simple Q&A, high-volume applications)",
                1, 1
            )
        );
        MODELS.put(
            "sonar-pro",
            new GenAIModel(
                PERPLEXITY,
                "sonar-pro",
                "Advanced search with deeper content understanding (complex queries, competitive analysis, detailed research)",
                3, 15
            )
        );
        MODELS.put(
            "sonar-reasoning",
            new GenAIModel(
                PERPLEXITY,
                "sonar-reasoning",
                "Quick problem-solving with step-by-step logic and search (logic puzzles, math problems, transparent reasoning)",
                1, 5
            )
        );
        MODELS.put(
            "sonar-reasoning-pro",
            new GenAIModel(
                PERPLEXITY,
                "sonar-reasoning-pro",
                "Enhanced multi-step reasoning with web search (complex problem-solving, research analysis, strategic planning)",
                2, 8
            )
        );
        MODELS.put(
            "sonar-deep-research",
            new GenAIModel(
                PERPLEXITY,
                "sonar-deep-research",
                "Exhaustive research and detailed report generation with search (academic research, market analysis, comprehensive reports)",
                2, 8
            )
        );
        MODELS.put("sonar", new GenAIModel(PERPLEXITY, "sonar",
                "Lightweight, cost-effective search model.", 1, 1));
        MODELS.put("sonar-pro", new GenAIModel(PERPLEXITY, "sonar-pro",
                "Advanced search with deeper content understanding.", 3, 15));
    }

    private final GenAIProvider provider;
    private final String name;
    private final String description;
    private final double inputPrice, outputPrice;

    public GenAIModel(GenAIProvider provider, String name, String description, double inputPrice, double outputPrice) {
        this.provider = provider;
        this.name = name;
        this.description = description;
        this.inputPrice = inputPrice;
        this.outputPrice = outputPrice;
    }

    public GenAIProvider getProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public static GenAIModel findByName(String name) {
        return MODELS.get(name);
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

    public String getFormattedInfo() {
        return String.format("%s: %s", name, description);
    }

    @Override
    public String toString() {
        return name;
    }
}

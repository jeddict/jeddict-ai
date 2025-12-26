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
package io.github.jeddict.ai.response;

import com.knuddels.jtokkit.Encodings;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.settings.ReportManager;
import java.util.Iterator;
import org.json.JSONObject;

/**
 * Tracks and manages token usage for input and output prompts with configurable granularity.
 * Author: Gaurav Gupta
 */
public class TokenHandler {

    private static final PreferencesManager preferencesManager = PreferencesManager.getInstance();
    private static final ReportManager reportManager = ReportManager.getInstance();

    public static int saveInputToken(final String content) {
        int tokenCount = countTokens(content);
        saveTokenUsage(reportManager.getDailyInputTokenStats(), tokenCount, true);
        return tokenCount;
    }

    public static void saveOutputToken(String response) {
        if (response == null || response.isEmpty()) {
            return;
        }

        int tokenCount = countTokens(response);
        saveTokenUsage(reportManager.getDailyOutputTokenStats(), tokenCount, false);
    }

    private static int countTokens(String text) {
        return Encodings.newDefaultEncodingRegistry()
                .getEncoding("cl100k_base")
                .map(enc -> enc.encode(text).size())
                .orElse(0);
    }

    private static void saveTokenUsage(JSONObject usage, int tokens, boolean isInput) {
        TokenGranularity granularity = preferencesManager.getTokenGranularity();
        String key = String.valueOf(granularity.getCurrentBucketKey());
        usage.put(key, usage.optInt(key, 0) + tokens);

        if (isInput) {
            reportManager.setDailyInputTokenStats(usage);
        } else {
            reportManager.setDailyOutputTokenStats(usage);
        }
    }

    public static int getLastNInputUsage(int n) {
        return getLastNUsage(reportManager.getDailyInputTokenStats(), n);
    }

    public static int getLastNOutputUsage(int n) {
        return getLastNUsage(reportManager.getDailyOutputTokenStats(), n);
    }

    private static int getLastNUsage(JSONObject usage, int n) {
        TokenGranularity granularity = preferencesManager.getTokenGranularity();
        long now = System.currentTimeMillis() / granularity.intervalMillis;
        int total = 0;

        for (int i = 0; i < n; i++) {
            String key = String.valueOf(now - i);
            total += usage.optInt(key, 0);
        }

        return total;
    }

    public static void cleanOldInputEntries() {
        cleanOldEntries(reportManager.getDailyInputTokenStats(), true);
    }

    public static void cleanOldOutputEntries() {
        cleanOldEntries(reportManager.getDailyOutputTokenStats(), false);
    }

    private static void cleanOldEntries(JSONObject usage, boolean isInput) {
        TokenGranularity granularity = preferencesManager.getTokenGranularity();
        long cutoff = (System.currentTimeMillis() / granularity.intervalMillis) - 30;

        Iterator<String> keys = usage.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (Long.parseLong(key) < cutoff) {
                    keys.remove();
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (isInput) {
            reportManager.setDailyInputTokenStats(usage);
        } else {
            reportManager.setDailyOutputTokenStats(usage);
        }
    }
}

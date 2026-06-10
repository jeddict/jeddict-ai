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

import io.github.jeddict.ai.models.Constant;
import static io.github.jeddict.ai.models.Constant.CUSTOM_OPEN_AI_URL;
import static io.github.jeddict.ai.models.Constant.DEEPINFRA_URL;
import static io.github.jeddict.ai.models.Constant.DEEPSEEK_URL;
import io.github.jeddict.ai.models.GroqModelFetcher;
import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import io.github.jeddict.ai.response.TokenGranularity;
import io.github.jeddict.ai.test.DummyProject;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.util.FileUtil;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static io.github.jeddict.ai.settings.PreferencesManager.JEDDICT_CONFIG;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for PreferencesManager covering path logic, persistence,
 * and system property overrides.
 */
public class PreferencesManagerTest extends TestBase {


    // --- Path and OS Logic Tests ---

    @Test
    public void constructor_without_given_path_linux() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("os.name", LINUX);
            System.setProperty("user.name", USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance(true);
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }

    @Test
    public void constructor_without_given_path_macos() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("os.name", MACOS);
            System.setProperty("user.name", USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance(true);
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }

    @Test
    public void constructor_without_given_path_windows() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("os.name", WINDOWS);
            System.setProperty("user.name", USER);

            Path expectedPath = FileUtil.getConfigPath().resolve(JEDDICT_CONFIG);

            PreferencesManager manager = PreferencesManager.getInstance(true);
            Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            FilePreferences filePreferences = (FilePreferences) prefsField.get(manager);

            then(filePreferences.preferencesPath).isEqualTo(expectedPath);
        });
    }

    // --- Core Functional and Persistence Tests ---

    @Test
    public void model_and_provider_and_api_key_preferences() {
        preferences.setProvider(GenAIProvider.GOOGLE);
        then(preferences.getProvider()).isEqualTo(GenAIProvider.GOOGLE);

        preferences.setModel("test-model");
        then(preferences.getModelName()).isEqualTo("test-model");

        preferences.setApiKey("secret-key");
        then(preferences.getApiKey()).isEqualTo("secret-key");

        preferences.clearApiKey();
        then(preferences.getApiKey(GenAIProvider.GOOGLE)).isEmpty();
    }

    @Test
    public void boolean_numeric_and_string_preferences() {
        preferences.setAiAssistantActivated(false);
        then(preferences.isAiAssistantActivated()).isFalse();

        preferences.setTemperature(0.75);
        then(preferences.getTemperature()).isEqualTo(0.75);

        preferences.setTimeout(30);
        then(preferences.getTimeout()).isEqualTo(30);

        preferences.setStreamEnabled(false);
        then(preferences.isStreamEnabled()).isFalse();

        preferences.setLogRequestsEnabled(true);
        then(preferences.isLogRequestsEnabled()).isTrue();

        preferences.setTokenGranularity(TokenGranularity.HOUR);
        then(preferences.getTokenGranularity()).isEqualTo(TokenGranularity.HOUR);
    }

    @Test
    public void file_extensions_and_exclude_dirs() {
        preferences.setFileExtensionToInclude("java,py");
        List<String> exts = preferences.getFileExtensionListToInclude();
        then(exts).containsExactly("java", "py");

        preferences.setExcludeDirs("foo,bar");
        List<String> dirs = preferences.getExcludeDirs();
        then(dirs).containsExactly("foo", "bar");
    }

    @Test
    public void prompts_and_custom_headers() {
        Map<String, String> prompts = new HashMap<>();
        prompts.put("custom", "hello world");
        preferences.setPrompts(prompts);

        Map<String, String> loaded = preferences.getPrompts();
        then(loaded.get("custom")).isEqualTo("hello world");

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "v1");
        preferences.setCustomHeaders(headers);
        then(preferences.getCustomHeaders().get("X-Test")).isEqualTo("v1");
    }

    @Test
    public void model_list_and_genai_models_handling() {
        preferences.setModelList(List.of("m1", "m2"));
        then(preferences.getModelList()).containsExactly("m1", "m2");

        GenAIModel gm = new GenAIModel(GenAIProvider.OPEN_AI, "gpt-test", "d", 0.1, 0.2);
        preferences.setGenAIModelList(List.of(gm), GenAIProvider.OPEN_AI.name());
        List<GenAIModel> loaded = preferences.getGenAIModelList(GenAIProvider.OPEN_AI.name());
        then(loaded).hasSize(1);
        then(loaded.get(0).name()).isEqualTo("gpt-test");

        then(preferences.getGenAIModelByName(GenAIProvider.OPEN_AI.name(), "gpt-test").name()).isEqualTo("gpt-test");
        then(preferences.getGenAIModelMap(GenAIProvider.OPEN_AI.name()).get("gpt-test").name()).isEqualTo("gpt-test");
    }

    @Test
    public void export_and_import_preferences() throws Exception {
        // create import file
        Path tmp = HOME.resolve("import-test.json");
        Files.writeString(tmp, "{\"model\":\"imported-model\"}");

        preferences.importPreferences(tmp.toString());
        then(preferences.getModelName()).isEqualTo("imported-model");

        Path export = HOME.resolve("export-test.json");
        preferences.exportPreferences(export.toString());
        then(Files.exists(export)).isTrue();
    }

    @Test
    public void build_and_test_command_detection() throws Exception {
        Path pDir = HOME.resolve("mvn-project");
        Files.createDirectories(pDir);
        Files.createFile(pDir.resolve("pom.xml"));

        DummyProject p = new DummyProject(pDir);

        then(preferences.getBuildCommand(p)).isEqualTo("mvn install");
        then(preferences.getTestCommand(p)).isEqualTo("mvn test");

        preferences.setBuildCommand(p, "./mvnw install");
        then(preferences.getBuildCommand(p)).isEqualTo("./mvnw install");
    }

    @Test
    public void chat_model_and_model_list_and_chatModel() {
        preferences.setChatModel("chat-x");
        then(preferences.getChatModel()).isEqualTo("chat-x");

        preferences.setModelList(List.of("alpha", "beta"));
        then(preferences.getModelList()).containsExactly("alpha", "beta");

        preferences.setModelList(List.of());
        then(preferences.getModelList()).isEmpty();
    }

    @Test
    public void prompts_migration_and_default_restoration() throws Exception {
        // Clear prompts node to force restoration
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);

        fp.setChild("prompts", new org.json.JSONObject());

        Map<String, String> loaded = preferences.getPrompts();
        // current implementation stores defaults in the underlying preferences
        // but does not populate the in-memory userPrompts map. Expect empty map.
        then(loaded).isEmpty();

        // underlying preferences should have the prompt saved
        org.json.JSONObject node = fp.getChild("prompts");
        then(node.keySet()).contains("rest");
    }

    @Test
    public void genai_model_list_error_handling() throws Exception {
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);

        String key = "modelPreferenceList_" + GenAIProvider.OPEN_AI.name();
        // provider invalid will be skipped by getGenAIModelList
        fp.put(key, "[{\"provider\":\"INVALID\",\"name\":\"nm\",\"description\":\"d\",\"inputPrice\":0.1,\"outputPrice\":0.2}]");

        List<GenAIModel> list = preferences.getGenAIModelList(GenAIProvider.OPEN_AI.name());
        then(list).isEmpty();
        then(preferences.getGenAIModelByName(GenAIProvider.OPEN_AI.name(), "nm")).isNull();

        // ensure map gracefully handles the same malformed entry
        then(preferences.getGenAIModelMap(GenAIProvider.OPEN_AI.name()).isEmpty()).isTrue();
    }

    @Test
    public void import_nonexistent_throws() {
        org.assertj.core.api.BDDAssertions.thenThrownBy(() -> preferences.importPreferences("nonexistent-file.json")).isInstanceOf(Exception.class);
    }

    @Test
    public void ant_build_detection() throws Exception {
        Path pDir = HOME.resolve("ant-project");
        Files.createDirectories(pDir);
        Files.createFile(pDir.resolve("build.xml"));

        DummyProject p = new DummyProject(pDir);
        then(preferences.getBuildCommand(p)).isEqualTo("ant build");
        then(preferences.getTestCommand(p)).isEqualTo("ant test");
    }

    @Test
    public void class_context_and_var_context_roundtrip() {
        preferences.setClassContextInlineHint(AIClassContext.CURRENT_PACKAGE);
        then(preferences.getClassContextInlineHint()).isEqualTo(AIClassContext.CURRENT_PACKAGE);

        preferences.setClassContext(AIClassContext.ENTIRE_PROJECT);
        then(preferences.getClassContext()).isEqualTo(AIClassContext.ENTIRE_PROJECT);

        preferences.setVarContext(AIClassContext.CURRENT_CLASS);
        then(preferences.getVarContext()).isEqualTo(AIClassContext.CURRENT_CLASS);
    }

    @Test
    public void set_inline_hint_enabled_updates_static_and_pref() throws Exception {
        // ensure both preferences are false first
        preferences.setInlineHintEnabled(false);
        preferences.setInlinePromptHintEnabled(false);
        PreferencesManager.setInlineHintsEnabled(false);
        then(preferences.isInlinePromptHintEnabled()).isFalse();
        then(PreferencesManager.isInlineHintsEnabled()).isFalse();

        // enabling inline hint should set preference and static inline hints
        preferences.setInlineHintEnabled(true);
        then(PreferencesManager.isInlineHintsEnabled()).isTrue();

        // disabling inline hint should update preference and still reflect combined state
        preferences.setInlinePromptHintEnabled(true);
        preferences.setInlineHintEnabled(false); // since prompt hint true, static should remain true
        then(PreferencesManager.isInlineHintsEnabled()).isTrue();

        // clear prompt hint and disable
        preferences.setInlinePromptHintEnabled(false);
        preferences.setInlineHintEnabled(false);
        then(PreferencesManager.isInlineHintsEnabled()).isFalse();
    }

    @Test
    public void get_prompts_when_pref_prompts_not_empty() throws Exception {
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);

        // put an encoded prompt value into storage
        String encoded = java.net.URLEncoder.encode("a prompt with spaces & =?", "UTF-8");
        fp.putChild("prompts", "stored", encoded);

        // reset in-memory userPrompts
        Field userPromptsField = PreferencesManager.class.getDeclaredField("userPrompts");
        userPromptsField.setAccessible(true);
        Map<String, String> map = (Map<String, String>) userPromptsField.get(preferences);
        map.clear();

        Map<String, String> loaded = preferences.getPrompts();
        then(loaded).containsKey("stored");
        then(loaded.get("stored")).isEqualTo("a prompt with spaces & =?");
    }

    @Test
    public void provider_location_and_provider_error_cases() throws Exception {
        preferences.setProvider(GenAIProvider.GOOGLE);
        preferences.setProviderLocation("http://example.com");
        then(preferences.getProviderLocation()).isEqualTo("http://example.com");
        then(preferences.getProviderLocation(GenAIProvider.GOOGLE)).isEqualTo("http://example.com");

        // Simulate invalid provider stored
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);
        fp.put("provider", "INVALID");

        // getProvider should fallback to OPEN_AI
        then(preferences.getProvider()).isEqualTo(GenAIProvider.OPEN_AI);
    }

    @Test
    public void inline_hints_and_flags() {
        preferences.setInlinePromptHintEnabled(true);
        then(preferences.isInlinePromptHintEnabled()).isTrue();
        preferences.setInlinePromptHintEnabled(false);
        then(preferences.isInlinePromptHintEnabled()).isFalse();

        PreferencesManager.setInlineHintsEnabled(true);
        then(PreferencesManager.isInlineHintsEnabled()).isTrue();
        PreferencesManager.setInlineHintsEnabled(false);
        then(PreferencesManager.isInlineHintsEnabled()).isFalse();

        preferences.setHintsEnabled(true);
        then(preferences.isHintsEnabled()).isTrue();
        preferences.setHintsEnabled(false);
        then(preferences.isHintsEnabled()).isFalse();

        preferences.setSmartCodeEnabled(true);
        then(preferences.isSmartCodeEnabled()).isTrue();
        preferences.setSmartCodeEnabled(false);
        then(preferences.isSmartCodeEnabled()).isFalse();

        preferences.setCompletionAllQueryType(true);
        then(preferences.isCompletionAllQueryType()).isTrue();
        preferences.setCompletionAllQueryType(false);
        then(preferences.isCompletionAllQueryType()).isFalse();

        preferences.setDescriptionEnabled(true);
        then(preferences.isDescriptionEnabled()).isTrue();
        preferences.setDescriptionEnabled(false);
        then(preferences.isDescriptionEnabled()).isFalse();

        preferences.setExcludeJavadocEnabled(true);
        then(preferences.isExcludeJavadocEnabled()).isTrue();
        preferences.setExcludeJavadocEnabled(false);
        then(preferences.isExcludeJavadocEnabled()).isFalse();
    }

    @Test
    public void conversation_and_misc_preferences() throws Exception {
        preferences.setConversationContext(5);
        then(preferences.getConversationContext()).isEqualTo(5);

        Map<String, String> headers = new HashMap<>();
        headers.put("H1", "v1");
        preferences.setCustomHeaders(headers);
        then(preferences.getCustomHeaders()).containsEntry("H1", "v1");

        Map<String, String> prompts = new HashMap<>();
        prompts.put("a", "line1+line2 & extras");
        preferences.setPrompts(prompts);
        then(preferences.getPrompts().get("a")).isEqualTo("line1+line2 & extras");

        preferences.setGlobalRules("GLOBAL");
        then(preferences.getGlobalRules()).isEqualTo("GLOBAL");
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);
        then(fp.get("systemMessage", null)).isNull();

        DummyProject p = new DummyProject(projectFolderPath());
        preferences.setProjectRules(p, "PRULE");
        then(preferences.getProjectRules(p)).isEqualTo("PRULE");

        preferences.setAssistantAction("ACT");
        then(preferences.getAssistantAction()).isEqualTo("ACT");
        preferences.setSessionRules("SRULE");
        then(preferences.getSessionRules()).isEqualTo("SRULE");

        preferences.setTestCommand(p, "./mvnw test");
        then(preferences.getTestCommand(p)).isEqualTo("./mvnw test");
    }

    @Test
    public void logging_org_and_numeric_preferences() {
        preferences.setLogResponsesEnabled(true);
        then(preferences.isLogResponsesEnabled()).isTrue();
        preferences.setLogResponsesEnabled(false);
        then(preferences.isLogResponsesEnabled()).isFalse();

        preferences.setOrganizationId("ORG1");
        then(preferences.getOrganizationId()).isEqualTo("ORG1");

        preferences.setTopK(7);
        then(preferences.getTopK()).isEqualTo(7);

        preferences.setMaxCompletionTokens(400);
        then(preferences.getMaxCompletionTokens()).isEqualTo(400);

        preferences.setMaxOutputTokens(500);
        then(preferences.getMaxOutputTokens()).isEqualTo(500);

        preferences.setPresencePenalty(0.3);
        then(preferences.getPresencePenalty()).isEqualTo(0.3);

        preferences.setFrequencyPenalty(0.4);
        then(preferences.getFrequencyPenalty()).isEqualTo(0.4);

        preferences.setSeed(42);
        then(preferences.getSeed()).isEqualTo(42);

        preferences.setMaxRetries(3);
        then(preferences.getMaxRetries()).isEqualTo(3);

        preferences.setTokenGranularity(TokenGranularity.WEEK);
        then(preferences.getTokenGranularity()).isEqualTo(TokenGranularity.WEEK);

        preferences.setLastBrowseDirectory("/tmp");
        then(preferences.getLastBrowseDirectory()).isEqualTo("/tmp");
    }

    @Test
    public void getProviderLocation_returns_default_for_some_models() {
        then(preferences.getProviderLocation(GenAIProvider.DEEPINFRA))
                .isEqualTo(DEEPINFRA_URL);
        then(preferences.getProviderLocation(GenAIProvider.DEEPSEEK))
                .isEqualTo(DEEPSEEK_URL);
        then(preferences.getProviderLocation(GenAIProvider.GROQ))
                .isEqualTo(GroqModelFetcher.API_URL);
        then(preferences.getProviderLocation(GenAIProvider.CUSTOM_OPEN_AI))
                .isEqualTo(CUSTOM_OPEN_AI_URL);
        then(preferences.getProviderLocation(GenAIProvider.GPT4ALL))
                .isEqualTo(Constant.GPT4ALL_URL);
    }

    // --- More Specific Override and Migration Tests ---

    @Test
    public void model_name_uses_system_property() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("openai.model", "prop-model");
            then(preferences.getModelName()).isEqualTo("prop-model");
        });
    }

    @Test
    public void api_key_prefers_system_property_over_missing() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("openai.api.key", "syskey");
            then(preferences.getApiKey()).isEqualTo("syskey");
        });
    }

    @Test
    public void api_key_returns_null_when_missing() throws Exception {
        // Ensure no environment variables or system properties interfere
        // (Assuming restoreSystemProperties and clean environment in test setup)
        preferences.clearApiKey();
        then(preferences.getApiKey()).isEmpty();
    }

    @Test
    public void global_rules_migration_from_old_key() throws Exception {
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);

        fp.put("systemMessage", "old-global");

        then(preferences.getGlobalRules()).isEqualTo("old-global");
        then(fp.get("systemMessage", null)).isNull();
    }

    @Test
    public void gradle_build_detection() throws Exception {
        Path pDir = HOME.resolve("gradle-project");
        Files.createDirectories(pDir);
        Files.createFile(pDir.resolve("gradlew"));
        Files.createFile(pDir.resolve("build.gradle"));

        DummyProject p = new DummyProject(pDir);
        then(preferences.getBuildCommand(p)).isEqualTo("./gradlew build");
        then(preferences.getTestCommand(p)).isEqualTo("./gradlew test");
    }

    @Test
    public void numeric_and_boolean_prefs_roundtrip_more() {
        preferences.setTopP(0.2);
        then(preferences.getTopP()).isEqualTo(0.2);

        preferences.setRepeatPenalty(1.2);
        then(preferences.getRepeatPenalty()).isEqualTo(1.2);

        preferences.setMaxTokens(123);
        then(preferences.getMaxTokens()).isEqualTo(123);

        preferences.setAllowCodeExecution(true);
        then(preferences.isAllowCodeExecution()).isTrue();

        preferences.setIncludeCodeExecutionOutput(true);
        then(preferences.isIncludeCodeExecutionOutput()).isTrue();

        preferences.setTokenGranularity(TokenGranularity.DAY);
        then(preferences.getTokenGranularity()).isEqualTo(TokenGranularity.DAY);
    }

    @Test
    public void prompts_and_system_prompts_present() {
        Map<String, String> prompts = new HashMap<>();
        prompts.put("p1", "v1");
        preferences.setPrompts(prompts);

        Map<String, String> loaded = preferences.getPrompts();
        then(loaded.get("p1")).isEqualTo("v1");

        Map<String, String> system = preferences.getSystemPrompts();
        then(system).isNotEmpty();
    }

    @Test
    public void custom_headers_roundtrip_more() {
        Map<String, String> headers = new HashMap<>();
        headers.put("A", "1");
        headers.put("B", "2");
        preferences.setCustomHeaders(headers);

        Map<String, String> loaded = preferences.getCustomHeaders();
        then(loaded).containsEntry("A", "1").containsEntry("B", "2");
    }

    @Test
    public void misc_string_and_list_prefs_more() {
        preferences.setChatPlacement("Left");
        then(preferences.getChatPlacement()).isEqualTo("Left");

        preferences.setSubmitShortcut("Alt+S");
        then(preferences.getSubmitShortcut()).isEqualTo("Alt+S");

        preferences.setFileExtensionToInclude("java,kt");
        List<String> exts = preferences.getFileExtensionListToInclude();
        then(exts).containsExactly("java", "kt");

        preferences.setExcludeDirs("a,b,c");
        List<String> dirs = preferences.getExcludeDirs();
        then(dirs).containsExactly("a", "b", "c");
    }

    @Test
    public void development_property_updates_logging_settings() {
        preferences.setDevelopment(true);
        then(preferences.isDevelopment()).isTrue();
        then(preferences.isLogRequestsEnabled()).isTrue();
        then(preferences.isLogResponsesEnabled()).isTrue();

        preferences.setDevelopment(false);
        then(preferences.isDevelopment()).isFalse();
        then(preferences.isLogRequestsEnabled()).isFalse();
        then(preferences.isLogResponsesEnabled()).isFalse();
    }

    @Test
    public void get_and_set_current_model() {
        preferences.setGenAIModelList(List.of(
            new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, "model1", "", 1.0, 1.0),
            new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, "model2", "", 2.0, 2.0)
        ), GenAIProvider.CUSTOM_OPEN_AI.name());
        preferences.setProvider(GenAIProvider.CUSTOM_OPEN_AI);
        preferences.setModel("model1");

        then(preferences.getModel()).isEqualTo(new GenAIModel(GenAIProvider.CUSTOM_OPEN_AI, "model1", "", 1.0, 1.0));
    }
}

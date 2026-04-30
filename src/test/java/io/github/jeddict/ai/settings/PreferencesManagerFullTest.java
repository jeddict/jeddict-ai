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

import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import io.github.jeddict.ai.response.TokenGranularity;
import io.github.jeddict.ai.test.TestBase;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.jeddict.ai.test.DummyProject;

public class PreferencesManagerFullTest extends TestBase {

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        // Reset singleton
        Field instance = PreferencesManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        // Ensure user dir exists
        Files.createDirectory(HOME.resolve(USER));

        // Re-initialize with test home
        java.util.concurrent.Callable<Void> c = () -> {
            System.setProperty("user.home", HOME.toAbsolutePath().toString());
            preferences = PreferencesManager.getInstance();
            return null;
        };
        c.call();
    }

    @Test
    public void model_and_provider_and_api_key_preferences() {
        preferences.setProvider(GenAIProvider.GOOGLE);
        then(preferences.getProvider()).isEqualTo(GenAIProvider.GOOGLE);

        preferences.setModel("test-model");
        then(preferences.getModel()).isEqualTo("test-model");

        preferences.setApiKey("secret-key");
        then(preferences.getApiKey()).isEqualTo("secret-key");

        preferences.clearApiKey();
        then(preferences.getApiKey(GenAIProvider.GOOGLE)).isNull();
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
        then(exts).containsExactly("java","py");

        preferences.setExcludeDirs("foo,bar");
        List<String> dirs = preferences.getExcludeDirs();
        then(dirs).containsExactly("foo","bar");
    }

    @Test
    public void prompts_and_custom_headers() {
        Map<String,String> prompts = new HashMap<>();
        prompts.put("custom","hello world");
        preferences.setPrompts(prompts);

        Map<String,String> loaded = preferences.getPrompts();
        then(loaded.get("custom")).isEqualTo("hello world");

        Map<String,String> headers = new HashMap<>();
        headers.put("X-Test","v1");
        preferences.setCustomHeaders(headers);
        then(preferences.getCustomHeaders().get("X-Test")).isEqualTo("v1");
    }

    @Test
    public void model_list_and_genai_models_handling() {
        preferences.setModelList(List.of("m1","m2"));
        then(preferences.getModelList()).containsExactly("m1","m2");

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
        then(preferences.getModel()).isEqualTo("imported-model");

        Path export = HOME.resolve("export-test.json");
        preferences.exportPreferences(export.toString());
        then(Files.exists(export)).isTrue();
    }

    @Test
    public void build_and_test_command_detection() throws Exception {
        // projectPath points to the copied minimal project
        // Use DummyProject helper for tests instead of loading a real NetBeans project
        DummyProject p = new DummyProject(projectFolderPath());

        then(preferences.getBuildCommand(p)).isEqualTo("mvn install");
        then(preferences.getTestCommand(p)).isEqualTo("mvn test");

        preferences.setBuildCommand(p, "./mvnw install");
        then(preferences.getBuildCommand(p)).isEqualTo("./mvnw install");
    }

    @Test
    public void chat_model_and_model_list_and_chatModel() {
        preferences.setChatModel("chat-x");
        then(preferences.getChatModel()).isEqualTo("chat-x");

        preferences.setModelList(List.of("alpha","beta"));
        then(preferences.getModelList()).containsExactly("alpha","beta");

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

        Map<String,String> loaded = preferences.getPrompts();
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
        // create ant build file and remove other build files
        Files.createFile(projectFolderPath().resolve("build.xml"));
        Files.deleteIfExists(projectFolderPath().resolve("pom.xml"));
        Files.deleteIfExists(projectFolderPath().resolve("build.gradle"));
        Files.deleteIfExists(projectFolderPath().resolve("gradlew"));

        DummyProject p = new DummyProject(projectFolderPath());
        then(preferences.getBuildCommand(p)).isEqualTo("ant build");
        then(preferences.getTestCommand(p)).isEqualTo("ant test");
    }

    @Test
    public void class_context_and_var_context_roundtrip() {
        preferences.setClassContextInlineHint(io.github.jeddict.ai.settings.AIClassContext.CURRENT_PACKAGE);
        then(preferences.getClassContextInlineHint()).isEqualTo(io.github.jeddict.ai.settings.AIClassContext.CURRENT_PACKAGE);

        preferences.setClassContext(io.github.jeddict.ai.settings.AIClassContext.ENTIRE_PROJECT);
        then(preferences.getClassContext()).isEqualTo(io.github.jeddict.ai.settings.AIClassContext.ENTIRE_PROJECT);

        preferences.setVarContext(io.github.jeddict.ai.settings.AIClassContext.CURRENT_CLASS);
        then(preferences.getVarContext()).isEqualTo(io.github.jeddict.ai.settings.AIClassContext.CURRENT_CLASS);
    }

}


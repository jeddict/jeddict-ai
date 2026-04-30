package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIProvider;
import io.github.jeddict.ai.response.TokenGranularity;
import io.github.jeddict.ai.test.DummyProject;
import io.github.jeddict.ai.test.TestBase;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PreferencesManagerMoreTest extends TestBase {

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        // Reset singleton
        Field instance = PreferencesManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        Files.createDirectory(HOME.resolve(USER));

        System.setProperty("user.home", HOME.toAbsolutePath().toString());
        preferences = PreferencesManager.getInstance();
    }

    @Test
    public void model_name_uses_system_property() {
        System.setProperty("openai.model", "prop-model");
        then(preferences.getModelName()).isEqualTo("prop-model");
        System.clearProperty("openai.model");
    }

    @Test
    public void api_key_prefers_system_property_over_missing() {
        System.setProperty("openai.api.key", "syskey");
        then(preferences.getApiKey()).isEqualTo("syskey");
        System.clearProperty("openai.api.key");
    }

    @Test
    public void global_rules_migration_from_old_key() throws Exception {
        // Inject old key into underlying FilePreferences
        Field prefsField = PreferencesManager.class.getDeclaredField("preferences");
        prefsField.setAccessible(true);
        FilePreferences fp = (FilePreferences) prefsField.get(preferences);

        fp.put("systemMessage", "old-global");

        // Trigger migration
        then(preferences.getGlobalRules()).isEqualTo("old-global");
        // Old key should be removed
        then(fp.get("systemMessage", null)).isNull();
    }

    @Test
    public void gradle_build_detection() throws Exception {
        // create gradle files in project dir
        Files.createFile(projectFolderPath().resolve("gradlew"));
        Files.createFile(projectFolderPath().resolve("build.gradle"));

        DummyProject p = new DummyProject(projectFolderPath());
        then(preferences.getBuildCommand(p)).isEqualTo("./gradlew build");
        then(preferences.getTestCommand(p)).isEqualTo("./gradlew test");
    }

    @Test
    public void numeric_and_boolean_prefs_roundtrip() {
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
        Map<String,String> prompts = new HashMap<>();
        prompts.put("p1","v1");
        preferences.setPrompts(prompts);

        Map<String,String> loaded = preferences.getPrompts();
        then(loaded.get("p1")).isEqualTo("v1");

        Map<String,String> system = preferences.getSystemPrompts();
        then(system).isNotEmpty();
    }

    @Test
    public void custom_headers_roundtrip() {
        Map<String,String> headers = new HashMap<>();
        headers.put("A","1");
        headers.put("B","2");
        preferences.setCustomHeaders(headers);

        Map<String,String> loaded = preferences.getCustomHeaders();
        then(loaded).containsEntry("A","1").containsEntry("B","2");
    }

    @Test
    public void misc_string_and_list_prefs() {
        preferences.setChatPlacement("Left");
        then(preferences.getChatPlacement()).isEqualTo("Left");

        preferences.setSubmitShortcut("Alt+S");
        then(preferences.getSubmitShortcut()).isEqualTo("Alt+S");

        preferences.setFileExtensionToInclude("java,kt");
        List<String> exts = preferences.getFileExtensionListToInclude();
        then(exts).containsExactly("java","kt");

        preferences.setExcludeDirs("a,b,c");
        List<String> dirs = preferences.getExcludeDirs();
        then(dirs).containsExactly("a","b","c");
    }
}

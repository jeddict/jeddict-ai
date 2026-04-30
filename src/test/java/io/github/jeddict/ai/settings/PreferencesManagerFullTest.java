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
        Path pom = projectFolderPath().resolve("pom.xml");
        var p = project(pom.toString());

        then(preferences.getBuildCommand(p)).isEqualTo("mvn install");
        then(preferences.getTestCommand(p)).isEqualTo("mvn test");

        preferences.setBuildCommand(p, "./mvnw install");
        then(preferences.getBuildCommand(p)).isEqualTo("./mvnw install");
    }
}

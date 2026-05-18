package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIProvider;
import io.github.jeddict.ai.test.TestBase;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Map;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JeddictPreferencesTest extends TestBase {

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
    public void save_persists_ui_settings_to_preferences_manager() {
        // Construct headless JeddictPreferences (skip JavaFX UI creation)
        JeddictPreferences ui = new JeddictPreferences();

        // Booleans
        ui.settings.set("enableAssistant", true);
        ui.settings.set("enableInlineCompletion", true);
        ui.settings.set("enableInlinePromptHint", true);
        ui.settings.set("enableInlineHintOnEnter", true);
        ui.settings.set("enableInlineHint", true);

        // Contexts
        ui.settings.set("classContext", AIClassContext.ENTIRE_PROJECT);
        ui.settings.set("varClassContext", AIClassContext.CURRENT_PACKAGE);

        // Provider / model / keys
        ui.settings.set("provider", GenAIProvider.OPEN_AI);
        ui.settings.set("model", "gpt-test");
        ui.settings.set("apiKey", "apikey-123");
        ui.settings.set("provider_location", "https://example.local");

        // Numeric values
        ui.settings.set("temperature", 0.7);
        ui.settings.set("topP", 0.6);
        ui.settings.set("presencePenalty", 0.1);
        ui.settings.set("frequencyPenalty", 0.2);
        ui.settings.set("seed", 42);
        ui.settings.set("maxTokens", 1111);
        ui.settings.set("maxCompletionTokens", 2222);
        ui.settings.set("maxOutputTokens", 3333);
        ui.settings.set("topK", 5);

        // Stream / timeout / retries / org
        ui.settings.set("stream", true);
        ui.settings.set("timeout", 60);
        ui.settings.set("maxRetries", 3);
        ui.settings.set("organizationId", "org-1");

        // Code execution
        ui.settings.set("allowCodeExecution", true);
        ui.settings.set("includeCodeExecutionOutput", true);

        // File lists
        ui.settings.set("fileExtensionToInclude", "java,kt");
        ui.settings.set("excludeDirs", "a,b,c");

        // Conversation context (string -> numeric mapping)
        ui.settings.set("conversationContext", "Include last 5 replies");

        // Rules and headers
        ui.settings.set("globalRules", "rule-xyz");
        ui.settings.set("headers", "A:1\nB:2");

        // Prompts (headless): provide via settings map so save() can persist them
        ui.settings.set("prompts", java.util.Map.of("p1", "prompt one", "p2", "prompt two"));

        // Persist
        ui.save();

        PreferencesManager pm = PreferencesManager.getInstance();

        // Prompts should be saved
        Map<String,String> savedPrompts = pm.getPrompts();
        then(savedPrompts).containsEntry("p1","prompt one").containsEntry("p2","prompt two");

        then(pm.isAiAssistantActivated()).isTrue();
        then(pm.isSmartCodeEnabled()).isTrue();
        then(pm.isInlinePromptHintEnabled()).isTrue();
        then(pm.isInlineHintEnabled()).isTrue();
        then(pm.isHintsEnabled()).isTrue();

        then(pm.getClassContext()).isEqualTo(AIClassContext.ENTIRE_PROJECT);
        then(pm.getClassContextInlineHint()).isEqualTo(AIClassContext.ENTIRE_PROJECT);
        then(pm.getVarContext()).isEqualTo(AIClassContext.CURRENT_PACKAGE);

        then(pm.getProvider()).isEqualTo(GenAIProvider.OPEN_AI);
        then(pm.getModel()).isEqualTo("gpt-test");
        then(pm.getApiKey()).isEqualTo("apikey-123");
        then(pm.getProviderLocation()).isEqualTo("https://example.local");

        then(pm.getTemperature()).isEqualTo(0.7);
        then(pm.getTopP()).isEqualTo(0.6);
        then(pm.getPresencePenalty()).isEqualTo(0.1);
        then(pm.getFrequencyPenalty()).isEqualTo(0.2);
        then(pm.getSeed()).isEqualTo(42);
        then(pm.getMaxTokens()).isEqualTo(1111);
        then(pm.getMaxCompletionTokens()).isEqualTo(2222);
        then(pm.getMaxOutputTokens()).isEqualTo(3333);
        then(pm.getTopK()).isEqualTo(5);

        then(pm.isStreamEnabled()).isTrue();
        then(pm.getTimeout()).isEqualTo(60);
        then(pm.getMaxRetries()).isEqualTo(3);
        then(pm.getOrganizationId()).isEqualTo("org-1");

        then(pm.getFileExtensionListToInclude()).containsExactly("java","kt");
        then(pm.getExcludeDirs()).containsExactly("a","b","c");

        then(pm.getConversationContext()).isEqualTo(5);

        then(pm.getGlobalRules()).isEqualTo("rule-xyz");

        Map<String,String> headers = pm.getCustomHeaders();
        then(headers).containsEntry("A","1").containsEntry("B","2");
    }
}

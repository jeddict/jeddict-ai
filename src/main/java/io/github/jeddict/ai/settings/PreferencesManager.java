/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.settings;

/**
 *
 * @author Gaurav Gupta
 */
import org.openide.util.NbPreferences;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

public class PreferencesManager {

    private final Preferences preferences;
    private static final String API_KEY_ENV_VAR = "OPENAI_API_KEY";
    private static final String API_KEY_SYS_PROP = "openai.api.key";
    private static final String MODEL_ENV_VAR = "OPENAI_MODEL";
    private static final String MODEL_SYS_PROP = "openai.model";
    private static final String API_KEY_PREFERENCES = "api_key";

    private PreferencesManager() {
        preferences = NbPreferences.forModule(AIAssistancePanel.class);
    }

    private static PreferencesManager instance;

    public static PreferencesManager getInstance() {
        if (instance == null) {
            instance = new PreferencesManager();
        }
        return instance;
    }

    public void clearApiKey() {
        preferences.remove(API_KEY_PREFERENCES);
    }

    public void setApiKey(String key) {
        preferences.put(API_KEY_PREFERENCES, key);
    }

    public String getApiKey() {
        // First, try to get the API key from the environment variable
        String apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment variable, try system properties
            apiKey = System.getProperty(API_KEY_SYS_PROP);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment or system properties, check Preferences
            apiKey = preferences.get(API_KEY_PREFERENCES, null);
        }

        if (apiKey == null || apiKey.isEmpty()) {
            // If still not found, show input dialog to enter API key
            apiKey = JOptionPane.showInputDialog(null,
                    "Your OpenAI API key is not configured. Please enter it now.",
                    "OpenAI API Key Required",
                    JOptionPane.WARNING_MESSAGE);

            if (apiKey != null && !apiKey.isEmpty()) {
                // Save the entered API key in Preferences for future use
                preferences.put(API_KEY_PREFERENCES, apiKey);
            } else {
                // If user didn't provide a valid key, show error and throw exception
                JOptionPane.showMessageDialog(null,
                        "OpenAI API key setup is incomplete. Please provide a valid key.",
                        "API Key Not Configured",
                        JOptionPane.ERROR_MESSAGE);
                throw new IllegalStateException("A valid OpenAI API key is necessary for this feature.");
            }
        }

        return apiKey;
    }

    public String getModelName() {
        // Try to get the model name from the environment variable
        String modelName = System.getenv(MODEL_ENV_VAR);
        if (modelName == null || modelName.isEmpty()) {
            // If not found in environment variable, try system properties
            modelName = System.getProperty(MODEL_SYS_PROP);
        }
        if (modelName == null || modelName.isEmpty()) {
            // Fallback to default model name
            modelName = GPTModel.GPT_4O_MINI.getDisplayName();
        }
        return modelName;
    }

    public boolean isAiAssistantActivated() {
        return preferences.getBoolean("aiAssistantActivated", true);
    }

    public void setAiAssistantActivated(boolean activated) {
        preferences.putBoolean("aiAssistantActivated", activated);
    }

    public AIClassContext getClassContext() {
        String classContext = preferences.get("classContext", null);
        if( classContext != null){
            try {
            return AIClassContext.valueOf(classContext);
            } catch(IllegalArgumentException iae) {
                // .. skip
            }
        }
        return AIClassContext.REFERENCED_CLASSES;
    }

    public void setClassContext(AIClassContext context) {
        preferences.put("classContext", context != null ? context.name() : null);
    }

    public GPTModel getGptModel() {
        String gptModel = preferences.get("gptModel", null);
        if( gptModel != null){
            try {
            return GPTModel.valueOf(gptModel);
            } catch(IllegalArgumentException iae) {
                // .. skip
            }
        }
        return GPTModel.GPT_4O_MINI;
    }

    public void setGptModel(GPTModel model) {
        preferences.put("gptModel", model != null ? model.toString() : null);
    }

    public boolean isHintsEnabled() {
        return preferences.getBoolean("enableHints", true);
    }

    public void setHintsEnabled(boolean enabled) {
        preferences.putBoolean("enableHints", enabled);
    }

    public boolean isSmartCodeEnabled() {
        return preferences.getBoolean("enableSmartCode", true);
    }

    public void setSmartCodeEnabled(boolean enabled) {
        preferences.putBoolean("enableSmartCode", enabled);
    }
}

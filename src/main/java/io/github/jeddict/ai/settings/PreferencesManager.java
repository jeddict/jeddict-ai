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
 * @author Gaurav Gupta, Shiwani Gupta
 */
import io.github.jeddict.ai.response.TokenGranularity;
import static io.github.jeddict.ai.settings.GenAIModel.DEFAULT_MODEL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.project.Project;

public class PreferencesManager {

    private final FilePreferences preferences;
    private static final String API_KEY_ENV_VAR = "OPENAI_API_KEY";
    private static final String API_KEY_SYS_PROP = "openai.api.key";
    private static final String MODEL_ENV_VAR = "OPENAI_MODEL";
    private static final String MODEL_SYS_PROP = "openai.model";
    private static final String API_KEY_PREFERENCES = "api_key";
    private static final String PROVIDER_LOCATION_PREFERENCES = "provider_location";
    private static final String PROVIDER_PREFERENCE = "provider";
    private static final String MODEL_PREFERENCE = "model";
    private static final String MODEL_LIST = "modelList";
    private static final String CHAT_MODEL_PREFERENCE = "chatModel";
    private static final String CHAT_MODEL_LIST = "chatModelList";
    private static final String GLOBAL_RULES_PREFERENCE = "globalRules";
    private static final String PROJECT_RULES_PREFERENCE = "projectRules";
    private static final String BUILD_COMMAND_PREFERENCE = "buildCommand";
    private static final String TEST_COMMAND_PREFERENCE = "testCommand";
    private static final String SESSION_RULES_PREFERENCE = "sessionRules";
    private static final String ASSISTANT_ACTION_PREFERENCE = "assistantAction";
    private static final String TEMPERATURE_PREFERENCE = "temperature";
    private static final String TOP_P_PREFERENCE = "topP";
    private static final String STREAM_PREFERENCE = "stream";
    private static final String TIMEOUT_PREFERENCE = "timeout";
    private static final String LOG_REQUESTS_PREFERENCE = "logRequests";
    private static final String LOG_RESPONSES_PREFERENCE = "logResponses";
    private static final String REPEAT_PENALTY_PREFERENCE = "repeatPenalty";
    private static final String ORGANIZATION_ID_PREFERENCE = "organizationId";
    private static final String TOP_K_PREFERENCE = "topK";
    private static final String MAX_TOKENS_PREFERENCE = "maxTokens";
    private static final String MAX_COMPLETION_TOKENS_PREFERENCE = "maxCompletionTokens";
    private static final String MAX_OUTPUT_TOKENS_PREFERENCE = "maxOutputTokens";
    private static final String PRESENCE_PENALTY_PREFERENCE = "presencePenalty";
    private static final String FREQUENCY_PENALTY_PREFERENCE = "frequencyPenalty";
    private static final String SEED_PREFERENCE = "seed";
    private static final String ALLOW_CODE_EXECUTION_PREFERENCE = "allowCodeExecution";
    private static final String INCLUDE_CODE_EXECUTION_OUTPUT_PREFERENCE = "includeCodeExecutionOutput";
    private static final String MAX_RETRIES_PREFERENCE = "maxRetries";
    private static final String TOKEN_GRANULARITY_KEY = "tokenGranularity";
    private static final String LAST_BROWSE_DIRECTORY_PREFERENCE = "lastBrowseDirectory";

    private final List<String> DEFAULT_ACCEPTED_EXTENSIONS = Arrays.asList(
            "java", "php", "jsf", "kt", "groovy", "scala", "xml", "json", "yaml", "yml",
            "properties", "txt", "md", "js", "ts", "css", "scss", "html", "xhtml", "sh",
            "bat", "sql", "jsp", "rb", "cs", "go", "swift", "rs", "c", "cpp", "h", "py"
    );
    private final List<String> EXCLUDE_DIR_DEFAULT = Arrays.asList(
            // Test Resources
            "src/test/java",
            "src/test/resources",
            "test",
            // Main Resources
//            "src/main/resources",
//            "src/main/webapp",
            // Build Directories
            "target",
            "build",
            "out", // Output directory for compiled files

            // IDE Specific Directories
            ".idea",
            ".vscode",
            ".settings",
            ".classpath", // Eclipse classpath file
            ".project", // Eclipse project file
            "nbproject",
            "nbactions.xml",
            "nb-configuration.xml",
            // Version Control Directories
            ".git",
            ".svn",
            // Temporary Directories
            "tmp",
            "temp",
            // Log Directories
            "logs",
            "log",
            "debug",
            "trace",
            "cache",
            "backup",
            // Other Configuration/Files
            ".env", // Environment variable definitions
            "docker-compose.yml", // Docker Compose config
            "Dockerfile", // Docker configuration

            // JavaScript Project Directories
            "node_modules",
            "dist",
            "public",
            "build",
            ".next",
            // Gradle
            "gradle",
            "gradlew",
            "gradlew.bat",
            // Github
            ".github",
            ".dependabot",
            ".gitignore",
            "CODE_OF_CONDUCT.md",
            "CONTRIBUTING.md",
            "LICENSE",
            // Security and Configuration Files
            "secrets", // Directory for secrets
            "credentials", // Directory for credentials
            "private",
            "confidential",
            "vault",
            // CI configuration
            ".gitlab-ci.yml",
            ".travis.yml",
            "azure-pipelines.yml",
            "Jenkinsfile"
    );
    private static final String PROMPT_RESOURCE_PATH = "/templates/prompts/";
    private List<String> acceptedExtensions = Collections.EMPTY_LIST;
    private List<String> excludeDir = Collections.EMPTY_LIST;
    private Map<String, String> userPrompts = new LinkedHashMap<>();
    private final Map<String, String> systemPrompts = new LinkedHashMap<>();
    private Map<String, String> headerKeyValueMap = new HashMap<>();
    private TokenGranularity tokenGranularity;

    private PreferencesManager() {
        preferences = new FilePreferences();
    }

    private static PreferencesManager instance;

    public static PreferencesManager getInstance() {
        if (instance == null) {
            synchronized (PreferencesManager.class) {
                if (instance == null) {
                    instance = new PreferencesManager();
                }
            }
        }
        return instance;
    }

    public void exportPreferences(String filePath) throws IOException {
            preferences.exportPreferences(filePath);
    }

    public void importPreferences(String filePath) throws Exception {
        preferences.importPreferences(filePath);
    }

    public void clearApiKey() {
        preferences.remove(getProvider().name() + API_KEY_PREFERENCES);
    }

    public void setApiKey(String key) {
        preferences.put(getProvider().name() + API_KEY_PREFERENCES, key);
    }

    public String getApiKey() {
        return getApiKey(false);
    }

    String getApiKey(GenAIProvider provider) {
        return preferences.get(provider.name() + API_KEY_PREFERENCES, null);
    }

    // TODO: P3 - PreferencesManger should not provide UI
    public String getApiKey(boolean headless) {
        // First, try to get the API key from the environment variable
        String apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment variable, try system properties
            apiKey = System.getProperty(API_KEY_SYS_PROP);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment or system properties, check Preferences
            apiKey = preferences.get(getProvider().name() + API_KEY_PREFERENCES, null);
        }

        if (apiKey == null || apiKey.isEmpty()) {
            // If still not found, show input dialog to enter API key
            if (!headless) {
                apiKey = JOptionPane.showInputDialog(null,
                        getProvider().name() + ":" + getModelName() + " API key is not configured. Please enter it now.",
                        getProvider().name() + ":" + getModelName() + " API Key Required",
                        JOptionPane.WARNING_MESSAGE);
            }

            if (apiKey != null && !apiKey.isEmpty()) {
                // Save the entered API key in Preferences for future use
                preferences.put(getProvider().name() + API_KEY_PREFERENCES, apiKey);
            } else {
                if (!headless) {
                    // If user didn't provide a valid key, show error and throw exception
                    JOptionPane.showMessageDialog(null,
                            getProvider().name() + ":" + getModelName() + " API key setup is incomplete. Please provide a valid key.",
                            getProvider().name() + ":" + getModelName() + " API Key Not Configured",
                            JOptionPane.ERROR_MESSAGE);
                    throw new IllegalStateException("A valid OpenAI API key is necessary for this feature.");
                } else {
                    return null;
                }
            }
        }

        return apiKey;
    }

    public void setProviderLocation(String providerLocation) {
        preferences.put(getProvider().name() + PROVIDER_LOCATION_PREFERENCES, providerLocation);
    }

    public String getProviderLocation() {
        return getProviderLocation(getProvider());
    }

     public String getProviderLocation(GenAIProvider provider) {
        return preferences.get(provider.name() + PROVIDER_LOCATION_PREFERENCES, null);
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
            modelName = getModel();
        }
        return modelName;
    }

    public boolean isAiAssistantActivated() {
        return preferences.getBoolean("aiAssistantActivated", true);
    }

    public void setAiAssistantActivated(boolean activated) {
        preferences.putBoolean("aiAssistantActivated", activated);
    }

    public AIClassContext getClassContextInlineHint() {
        String classContext = preferences.get("classContextInlineHint", null);
        if (classContext != null) {
            try {
                return AIClassContext.valueOf(classContext);
            } catch (IllegalArgumentException iae) {
                // .. skip
            }
        }
        return AIClassContext.REFERENCED_CLASSES;
    }

    public void setClassContextInlineHint(AIClassContext context) {
        preferences.put("classContextInlineHint", context != null ? context.name() : null);
    }

    public AIClassContext getClassContext() {
        String classContext = preferences.get("classContext", null);
        if (classContext != null) {
            try {
                return AIClassContext.valueOf(classContext);
            } catch (IllegalArgumentException iae) {
                // .. skip
            }
        }
        return AIClassContext.REFERENCED_CLASSES;
    }

    public void setClassContext(AIClassContext context) {
        preferences.put("classContext", context != null ? context.name() : null);
    }

    public AIClassContext getVarContext() {
        String classContext = preferences.get("varContext", null);
        if (classContext != null) {
            try {
                return AIClassContext.valueOf(classContext);
            } catch (IllegalArgumentException iae) {
                // .. skip
            }
        }
        return AIClassContext.CURRENT_CLASS;
    }

    public void setVarContext(AIClassContext context) {
        preferences.put("varContext", context != null ? context.name() : null);
    }

    public String getModel() {
        return preferences.get(MODEL_PREFERENCE, DEFAULT_MODEL);
    }

    public void setModel(String model) {
        preferences.put(MODEL_PREFERENCE, model);
    }
    
    public void setModelList(List<String> models) {
        JSONArray jsonArray = new JSONArray();
        for (String model : models) {
            jsonArray.put(model);
        }
        preferences.put(MODEL_LIST, jsonArray.toString());
    }

    public List<String> getModelList() {
        String jsonString = preferences.get(MODEL_LIST, "[\"" + getModel() + "\"]");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<String> models = new ArrayList();
        for (int i = 0; i < jsonArray.length(); i++) {
            models.add(jsonArray.getString(i));
        }
        return models;
    }
    
    public String getChatModel() {
        return preferences.get(CHAT_MODEL_PREFERENCE, getModel());
    }

    public void setChatModel(String model) {
        preferences.put(CHAT_MODEL_PREFERENCE, model);
    }
    
    public void setChatModelList(List<String> models) {
        JSONArray jsonArray = new JSONArray();
        for (String model : models) {
            jsonArray.put(model);
        }
        preferences.put(CHAT_MODEL_LIST, jsonArray.toString());
    }

    public List<String> getChatModelList() {
        String jsonString = preferences.get(CHAT_MODEL_LIST, "[\"" + getChatModel() + "\"]");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<String> models = new ArrayList();
        for (int i = 0; i < jsonArray.length(); i++) {
            models.add(jsonArray.getString(i));
        }
        return models;
    }

    public void setProvider(GenAIProvider provider) {
        if (provider != null) {
            preferences.put(PROVIDER_PREFERENCE, provider.name());
        }
    }

    public GenAIProvider getProvider() {
        String providerName = preferences.get(PROVIDER_PREFERENCE, null);
        if (providerName != null) {
            try {
                return GenAIProvider.valueOf(providerName);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown provider: " + providerName + ". Falling back to default.");
            }
        }
        return GenAIProvider.OPEN_AI;
    }

    public boolean isInlineHintEnabled() {
        return preferences.getBoolean("enableInlineHint", false);
    }

    public void setInlineHintEnabled(boolean enabled) {
        preferences.putBoolean("enableInlineHint", enabled);
        setInlineHintsEnabled(isInlineHintEnabled() || isInlinePromptHintEnabled());
    }

    public boolean isInlinePromptHintEnabled() {
        return preferences.getBoolean("enableInlinePromptHint", false);
    }

    public void setInlinePromptHintEnabled(boolean enabled) {
        preferences.putBoolean("enableInlinePromptHint", enabled);
        setInlineHintsEnabled(isInlineHintEnabled() || isInlinePromptHintEnabled());
    }

    private static final String JAVA_INLINE_HINTS_KEY = "enable.inline.hints";

    public static boolean isInlineHintsEnabled() {
        return MimeLookup.getLookup("").lookup(Preferences.class)
                .getBoolean(JAVA_INLINE_HINTS_KEY, false);
    }

    public static void setInlineHintsEnabled(boolean enabled) {
        MimeLookup.getLookup("").lookup(Preferences.class)
                .putBoolean(JAVA_INLINE_HINTS_KEY, enabled);
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

    public boolean isCompletionAllQueryType() {
        return preferences.getBoolean("enableCompletionAllQueryType", true);
    }

    public void setCompletionAllQueryType(boolean enabled) {
        preferences.putBoolean("enableCompletionAllQueryType", enabled);
    }

    public boolean isDescriptionEnabled() {
        return preferences.getBoolean("showDecription", true);
    }

    public void setDescriptionEnabled(boolean enabled) {
        preferences.putBoolean("showDecription", enabled);
    }

    public boolean isExcludeJavadocEnabled() {
        return preferences.getBoolean("excludeJavadoc", true);
    }

    public void setExcludeJavadocEnabled(boolean enabled) {
        preferences.putBoolean("excludeJavadoc", enabled);
    }


    public String getChatPlacement() {
        return preferences.get("chatPlacement", "Right");
    }

    public void setChatPlacement(String placement) {
        preferences.put("chatPlacement", placement);
    }

    public String getSubmitShortcut() {
        // Default to "Ctrl + Enter" if user has not set any preference
        return preferences.get("submitShortcut", "Ctrl + Enter");
    }

    public void setSubmitShortcut(String shortcut) {
        preferences.put("submitShortcut", shortcut);
    }

    public int getConversationContext() {
        // Default = 3 (Last 3 replies)
        return preferences.getInt("conversationContext", 3);
    }

    public void setConversationContext(int contextValue) {
        preferences.putInt("conversationContext", contextValue);
    }

    public void setFileExtensionToInclude(String exts) {
        if (exts != null) {
            String[] fileExtensionToInclude = exts.split("\\s*,\\s*");
            JSONArray array = preferences.getChildArray("fileExtensionToInclude", DEFAULT_ACCEPTED_EXTENSIONS);
            array.clear();
            for (String ext : fileExtensionToInclude) {
                array.put(ext);
            }
            acceptedExtensions = Arrays.asList(fileExtensionToInclude);
        }
    }

    public List<String> getFileExtensionListToInclude() {
        if (acceptedExtensions.isEmpty()) {
            acceptedExtensions = preferences.getChildList("fileExtensionToInclude", DEFAULT_ACCEPTED_EXTENSIONS);
        }
        return acceptedExtensions;
    }

    public void setExcludeDirs(String dirs) {
        if (dirs != null) {
            String[] excludeDirs = dirs.split("\\s*,\\s*");
            JSONArray array = preferences.getChildArray("excludeDirs", EXCLUDE_DIR_DEFAULT);
            array.clear();
            for (String dir : excludeDirs) {
                array.put(dir);
            }
            excludeDir = Arrays.asList(excludeDirs);
        }
    }

    public List<String> getExcludeDirs() {
        if (excludeDir.isEmpty()) {
            excludeDir = preferences.getChildList("excludeDirs", EXCLUDE_DIR_DEFAULT);
        }
        return excludeDir;
    }


        public synchronized Map<String, String> getCustomHeaders() {
        String nodeKey = "customHeaders";
        if (headerKeyValueMap.isEmpty()) {
            JSONObject prefPrompts = preferences.getChild(nodeKey);
            if (!prefPrompts.isEmpty()) {
                for (String key : prefPrompts.keySet()) {
                    String value = prefPrompts.getString(key);
                    headerKeyValueMap.put(key, value);
                }
            }
        }
        return headerKeyValueMap;
    }

    public void setCustomHeaders(Map<String, String> map) {
        String nodeKey = "customHeaders";
        JSONObject prefPrompts = preferences.getChild(nodeKey);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            prefPrompts.put(entry.getKey(), entry.getValue());
        }
        preferences.save();
        headerKeyValueMap = map;
    }


    /**
     * Loads all default prompts from resource files in /prompts/
     * Make sure you have these files in src/main/resources/prompts/
     */
    public Map<String, String> getSystemPrompts() {
        if(!systemPrompts.isEmpty()) {
            return systemPrompts;
        }
        String[] promptKeys = {"test", "getset", "rest", "openapi", "codereview"};

        for (String key : promptKeys) {
            String content = loadPromptFromResource(key + ".txt");
            if (content != null && !content.isBlank()) {
                systemPrompts.put(key, content);
            }
        }
        return systemPrompts;
    }

    private String loadPromptFromResource(String resourceFileName) {
        try (InputStream is = getClass().getResourceAsStream(PROMPT_RESOURCE_PATH + resourceFileName)) {
            if (is == null) {
                System.err.println("Prompt resource not found: " + PROMPT_RESOURCE_PATH + resourceFileName);
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Returns the prompts map, merged from user preferences and restored
     * defaults if deleted. Saves merged prompts back to preferences if
     * restoration happened.
     */
    public synchronized Map<String, String> getPrompts() {
        String nodeKey = "prompts";
        if (userPrompts.isEmpty()) {
            // Load existing prompts from the preferences
            JSONObject prefPrompts = preferences.getChild(nodeKey);
            if (!prefPrompts.isEmpty()) {
                for (String key : prefPrompts.keySet()) {
                    String value = prefPrompts.getString(key);
                    userPrompts.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8));
                }
            }
        }

        // Check if system prompts need to be added
        boolean changed = false;
        for (Map.Entry<String, String> entry : getSystemPrompts().entrySet()) {
            String key = entry.getKey();
            if (!userPrompts.containsKey(key) || userPrompts.get(key).isBlank()) {
                preferences.putChild(nodeKey, key, entry.getValue());
                changed = true;
            }
        }

        if(changed) {
            preferences.save();
        }
        return userPrompts;
    }

    public void setPrompts(Map<String, String> map) {
        String nodeKey = "prompts";
        JSONObject prefPrompts = preferences.getChild(nodeKey);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            prefPrompts.put(entry.getKey(), URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        preferences.save();
        userPrompts = map;
    }

    public String getGlobalRules() {
        // First check for old key "systemMessage"
        String oldValue = preferences.get("systemMessage", null);
        if (oldValue != null) {
            // Migrate value to "globalRules"
            preferences.put(GLOBAL_RULES_PREFERENCE, oldValue);
            preferences.remove("systemMessage");
            return oldValue;
        }
        // Fallback to new key
        return preferences.get(GLOBAL_RULES_PREFERENCE, null);
    }

    public void setGlobalRules(String message) {
        preferences.put(GLOBAL_RULES_PREFERENCE, message);
        // Remove old key to avoid duplication
        preferences.remove("systemMessage");
    }

    public String getProjectRules(Project project) {
        return preferences.get(project.getProjectDirectory().getName() + "-" + PROJECT_RULES_PREFERENCE, null);
    }

    public void setProjectRules(Project project, String message) {
        preferences.put(project.getProjectDirectory().getName() + "-" + PROJECT_RULES_PREFERENCE, message);
    }
    
    /**
     * Get the build command for the given project.
     * <p>
     * Preference is checked first. If not present, fallback to project type: -
     * Maven: uses wrapper if available (`./mvnw clean install`) or `mvn clean
     * install` - Gradle: uses wrapper if available (`./gradlew build`) or
     * `gradle build` - Ant: `ant clean build`
     *
     * @param project the NetBeans project
     * @return the build command, or null if unknown
     */
    public String getBuildCommand(Project project) {
        return getCommand(project, BUILD_COMMAND_PREFERENCE, "build");
    }

    /**
     * Get the test command for the given project.
     * <p>
     * Preference is checked first. If not present, fallback to project type: -
     * Maven: uses wrapper if available (`./mvnw test`) or `mvn test` - Gradle:
     * uses wrapper if available (`./gradlew test`) or `gradle test` - Ant: `ant
     * test`
     *
     * @param project the NetBeans project
     * @return the test command, or null if unknown
     */
    public String getTestCommand(Project project) {
        return getCommand(project, TEST_COMMAND_PREFERENCE, "test");
    }

    /**
     * Common helper for determining build/test command.
     *
     * @param project the NetBeans project
     * @param preferenceKey the preference key suffix (build/test)
     * @param commandType either "build" or "test"
     * @return the command string, or null if unknown
     */
    private String getCommand(Project project, String preferenceKey, String commandType) {
        String key = project.getProjectDirectory().getName() + "-" + preferenceKey;
        String cmd = preferences.get(key, null);
        if (cmd != null) {
            return cmd;
        }

        try {
            // Maven
            if (project.getProjectDirectory().getFileObject("pom.xml") != null) {
                if( commandType.equals("build")) {
                     commandType = "install";
                }
                if (project.getProjectDirectory().getFileObject("mvnw") != null) {
                    return "./mvnw " + commandType;
                }
                return "mvn " + commandType;
            }

            // Gradle
            if (project.getProjectDirectory().getFileObject("build.gradle") != null
                    || project.getProjectDirectory().getFileObject("build.gradle.kts") != null) {
                if (project.getProjectDirectory().getFileObject("gradlew") != null) {
                    return "./gradlew " + commandType;
                }
                return "gradle " + commandType;
            }

            // Ant
            if (project.getProjectDirectory().getFileObject("build.xml") != null) {
                return "ant " + commandType;
            }

        } catch (Exception e) {
            // Fail silently
        }

        return null;
    }

    public void setBuildCommand(Project project, String command) {
        preferences.put(project.getProjectDirectory().getName() + "-" + BUILD_COMMAND_PREFERENCE, command);
    }

    public void setTestCommand(Project project, String command) {
        preferences.put(project.getProjectDirectory().getName() + "-" + TEST_COMMAND_PREFERENCE, command);
    }
    
    public String getAssistantAction() {
        return preferences.get(ASSISTANT_ACTION_PREFERENCE, "");
    }

    public void setAssistantAction(String action) {
        if (action != null) {
            preferences.put(ASSISTANT_ACTION_PREFERENCE, action.trim());
        }
    }

    public String getSessionRules() {
        return preferences.get(SESSION_RULES_PREFERENCE, "");
    }

    public void setSessionRules(String rules) {
        if (rules != null) {
            preferences.put(SESSION_RULES_PREFERENCE, rules.trim());
        }
    }

    public boolean isStreamEnabled() {
        return preferences.getBoolean(STREAM_PREFERENCE, true); // Default value
    }

    public void setStreamEnabled(boolean enabled) {
        preferences.putBoolean(STREAM_PREFERENCE, enabled);
    }

    public Double getTemperature() {
        return preferences.getDouble(TEMPERATURE_PREFERENCE, Double.MIN_VALUE);
    }

    public void setTemperature(Double temperature) {
        preferences.putDouble(TEMPERATURE_PREFERENCE, temperature);
    }

    public Double getTopP() {
        return preferences.getDouble(TOP_P_PREFERENCE, Double.MIN_VALUE);
    }

    public void setTopP(Double topP) {
        preferences.putDouble(TOP_P_PREFERENCE, topP);
    }

    public Integer getTimeout() {
        return preferences.getInt(TIMEOUT_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setTimeout(Integer timeout) {
        preferences.putInt(TIMEOUT_PREFERENCE, timeout);
    }

    public boolean isLogRequestsEnabled() {
        return preferences.getBoolean(LOG_REQUESTS_PREFERENCE, false);
    }

    public void setLogRequestsEnabled(boolean enabled) {
        preferences.putBoolean(LOG_REQUESTS_PREFERENCE, enabled);
    }

    public boolean isLogResponsesEnabled() {
        return preferences.getBoolean(LOG_RESPONSES_PREFERENCE, false);
    }

    public void setLogResponsesEnabled(boolean enabled) {
        preferences.putBoolean(LOG_RESPONSES_PREFERENCE, enabled);
    }

    public Double getRepeatPenalty() {
        return preferences.getDouble(REPEAT_PENALTY_PREFERENCE, Double.MIN_VALUE);
    }

    public void setRepeatPenalty(Double repeatPenalty) {
        preferences.putDouble(REPEAT_PENALTY_PREFERENCE, repeatPenalty);
    }

    public String getOrganizationId() {
        return preferences.get(ORGANIZATION_ID_PREFERENCE, null);
    }

    public void setOrganizationId(String organizationId) {
        preferences.put(ORGANIZATION_ID_PREFERENCE, organizationId);
    }

    public Integer getTopK() {
        return preferences.getInt(TOP_K_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setTopK(Integer topK) {
        preferences.putInt(TOP_K_PREFERENCE, topK);
    }

    public Integer getMaxTokens() {
        return preferences.getInt(MAX_TOKENS_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setMaxTokens(Integer maxTokens) {
        preferences.putInt(MAX_TOKENS_PREFERENCE, maxTokens);
    }

    public Integer getMaxCompletionTokens() {
        return preferences.getInt(MAX_COMPLETION_TOKENS_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        preferences.putInt(MAX_COMPLETION_TOKENS_PREFERENCE, maxCompletionTokens);
    }

    public Integer getMaxOutputTokens() {
        return preferences.getInt(MAX_OUTPUT_TOKENS_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        preferences.putInt(MAX_OUTPUT_TOKENS_PREFERENCE, maxOutputTokens);
    }

    public Double getPresencePenalty() {
        return preferences.getDouble(PRESENCE_PENALTY_PREFERENCE, Double.MIN_VALUE);
    }

    public void setPresencePenalty(Double presencePenalty) {
        preferences.putDouble(PRESENCE_PENALTY_PREFERENCE, presencePenalty);
    }

    public Double getFrequencyPenalty() {
        return preferences.getDouble(FREQUENCY_PENALTY_PREFERENCE, Double.MIN_VALUE);
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        preferences.putDouble(FREQUENCY_PENALTY_PREFERENCE, frequencyPenalty);
    }

    public Integer getSeed() {
        return preferences.getInt(SEED_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setSeed(Integer seed) {
        preferences.putInt(SEED_PREFERENCE, seed);
    }

    public boolean isAllowCodeExecution() {
        return preferences.getBoolean(ALLOW_CODE_EXECUTION_PREFERENCE, false);
    }

    public void setAllowCodeExecution(boolean allowCodeExecution) {
        preferences.putBoolean(ALLOW_CODE_EXECUTION_PREFERENCE, allowCodeExecution);
    }

    public boolean isIncludeCodeExecutionOutput() {
        return preferences.getBoolean(INCLUDE_CODE_EXECUTION_OUTPUT_PREFERENCE, false);
    }

    public void setIncludeCodeExecutionOutput(boolean includeCodeExecutionOutput) {
        preferences.putBoolean(INCLUDE_CODE_EXECUTION_OUTPUT_PREFERENCE, includeCodeExecutionOutput);
    }

    public Integer getMaxRetries() {
        return preferences.getInt(MAX_RETRIES_PREFERENCE, Integer.MIN_VALUE);
    }

    public void setMaxRetries(Integer maxRetries) {
        preferences.putInt(MAX_RETRIES_PREFERENCE, maxRetries);
    }

    public TokenGranularity getTokenGranularity() {
        if (tokenGranularity != null) {
            return tokenGranularity;
        }
        tokenGranularity = TokenGranularity.valueOf(preferences.get(TOKEN_GRANULARITY_KEY, TokenGranularity.DAY.name()));
        return tokenGranularity;
    }

    public void setTokenGranularity(TokenGranularity granularity) {
        this.tokenGranularity = granularity;
        preferences.put(TOKEN_GRANULARITY_KEY, granularity.name());
    }

    public String getLastBrowseDirectory() {
        return preferences.get(LAST_BROWSE_DIRECTORY_PREFERENCE, "");
    }

    public void setLastBrowseDirectory(String directory) {
        preferences.put(LAST_BROWSE_DIRECTORY_PREFERENCE, directory);
    }

}

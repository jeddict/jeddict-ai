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
package io.github.jeddict.ai.lang;

/**
 *
 * @author Shiwani Gupta
 */
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.Assistant;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.response.TokenHandler;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.MimeUtil.MIME_TYPE_DESCRIPTIONS;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import io.github.jeddict.ai.util.Utilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.project.Project;

//
// TODO: shall we combine the processing in using always StreamingModels
// providing an adapter for not streaming mode?
//
public class JeddictBrain {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    /**
     * property notified for changes: # of input tokens
     */
    public static final String PROPERTY_TOKENS = "tokens";
    public static final String PROPERTY_ERROR = "error";

    public final Optional<StreamingChatResponseHandler> streamHandler;
    public final Optional<ChatModel> chatModel;
    public final Optional<StreamingChatModel> streamingChatModel;
    protected final List<AbstractTool> tools;

    public final String modelName;

    protected final PropertyChangeSupport progressListeners = new PropertyChangeSupport(this);

    private static final String jsonRequest = """
    Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'.
    'imports' should be an array of required Java import statements (if no imports are required, return an empty array).
    'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks.
    Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid.

    """;

    private static final String singleJsonRequest = """
    Return a JSON object with a single best suggestion without any additional text or explanation. The object should contain two fields: 'imports' and 'snippet'.
    'imports' should be an array of required Java import statements (if no imports are required, return an empty array).
    'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks.
    Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid.

    """;

    private static final String jsonRequestWithDescription = """
    Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing three fields: 'imports', 'snippet', and 'description'.
    'imports' should be an array of required Java import statements (if no imports are required, return an empty array).
    'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks.
    'description' should be a very short explanation of what the snippet does and why it might be appropriate in this context, formatted with <b>, <br> and optionally, if required, include any important link with <a href=''> tags.
    Make sure to escape any double quotes within the snippet and description using a backslash (\\) so that the JSON remains valid.

    """;

    public JeddictBrain() {
        this.streamHandler = Optional.empty();
        this.streamingChatModel = Optional.empty();
        this.chatModel = Optional.empty();
        this.modelName = null;
        this.tools = List.of();
    }

    public JeddictBrain(
        final StreamingChatResponseHandler handler,
        final boolean streaming
    ) {
        this("", handler, streaming, List.of());
    }

    public JeddictBrain(
        final String modelName,
        final StreamingChatResponseHandler handler,
        final boolean streaming,
        final List<AbstractTool> tools
    ) {
        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }
        this.modelName = modelName;
        this.streamHandler = (handler != null) ? Optional.of(handler) : Optional.empty();

        final JeddictChatModelBuilder builder =
            new JeddictChatModelBuilder(handler, this.modelName);

        if (streaming && handler != null) {
            this.streamingChatModel = Optional.of(builder.buildStreaming());
            this.chatModel = Optional.empty();
        } else {
            this.chatModel = Optional.of(builder.build());
            this.streamingChatModel = Optional.empty();
        }
        this.tools = (tools != null)
                   ? List.of(tools.toArray(new AbstractTool[0])) // immutable
                   : List.of();
    }

    public String generate(final Project project, final String prompt) {
        return generateInternal(project, false, prompt, null, null);
    }

    public String generate(final Project project, final String prompt, List<String> images, List<Response> responseHistory) {
        return generateInternal(project, false, prompt, images, responseHistory);
    }

    public String generate(final Project project, boolean agentEnabled, final String prompt) {
        return generateInternal(project, agentEnabled, prompt, null, null);
    }

    public String generate(final Project project, boolean agentEnabled, final String prompt, List<String> images, List<Response> responseHistory) {
        return generateInternal(project, agentEnabled, prompt, images, responseHistory);
    }

    public UserMessage buildUserMessage(String prompt, List<String> imageBase64Urls) {
        List<Content> parts = new ArrayList<>();

        // Add the prompt text
        parts.add(new TextContent(prompt));

        // Add each image as ImageContent
        for (String imageUrl : imageBase64Urls) {
            parts.add(new ImageContent(imageUrl));
        }

        // Convert list to varargs
        return UserMessage.from(parts.toArray(new Content[0]));
    }

    //
    // TODO: P2 - StreamingChatResponseHandler shall be created in both
    //       streaming or not streaming chats; in the case of streaming
    //       onPartialResponse will be called, otherwise only onCompleteResponse
    //       and onError
    //       This method should then not return any value.
    //
    // TODO: P3 - better use of langchain4j functionalities (see https://docs.langchain4j.dev/tutorials/agents)
    //
    private String generateInternal(Project project, boolean agentEnabled, String prompt, List<String> images, List<Response> responseHistory) {
        if (chatModel.isEmpty() && streamHandler.isEmpty()) {
            throw new IllegalStateException("AI assistant model not intitalized, this looks like a bug!");
        }

        if (project != null) {
            prompt = prompt + "\n" + ProjectMetadataInfo.get(project);
        }
        String systemMessage = null;
        String globalRules = PreferencesManager.getInstance().getGlobalRules();
        if (globalRules != null) {
            systemMessage = globalRules;
        }
        if (project != null) {
            String projectRules = PreferencesManager.getInstance().getProjectRules(project);
            if (projectRules != null) {
                systemMessage = systemMessage + '\n' + projectRules;
            }
        }
        List<ChatMessage> messages = new ArrayList<>();
        if (systemMessage != null && !systemMessage.trim().isEmpty()) {
            messages.add(SystemMessage.from(systemMessage));
        }

        // add conversation history (multiple responses)
        if (responseHistory != null && !responseHistory.isEmpty()) {
            for (Response res : responseHistory) {
                messages.add(UserMessage.from(res.getQuery()));
                messages.add(AiMessage.from(res.toString()));
            }
        }

        if (images != null && !images.isEmpty()) {
            messages.add(buildUserMessage(prompt, images));
        } else {
            messages.add(UserMessage.from(prompt));
        }
        //
        // TODO: P3 - decouple token counting from saving stats; saving stats should listen to this event
        //
        progressListeners.firePropertyChange(PROPERTY_TOKENS, 0, TokenHandler.saveInputToken(messages));

        final StringBuilder response = new StringBuilder();
        try {

            if (streamingChatModel.isPresent()) {
                final StreamingChatResponseHandler handler = streamHandler.get();
                if(agentEnabled) {
                    final Assistant assistant = AiServices.builder(Assistant.class)
                        .streamingChatModel(streamingChatModel.get())
                        .tools(tools.toArray())
                        .build();

                    final TokenStream tokenStream = assistant.stream(messages);
                    tokenStream
                        .onCompleteResponse(partial -> {
                            handler.onCompleteResponse(partial);
                        })
                        .onPartialResponse(partial -> {
                            handler.onPartialResponse(partial);
                        })
                        .onError(error -> {
                            handler.onError(error);
                        })
                        .start();
                } else {
                    streamingChatModel.get().chat(messages, handler);
                }
            } else {
                ChatModel model = chatModel.get();
                if (agentEnabled) {
                    Assistant assistant = AiServices.builder(Assistant.class)
                            .chatModel(model)
                            .tools(tools.toArray())
                            .build();
                    response.append(assistant.chat(messages).aiMessage().text());
                } else {
                    response.append(model.chat(messages).aiMessage().text());
                }

                CompletableFuture.runAsync(() -> TokenHandler.saveOutputToken(response.toString()));
            }
        } catch (Exception x) {
            LOG.finest(() -> "Communication error: " + x.getMessage());
            response.append(Utilities.errorHTMLBlock(x));
            //
            // TODO: P2 - remove this and use onError
            //
            progressListeners.firePropertyChange(PROPERTY_ERROR, null, x);
        }

        LOG.finest(() -> "Returning " + response);

        return response.toString();
    }

    public String generateJavadocForClass(Project project, String classContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for class not the member of class. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java class not the member of class. Do not include any additional text or explanation.\n\n"
                + classContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String generateJavadocForMethod(Project project, String methodContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for method. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java method. Do not include any additional text or explanation.\n\n"
                + methodContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String generateJavadocForField(Project project, String fieldContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for field. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java variable. Do not include any additional text or explanation.\n\n"
                + fieldContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceJavadocForClass(Project project, String existingJavadoc, String classContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a class. "
                + "Given the existing Javadoc comment and the following Java class, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Class Content:\n" + classContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceJavadocForMethod(Project project, String existingJavadoc, String methodContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a method. "
                + "Given the existing Javadoc comment and the following Java method, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Method Content:\n" + methodContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceJavadocForField(Project project, String existingJavadoc, String fieldContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a field. "
                + "Given the existing Javadoc comment and the following Java field, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Field Content:\n" + fieldContent;
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String generateRestEndpointForClass(Project project, String classContent) {
        // Define a prompt to generate unique JAX-RS resource methods with necessary imports
        String prompt = """
        You are an API server that generates JAX-RS REST endpoints based on the provided Java class definition. \
        Analyze the context and functionality of the class to create meaningful and relevant REST endpoints. \
        Ensure that you create new methods for various HTTP operations (GET, POST, PUT, DELETE) and include all necessary imports for JAX-RS annotations and responses. \
        The generated methods should have some basic implementation and should not be empty. Avoid duplicating existing methods from the class content. \
        Format the output as a JSON object with two fields: 'imports' as array (list of necessary imports) and 'methodContent' as text. \
        Include all required imports such as Response, GET, POST, PUT, DELETE, Path, etc. \
        Example output:
        {
          "imports": [
            "jakarta.ws.rs.GET",
            "jakarta.ws.rs.POST",
            "jakarta.ws.rs.core.Response"
          ],
          "methodContent": "@GET public Response getPing() { // implementation }@POST public Response createPing() { // implementation for createPing }@PUT public Response updatePing() { // implementation }@DELETE public Response deletePing() { // implementation for deletePing }"
        }

        Only return methods with annotations, implementation details, and necessary imports for the given class. \
        Do not include class declarations, constructors, or unnecessary boilerplate code. Ensure the generated methods are unique and not duplicates of existing methods in the class content.

        """ + classContent;

        // Generate the unique JAX-RS methods with imports
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String updateMethodFromDevQuery(Project project, String javaClassContent, String methodContent, String developerRequest) {
        String prompt = """
            You are an API server that enhances Java methods based on user requests.
            Given the following Java method and the developer's request, modify and enhance the method accordingly.
            Incorporate any specific details or requirements mentioned by the developer. Do not include any additional text or explanation, just return the enhanced Java method source code.

            Include all necessary imports relevant to the enhanced or newly created method.
            Return only the Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code.
            Do not include full java class, any additional text or explanation, just the imports and the method source code.

            Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'.
            Developer Request:
            """ + developerRequest + """

            Java Class Content:
            """ + javaClassContent + """

            Java Method Content:
            """ + methodContent;

        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceMethodFromMethodContent(Project project, String javaClassContent, String methodContent) {
        String prompt = """
            You are an API server that enhances or creates Java methods based on the method name, comments, and its content.
            Given the following Java class content and Java method content, modify and enhance the method accordingly.
            Include all necessary imports relevant to the enhanced or newly created method.
            Return only the Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code.
            Do not include full java class, any additional text or explanation, just the imports and the method source code.

            Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'.
            Java Class Content:
            """ + javaClassContent + """

            Java Method Content:
            """ + methodContent;

        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String fixMethodCompilationError(Project project, String javaClassContent, String methodContent, String errorMessage, String classDatas) {
        String prompt = """
            You are an API server that fixes compilation errors in Java methods based on the provided error messages.
            Given the following Java method content, class content, and the error message, correct the method accordingly.
            Ensure that all compilation errors indicated by the error message are resolved.
            Include any necessary imports relevant to the fixed method.
            Return only the corrected Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code.
            Do not include full Java class, any additional text, or explanation—just the imports and the corrected method source code.

            Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'.
            Error Message:
            """ + errorMessage + """

            Java Class Content:
            """ + javaClassContent + """

            Java Method Content:
            """ + methodContent;

        prompt = loadClassData(prompt, classDatas);
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String fixVariableError(Project project, String javaClassContent, String errorMessage, String classDatas) {
        String prompt = """
            You are an API server that fixes variable-related compilation errors in Java classes based on the provided error messages.
            Given the following Java class content and the error message, correct given variable-related issues based on error message at class level.
            Ensure that all compilation errors indicated by the error message, such as undeclared variables, incorrect variable types, or misuse of variables, are resolved.
            Include any necessary imports relevant to the fixed method or class.
            Return only the corrected variable content and its necessary imports, without including any unnecessary boilerplate code.
            Do not include any additional text or explanation—just the imports and the corrected variable source code.

            Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'variableContent' (corrected variable line or content).
            Error Message:
            """ + errorMessage + """

            Java Class Content:
            """ + javaClassContent;

        prompt = loadClassData(prompt, classDatas);
        String response = generate(project, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceVariableName(String variableContext, String methodContent, String classContent) {
        StringBuilder prompt = new StringBuilder("""
            You are an API server that suggests a more meaningful and descriptive name for a specific variable in a given Java class.
            Based on the provided Java class content and the variable context, suggest an improved name for the variable.
            Return only the new variable name. Do not include any additional text or explanation.

            Variable Context:
            """ + variableContext + "\n\n");

        if (methodContent != null) {
            prompt.append("Java Method Content:\n").append(methodContent).append("\n\n");
        }
        if (classContent != null) {
            prompt.append("Java Class Content:\n").append(classContent);
        }

        String response = generate(null, prompt.toString());
        LOG.finest(response);
        return response;
    }

    public List<String> suggestVariableNames(String classDatas, String variablePrefix, String classContent, String variableExpression) {
        String prompt = """
        You are an API server that suggests a list of meaningful and descriptive names for a specific variable in a given Java class.
        Based on the provided Java class content, variable prefix, and variable expression, generate a list of improved names for the variable.
        Return only the list of suggested names, one per line, without any additional text or explanation.

        Variable Prefix: %s

        Variable Expression Line:
        %s

        Java Class Content:
        %s

        Here is the context of all classes in the project, including variable names and method signatures (method bodies are excluded to avoid sending unnecessary code):
        %s
        """.formatted(variablePrefix, variableExpression, classContent, classDatas);

        String response = generate(null, prompt);
        LOG.finest(response);

        // Split the response into a list and return
        return Arrays.asList(response.split("\n"));
    }

    private String loadClassData(String prompt, String classDatas) {
        if (classDatas == null || classDatas.isEmpty()) {
            return prompt;
        }
        prompt += "\n\nHere is the context of all classes in the project, including variable names and method signatures (method bodies are excluded to avoid sending unnecessary code):\n"
                + classDatas;
        return prompt;
    }

    public List<String> suggestVariableNames(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and descriptive names for a specific variable in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved names for the variable represented by the placeholder ${SUGGEST_VAR_NAMES_LIST} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);

        // Generate the list of new variable names
        String jsonResponse = generate(null, prompt);

        // Parse the JSON response into a List
        List<String> variableNames = parseJsonToList(jsonResponse);

        return variableNames;
    }

    public List<String> suggestMethodNames(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and descriptive names for a specific method in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved names for the method represented by the placeholder ${SUGGEST_METHOD_NAMES_LIST} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);
        // Generate the list of new method names
        String jsonResponse = generate(null, prompt);

        // Parse the JSON response into a List
        List<String> methodNames = parseJsonToList(jsonResponse);

        return methodNames;
    }

    public List<String> suggestStringLiterals(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and descriptive string literals for a specific context in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved string literals represented by the placeholder ${SUGGEST_STRING_LITERAL_LIST} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);
        // Generate the list of new string literals
        String jsonResponse = generate(null, prompt);

        // Parse the JSON response into a List
        List<String> stringLiterals = parseJsonToListWithSplit(jsonResponse);

        return stringLiterals;
    }

    public List<String> suggestMethodInvocations(Project project, String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and appropriate method invocations for a specific context in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved method invocations represented by the placeholder ${SUGGEST_METHOD_INVOCATION} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);

        // Generate the list of new method invocations
        String jsonResponse = generate(project, prompt);

        // Parse the JSON response into a List
        List<String> methodInvocations = parseJsonToList(jsonResponse);

        return methodInvocations;
    }

    public List<Snippet> suggestNextLineCode(
        final Project project, final String classDatas, final String classContent,
        final String lineText, final TreePath path, final String hintContext,
        final boolean hint, final boolean description
    ) {
        String prompt;
        if (hint) {
            prompt = "You are an API server that suggests relevant Java code to be inserted at the placeholder ${SUGGEST_CODE}.\n"
                    + "The goal is to generate context-aware, meaningful, and syntactically valid Java code suggestions "
                    + "that naturally fit into the surrounding code to improve code clarity or functionality.\n"
                    + "Do not repeat existing methods or duplicate code that already follows the placeholder.\n"
                    + "Avoid suggesting code that would not compile if inserted exactly where `${SUGGEST_CODE}` is.\n";
            prompt = prompt + "Java Class Content:\n" + classContent
                    + "\n" + singleJsonRequest;
            if (hintContext != null && !hintContext.isEmpty()) {
                prompt = prompt + "\n" + hintContext;
            }
        } else {
            final String descriptionText = (description ? jsonRequestWithDescription : jsonRequest);
            if (path == null) {
                prompt = "You are an API server that suggests Java code for the outermost context of a Java source file, outside of any existing class. "
                        + "Based on the provided Java source file content, suggest relevant code to be added at the placeholder location ${SUGGEST_CODE}. "
                        + "Suggest additional classes, interfaces, enums, or other top-level constructs. "
                        + "Ensure that the suggestions fit the context of the entire file. "
                        + descriptionText
                        + "Java Source File Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                prompt = "You are an API server that suggests Java code for the outermost context of a Java source file, outside of any existing class. "
                        + "Based on the provided Java source file content, suggest relevant code to be added at the placeholder location ${SUGGEST_CODE}. "
                        + "Suggest package declarations, import statements, comments, or annotations for public class. "
                        + "Ensure that the suggestions fit the context of the entire file. "
                        + descriptionText
                        + "Java Source File Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.MODIFIERS
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf().getKind() == Tree.Kind.CLASS) {
                prompt = "You are an API server that suggests Java code modifications for a class. "
                        + "At the placeholder location ${SUGGEST_CODE}, suggest either a class-level modifier such as 'public', 'protected', 'private', 'abstract', 'final', or a relevant class-level annotation. "
                        + "Ensure that the suggestions are appropriate for the class context provided. "
                        + descriptionText
                        + "Java Class Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.MODIFIERS
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf().getKind() == Tree.Kind.METHOD) {
                prompt = "You are an API server that suggests Java code modifications for a method. "
                        + "At the placeholder location ${SUGGEST_CODE}, suggest method-level modifiers such as 'public', 'protected', 'private', 'abstract', 'static', 'final', 'synchronized', or relevant method-level annotations. "
                        + "Additionally, you may suggest method-specific annotations. "
                        + "Ensure that the suggestions are appropriate for the method context provided. "
                        + descriptionText
                        + "Java Method Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.CLASS
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf().getKind() == Tree.Kind.CLASS) {
                prompt = "You are an API server that suggests Java code for an inner class at the placeholder location ${SUGGEST_CODE}. "
                        + "Based on the provided Java class content, suggest either relevant inner class modifiers such as 'public', 'private', 'protected', 'static', 'abstract', 'final', or a full inner class definition. "
                        + "Additionally, you may suggest class-level annotations for the inner class. Ensure that the suggestions are contextually appropriate for an inner class. "
                        + descriptionText
                        + "Java Class Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.CLASS
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                prompt = "You are an API server that suggests Java code for an class at the placeholder location ${SUGGEST_CODE}. "
                        + "Based on the provided Java class content, suggest either relevant class level members, attributes, constants, methods or blocks. "
                        + "Ensure that the suggestions are contextually appropriate for an class. "
                        + descriptionText
                        + "Java Class Content:\n" + classContent;
            } else if (path.getLeaf().getKind() == Tree.Kind.PARENTHESIZED
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf().getKind() == Tree.Kind.IF) {
                prompt = "You are an API server that suggests Java code to enhance an if-statement. "
                        + "At the placeholder location ${SUGGEST_IF_CONDITIONS}, suggest additional conditional checks or actions within the if-statement. "
                        + "Ensure that the suggestions are contextually appropriate for the condition. "
                        + descriptionText
                        + "Java If Statement Content:\n" + classContent;
            } else {
                prompt = "You are an API server that suggests Java code for a specific context in a given Java class at the placeholder location ${SUGGEST_CODE}. "
                        + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a relevant single line of code or a multi-line code block as appropriate for the context represented by the placeholder ${SUGGEST_CODE} in the Java class. "
                        + "Ensure that the suggestions are relevant to the context. "
                        + descriptionText
                        + "Java Class Content:\n" + classContent;
            }
        }
        prompt = loadClassData(prompt, classDatas);

        // Generate the list of suggested next lines of code
        String jsonResponse = generate(project, prompt);
        LOG.finest(() -> "jsonResponse " + jsonResponse);
        // Parse the JSON response into a List
        List<Snippet> nextLines = parseJsonToSnippets(jsonResponse);
        return nextLines;
    }

    public List<Snippet> hintNextLineCode(
        final Project project, final String classDatas, final String classContent,
        final String lineText, final TreePath path, final String hintContext,
        final boolean singleCodeSnippet, final boolean description
    ) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are an API server that suggests relevant Java code at the placeholder ${SUGGEST_CODE}.\n")
                .append("The goal is to generate context-aware, meaningful, and syntactically valid Java code suggestions that fit naturally in the given location.\n");

        if (hintContext != null && !hintContext.isEmpty()) {
            promptBuilder.append("Hint:\n").append(hintContext).append("\n");
        }

        if (lineText != null && !lineText.trim().isEmpty()) {
            promptBuilder.append("Current Line:\n\"").append(lineText).append("\"\n");
        }

        promptBuilder.append("Full Java Context:\n")
                .append(classContent).append("\n");

        // Choose the appropriate JSON request variant
        String request = singleCodeSnippet
                ? singleJsonRequest
                : (description ? jsonRequestWithDescription : jsonRequest);

        promptBuilder.append(request);

        // Replace placeholders with class-related data
        String prompt = loadClassData(promptBuilder.toString(), classDatas);

        // Generate suggestions using the AI model
        String jsonResponse = generate(project, prompt);

        LOG.finest(() -> "jsonResponse " + jsonResponse);

        // Parse and return results
        return parseJsonToSnippets(jsonResponse);
    }

    public List<String> suggestJavaComment(Project project, String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests appropriate Java comments for a specific context in a given Java class at the placeholder location ${SUGGEST_JAVA_COMMENT}. "
                + "Based on the provided Java class content and the line of comment: \"" + lineText + " ${SUGGEST_JAVA_COMMENT} \", suggest relevant Java comment as appropriate for the context represented by the placeholder ${SUGGEST_JAVA_COMMENT} in the Java Class. "
                + "Return a JSON array where each element must be single line comment. \n\n"
                + "Java Class Content:\n" + classContent;
        // Generate the list of suggested Javadoc or comments
        String jsonResponse = generate(project, prompt);

        LOG.finest(() -> "jsonResponse " + jsonResponse);

        // Parse the JSON response into a List
        List<String> comments = parseJsonToList(jsonResponse);

        return comments;
    }

    public List<String> suggestJavadocOrComment(Project project, String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests appropriate Javadoc or comments for a specific context in a given Java class at the placeholder location ${SUGGEST_JAVADOC}. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest relevant Javadoc or a comment block as appropriate for the context represented by the placeholder ${SUGGEST_JAVADOC} in the Java Class. "
                + "Return a JSON array where each element can either be a single-line comment, a multi-line comment block, or a Javadoc comment formatted as a single string using \\n for line breaks. "
                + " Do not split multi line javadoc comments to array, must be at same index in json array. \n\n"
                + "Java Class Content:\n" + classContent;
        // Generate the list of suggested Javadoc or comments
        String jsonResponse = generate(project, prompt);
        LOG.finest(() -> "jsonResponse " + jsonResponse);
        // Parse the JSON response into a List
        List<String> comments = parseJsonToList(jsonResponse);
        return comments;
    }

    public List<Snippet> suggestAnnotations(
        final Project project, final String classDatas, final String classContent,
        final String lineText, final String hintContext,
        final boolean singleCodeSnippet, final boolean description) {
        String prompt;

        boolean hasHint = hintContext != null && !hintContext.isEmpty();
        if (hasHint) {
            prompt = "You are an API server that suggest relevant code for ${SUGGEST_ANNOTATION_LIST} in the given Java class based on the line: "
                    + lineText + "\n\n Class: \n" + classContent + "\n" + singleJsonRequest + "\n"
                    + ((hintContext != null) ? hintContext + "\n" : "");
        } else {
            prompt = "You are an API server that suggests Java annotations for a specific context in a given Java class at the placeholder location ${SUGGEST_ANNOTATION_LIST}. "
                    + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest relevant annotations that can be applied at the placeholder location represented by ${SUGGEST_ANNOTATION_LIST} in the Java Class. "
                    + (description ? jsonRequestWithDescription : jsonRequest)
                    + "Ensure that the suggestions are appropriate for the given Java Class Content:\n\n" + classContent;
        }

        // Generate the list of suggested annotations
        String jsonResponse = generate(project, prompt);

        LOG.finest(() -> "jsonResponse " + jsonResponse);

        // Parse the JSON response into a List
        List<Snippet> annotations = parseJsonToSnippets(jsonResponse);
        return annotations;
    }

    public List<Snippet> parseJsonToSnippets(String jsonResponse) {
        if (jsonResponse == null) {
            return Collections.EMPTY_LIST;
        }
        List<Snippet> snippets = new ArrayList<>();

        JSONArray jsonArray;

        if (jsonResponse.contains("```json")) {
            int index = jsonResponse.indexOf("```json") + 7;
            jsonResponse = jsonResponse.substring(index, jsonResponse.indexOf("```", index)).trim();
        } else {
            jsonResponse = removeCodeBlockMarkers(jsonResponse);
        }
        try {
            // Parse the JSON response
            jsonArray = new JSONArray(jsonResponse);
        } catch (org.json.JSONException jsone) {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            jsonArray = new JSONArray();
            jsonArray.put(jsonObject);
        }

        // Loop through each element in the JSON array
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            List<String> importsList = new ArrayList<>();
            if (jsonObject.has("imports")) {
                // Extract the "imports" array
                JSONArray importsJsonArray = jsonObject.getJSONArray("imports");
                for (int j = 0; j < importsJsonArray.length(); j++) {
                    importsList.add(importsJsonArray.getString(j));
                }
            }

            // Extract the "snippet" field
            String snippet = jsonObject.getString("snippet");
            if (jsonObject.has("description")) {
                String descripion = jsonObject.getString("description");
                Snippet snippetObj = new Snippet(snippet, descripion, importsList);
                snippets.add(snippetObj);
            } else {
                Snippet snippetObj = new Snippet(snippet, importsList);
                snippets.add(snippetObj);
            }
        }

        return snippets;
    }

    private List<String> parseJsonToList(String json) {
        List<String> variableNames = new ArrayList<>();
        try {
            // Use JSONArray to parse the JSON array string
            JSONArray jsonArray = new JSONArray(removeCodeBlockMarkers(json));
            boolean split = false;
            int docCount = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                variableNames.add(jsonArray.getString(i));
                String line = jsonArray.getString(i).trim();
                if (line.startsWith("}")) {
                    split = true;
                }
                if (line.trim().startsWith("*")) {
                    docCount++;
                }
            }
            if (split || jsonArray.length() - 1 == docCount) {
                return Collections.singletonList(String.join("\n", variableNames));
            }
        } catch (Exception e) {
            return parseJsonToListWithSplit(removeCodeBlockMarkers(json));
        }
        return variableNames;
    }

    private List<String> parseJsonToListWithSplit(String json) {
        List<String> variableNames = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return variableNames;
        }

        json = removeCodeBlockMarkers(json).trim();
        String newjson = json;
        if (json.startsWith("[") && json.endsWith("]")) {
            newjson = json.substring(1, json.length() - 1).trim();
        }
        // Remove square brackets and split by new lines
        String[] lines = newjson.split("\\n");

        if (lines.length > 1) {
            for (String line : lines) {
                // Trim each line and add to the list if it's not empty
                line = line.trim();
                if (line.startsWith("\"")) {
                    if (line.endsWith("\"")) {
                        line = line.substring(1, line.length() - 1).trim();
                    } else if (line.endsWith("\",")) {
                        line = line.substring(1, line.length() - 2).trim();
                    }
                }
                if (!line.isEmpty()) {
                    variableNames.add(line);
                }
            }
        } else {
            variableNames = parseJsonToList(json);
        }

        return variableNames;
    }

    public String fixGrammar(String text, String classContent) {
        String prompt
                = "You are an AI model designed to correct grammar mistakes. "
                + "Given the following text and the context of the Java class, correct any grammar issues in the text. "
                + "Return only the fixed text. Do not include any additional details or explanations.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Text to Fix:\n" + text;

        // Generate the grammar-fixed text
        String response = generate(null, prompt);
        LOG.finest(response);
        return response;
    }

    public String enhanceText(String text, String classContent) {
        String prompt = "You are an AI model designed to improve text. "
                + "Given the following text and the context of the Java class, enhance the text to be more engaging, clear, and polished. "
                + "Ensure the text is well-structured and free of any grammatical errors or awkward phrasing. "
                + "Return only the enhanced text. Do not include any additional details or explanations.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Text to Enhance:\n" + text;

        // Generate the enhanced text
        String enhancedText = generate(null, prompt);
        LOG.finest(enhancedText);
        return enhancedText;
    }

    public String enhanceExpressionStatement(
            Project project, String classContent, String parentContent, String expressionStatementContent) {
        // Construct the prompt for enhancing the expression statement
        String prompt = "You are an API server that enhances Java code snippets. "
                + "Given the following Java class content, the parent content of the EXPRESSION_STATEMENT, "
                + "and the content of the EXPRESSION_STATEMENT itself, enhance the EXPRESSION_STATEMENT to be more efficient, "
                + "clear, or follow best practices. Do not include any additional text or explanation, just return the enhanced code snippet.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Parent Content of EXPRESSION_STATEMENT:\n" + parentContent + "\n\n"
                + "EXPRESSION_STATEMENT Content:\n" + expressionStatementContent;

        String enhanced = generate(project, prompt);
        LOG.finest(enhanced);
        return enhanced;
    }

    public String generateCommitMessageSuggestions(String gitDiffOutput, String referenceCommitMessage, List<String> images, List<Response> previousChatResponse) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an API server that generates commit message suggestions based on the provided 'git diff' and 'git status' output. ")
                .append("""
                    Please provide various types of commit messages based on the changes:
                    Your goal is to create commit messages that reflect business or domain features rather than technical details like dependency updates or refactoring.
                    """)
                .append("- Very Short\n")
                .append("- Short\n")
                .append("- Medium\n")
                .append("- Long\n")
                .append("- Descriptive\n\n")
                .append("Here is the 'git diff' and 'git status' output:\n")
                .append(gitDiffOutput)
                .append("\n");

        // Add reference commit message to the prompt if it is not empty or null
        if (referenceCommitMessage != null && !referenceCommitMessage.isEmpty()) {
            prompt.append("Reference Commit Message:\n").append(referenceCommitMessage).append("<br><br>")
                    .append("Ensure that all the following commit message suggestions are aligned with this reference message. "
                            + "The suggestions should reflect the intent and context of the reference commit message, focusing on the business or domain features, adapting it as necessary to fit the changes in the 'git diff' output. "
                            + "The goal is to keep all suggestions consistent with the meaning of the reference commit message.<br>");
        } else {
            prompt.append("No reference commit message provided.<br><br>")
                    .append("Please generate commit message suggestions based on the 'git diff' output and the context of the changes, emphasizing business or domain features.");
        }

        // Generate the commit message suggestions
        String response = generate(null, prompt.toString(), images, previousChatResponse);
        LOG.finest(response);
        response = removeCodeBlockMarkers(response);
        return response;
    }

    public String generateCodeReviewSuggestions(
            final String gitDiffOutput, final String query,
            final List<String> images, final List<Response> previousChatResponse,
            final String reviewPrompt
    ) {

        String prompt = """
            Instructions:
            - Base your review strictly on the provided Git diff.
            - Anchor each suggestion to a specific hunk header from the diff.
            - DO NOT infer or hallucinate line numbers not present in the diff.
            - DO NOT reference line numbers or attempt to estimate exact start/end lines.

            %s

            Respond only with a YAML array of review suggestions. Each suggestion must include:
            - file: the file name
            - hunk: the Git diff hunk header (e.g., "@@ -10,7 +10,9 @@")
            - type: one of "security", "warning", "info", or "suggestion"
                - "security" for vulnerabilities or high-risk flaws
                - "warning" for potential bugs or unsafe behavior
                - "info" for minor issues or readability
                - "suggestion" for non-critical improvements or refactoring
            - title: a short title summarizing the issue
            - description: a longer explanation or recommendation

            Output raw YAML with no markdown, code block, or extra formatting.

            Expected YAML format:

            - file: src/com/example/MyService.java
              hunk: "@@ -42,6 +42,10 @@"
              type: warning
              title: "Possible null pointer exception"
              description: "The 'items' list might be null before iteration. Add a null check to avoid NPE."

            %s
            """.formatted(query, gitDiffOutput);

        // pm.getPrompts().get("codereview")

        return generate(null, reviewPrompt  + '\n' + prompt, images, previousChatResponse);
    }

    public String assistDbMetadata(
        final String dbMetadata, final String query, final List<String> images,
        final List<Response> previousChatResponse, final String sessionRules
    ) {
        StringBuilder dbPrompt = new StringBuilder("You are an API server that provides assistance. ");

        dbPrompt.append("Given the following database schema metadata:\n")
                .append(dbMetadata);

        if (sessionRules != null && !sessionRules.isEmpty()) {
            dbPrompt.append("\n\n")
                    .append(sessionRules)
                    .append("\n\n");
        }

        dbPrompt.append("\nRespond to the developer's question: \n")
                .append(query)
                .append("\n")
                .append("""
                    There are two possible scenarios for your response:

                    1. **SQL Queries and Database-Related Questions**:
                       - Analyze the provided metadata and generate a relevant SQL query that addresses the developer's inquiry.
                       - Include a detailed explanation of the query, clarifying its purpose and how it relates to the developer's question.
                       - Ensure that the SQL syntax adheres to the database structure, constraints, and relationships.
                       - The full SQL query should be wrapped in ```sql for proper formatting.
                       - Avoid wrapping individual SQL keywords or table/column names in <code> tags, and do not wrap any partial SQL query segments in <code> tags.

                    2. **Generating Specific Code from Database Metadata**:
                       - If the developer requests specific code snippets related to the database metadata, generate the appropriate code and include a clear description of its functionality and relevance.
                    """);

        String response = generate(null, dbPrompt.toString(), images, previousChatResponse);

        LOG.finest(() -> response);

        return response;
    }

    public String assistJavaClass(
        final Project project, final String classContent, final String sessionRules
    ) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an API server that provides a description of the following class. ");
        if (sessionRules != null && !sessionRules.isEmpty()) {
            promptBuilder.append("\n\n")
                    .append(sessionRules)
                    .append("\n\n");
        }
        promptBuilder.append("Java Class:\n")
                .append(classContent);

        String prompt = promptBuilder.toString();
        String response = generate(project, prompt);

        LOG.finest(() -> response);

        return response;
    }

    public String assistJavaMethod(
        final Project project, final String methodContent, final String sessionRules
    ) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an API server that provides a description of the following Method. ");

        if (sessionRules != null && !sessionRules.isEmpty()) {
            promptBuilder.append("\n\n")
                    .append(sessionRules)
                    .append("\n\n");
        }
        promptBuilder.append("Java Method:\n")
                .append(methodContent);

        String prompt = promptBuilder.toString();
        String response = generate(project, prompt);

        LOG.finest(response);

        return response;
    }

    public String generateDescription(
        final Project project,
        final String source, final String methodContent, final List<String> images,
        final List<Response> previousChatResponse, final String userQuery,
        final String sessionRules
    ) {
        return generateDescription(project, false, source, methodContent, images, previousChatResponse, userQuery, sessionRules);
    }

    public String generateDescription(
        final Project project, final boolean agentEnabled,
        final String source, final String methodContent, final List<String> images,
        final List<Response> previousChatResponse, final String userQuery,
        final String sessionRules
    ) {
        StringBuilder prompt = new StringBuilder();
        if (sessionRules != null && !sessionRules.isEmpty()) {
            prompt.append(sessionRules).append("\n\n");
        }

        if (methodContent != null) {
            prompt.append("Method Content:\n")
                    .append(methodContent)
                    .append("\n\nDo not return complete Java Class, return only Method");
        } else if (source != null) {
            prompt.append("Source:\n")
                    .append(source)
                    .append("\n\n");
        }
        prompt.append("User Query:\n")
                .append(userQuery);

        String response = generate(project, agentEnabled, prompt.toString(), images, previousChatResponse);

        LOG.finest(response);

        return response;
    }

    public String generateTestCase(
        final Project project,
        final String projectContent, final String classContent, final String methodContent,
        final List<Response> previousChatResponse, final String userQuery,
        final String testPrompts, final String sessionRules
    ) {
        StringBuilder promptBuilder = new StringBuilder();
        StringBuilder promptExtend = new StringBuilder();
        Set<String> testCaseTypes = new HashSet<>(); // Using a Set to avoid duplicates

        StringBuilder userQueryBuilder = new StringBuilder("User Query: ");
        if (userQuery != null) {
            userQueryBuilder.append(userQuery).append(" ,\n ");

            //
            // If we have a valid query, let's check if any testing framework
            // has been mentioned to reinforce the prompt
            //
            // TODO: do we really need it, or the model takes care of it already?
            //
            if (userQuery.toLowerCase().contains("junit5")) {
                testCaseTypes.add("JUnit5");
            } else if (userQuery.toLowerCase().contains("junit")) {
                testCaseTypes.add("JUnit");
            }

            if (userQuery.toLowerCase().contains("testng")) {
                testCaseTypes.add("TestNG");
            }

            if (userQuery.toLowerCase().contains("mockito")) {
                testCaseTypes.add("Mockito");
            }

            if (userQuery.toLowerCase().contains("spock")) {
                testCaseTypes.add("Spock");
            }

            if (userQuery.toLowerCase().contains("assertj")) {
                testCaseTypes.add("AssertJ");
            }

            if (userQuery.toLowerCase().contains("hamcrest")) {
                testCaseTypes.add("Hamcrest");
            }

            if (userQuery.toLowerCase().contains("powermock")) {
                testCaseTypes.add("PowerMock");
            }

            if (userQuery.toLowerCase().contains("cucumber")) {
                testCaseTypes.add("Cucumber");
            }

            if (userQuery.toLowerCase().contains("spring test")) {
                testCaseTypes.add("Spring Test");
            }

            if (userQuery.toLowerCase().contains("arquillian")) {
                testCaseTypes.add("Arquillian Test");
            }

        }
        if (testPrompts != null) {
            userQueryBuilder.append(testPrompts);
        }

        String testCaseType = String.join(", ", testCaseTypes);

        // Build the promptExtend based on available content
        if (methodContent != null) {
            promptExtend.append("Method Content:\n").append(methodContent).append("\n\n")
                    .append("Generate ").append(testCaseType).append(" test cases for this method. Include assertions and necessary mock setups. ");
        } else if (projectContent != null) {
            promptExtend.append("Project Full Content:\n").append(projectContent).append("\n\n")
                    .append("Generate ").append(testCaseType).append(" test cases for all classes. Include assertions and necessary mock setups. ");
        } else {
            promptExtend.append("Java Class Content:\n").append(classContent).append("\n\n")
                    .append("Generate ").append(testCaseType).append(" test cases for this class. Include assertions and necessary mock setups. ");
        }

        promptBuilder.append("You are an API server that provides ");
        if (sessionRules != null && !sessionRules.isEmpty()) {
            promptBuilder.append("\n\n")
                    .append(sessionRules)
                    .append("\n\n");
        }

        promptBuilder.append(testCaseType).append(" test cases in Java for a given class or method based on the original Java class content. ")
                .append("Given the following Java class or method content and the user's query, generate ")
                .append(testCaseType).append(" test cases that are well-structured and functional. ")
                .append(promptExtend)
                .append(userQueryBuilder);

        // Generate the test cases
        String response = generate(project, promptBuilder.toString(), null, previousChatResponse);

        LOG.finest(response);

        return response;
    }

    public List<Snippet> suggestNextLineCode(
        final Project project,
        final String fileContent, final String currentLine, final String mimeType,
        final String hintContext, final boolean singleCodeSnippet
    ) {
        final StringBuilder description = new StringBuilder(MIME_TYPE_DESCRIPTIONS.getOrDefault(mimeType, "code snippets"));
        StringBuilder prompt = new StringBuilder("You are an API server that provides ").append(description).append(" suggestions based on the file content. ");

        boolean hasHint = hintContext != null && !hintContext.isEmpty();
        if (hasHint) {
            prompt.append("Suggest code for ${SUGGEST_CODE} based on the file's context. ");
            if (currentLine != null && !currentLine.isEmpty()) {
                prompt.append("Current line:\n").append(currentLine).append("\n");
            }
            prompt.append("Return a JSON object with 'snippet' as a single string using \\n for line breaks.\n\nFile Content:\n")
                    .append(fileContent).append(hintContext).append("\n");

        } else {
            if (currentLine == null || currentLine.isEmpty()) {
                prompt.append("Analyze the content and recommend appropriate additions at the placeholder ${SUGGEST_CODE}. ");
            } else {
                prompt.append("Analyze the content and the current line: \n")
                        .append(currentLine)
                        .append("\nRecommend appropriate additions at the placeholder ${SUGGEST_CODE}. ");
            }
            prompt.append("""
                  Ensure the suggestions align with the file's context and structure.
                  Respond with a JSON array containing a few of the best options.
                  Each entry should have one field, 'snippet', holding the recommended code block.
                  The code block can contain multiple lines, formatted as a single string using \\n for line breaks.

                  File Content:
                  """).append(fileContent);
        }

        String jsonResponse = generate(project, prompt.toString());

        LOG.finest(jsonResponse);

        return parseJsonToSnippets(jsonResponse);
    }

    public List<Snippet> suggestSQLQuery(
        final String dbMetadata, final String editorContent,
        final boolean description
    ) {
        StringBuilder prompt = new StringBuilder("You are an API server that provides SQL query suggestions based on the provided database schema metadata. ");

        if (editorContent == null || editorContent.isEmpty()) {
            prompt.append("Analyze the metadata and recommend appropriate SQL queries at the placeholder ${SUGGEST_SQL_QUERY_LIST}. ");
        } else {
            prompt.append("Based on the following content in the editor: \n")
                    .append(editorContent)
                    .append("\nAnalyze the metadata and recommend SQL queries at the placeholder ${SUGGEST_SQL_QUERY_LIST}. ");
        }

        prompt.append("""
          Ensure the SQL queries match the database structure, constraints, and relationships.
          Respond with a JSON array containing the best SQL query options.
          Each entry should have one field, 'snippet', holding the recommended SQL query block, which may include multiple lines formatted as a single string using \\n for line breaks.
          """);

        // Include description if enabled
        if (description) {
            prompt.append("""
          Additionally, each entry should contain a 'description' field providing a very short explanation of what the query does and why it might be appropriate in this context,
          formatted with <b>, <br> tags, and optionally, if required, include any important link with <a href=''> tags.
          """);
        }

        prompt.append("Database Metadata:\n").append(dbMetadata);

        String jsonResponse = generate(null, prompt.toString());
        List<Snippet> sqlQueries = parseJsonToSnippets(jsonResponse);
        return sqlQueries;
    }

    public void addProgressListener(final PropertyChangeListener listener) {
        progressListeners.addPropertyChangeListener(listener);
    }

    public void removeProgressListener(final PropertyChangeListener listener) {
        progressListeners.removePropertyChangeListener(listener);
    }
}

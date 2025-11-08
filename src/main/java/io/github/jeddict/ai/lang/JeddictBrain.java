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
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.Assistant;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.response.TokenHandler;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.JSONUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import io.github.jeddict.ai.util.Utilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;

public class JeddictBrain {

    private final Logger LOG = Logger.getLogger(JeddictBrain.class.getCanonicalName());

    public enum EventProperty {
        CHAT_TOKENS("chatTokens"),
        CHAT_ERROR("chatError"),
        CHAT_COMPLETED("chatComplete"),
        CHAT_PARTIAL("chatPartial"),
        CHAT_INTERMEDIATE("chatIntermediate"),
        TOOL_BEFORE_EXECUTION("toolBeforeExecution"),
        TOOL_EXECUTED("toolExecuted")
        ;

        public final String name;

        EventProperty(final String name) {
            this.name = JeddictBrain.class.getCanonicalName() + '.' + name;
        }
    }

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

    public JeddictBrain(
        final boolean streaming
    ) {
        this("", streaming, List.of());
    }

    public JeddictBrain(
        final String modelName,
        final boolean streaming,
        final List<AbstractTool> tools
    ) {
        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }
        this.modelName = modelName;

        final JeddictChatModelBuilder builder =
            new JeddictChatModelBuilder(this.modelName);

        if (streaming) {
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
    // TODO: P3 - better use of langchain4j functionalities (see https://docs.langchain4j.dev/tutorials/agents)
    // TODO: P3 - after refactory project should not be needed any more
    //
    private String generateInternal(Project project, boolean agentEnabled, String prompt, List<String> images, List<Response> responseHistory) {
        if (chatModel.isEmpty() && streamingChatModel.isEmpty()) {
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
        fireEvent(EventProperty.CHAT_TOKENS, TokenHandler.saveInputToken(messages));

        final StringBuilder response = new StringBuilder();
        try {

            if (streamingChatModel.isPresent()) {
                if(agentEnabled) {
                    final Assistant assistant = AiServices.builder(Assistant.class)
                        .streamingChatModel(streamingChatModel.get())
                        .tools(tools.toArray())
                        .build();

                    assistant.stream(messages)
                        .onCompleteResponse(complete -> {
                            fireEvent(EventProperty.CHAT_COMPLETED, complete);
                            //handler.onCompleteResponse(partial);
                        })
                        .onPartialResponse(partial -> {
                            fireEvent(EventProperty.CHAT_PARTIAL, partial);
                            //handler.onPartialResponse(partial);
                        })
                        .onIntermediateResponse(intermediate -> fireEvent(EventProperty.CHAT_INTERMEDIATE, intermediate))
                        .beforeToolExecution(execution -> fireEvent(EventProperty.TOOL_BEFORE_EXECUTION, execution))
                        .onToolExecuted(execution -> fireEvent(EventProperty.TOOL_EXECUTED, execution))
                        .onError(error -> {
                            fireEvent(EventProperty.CHAT_ERROR, error);
                            //handler.onError(error);
                        })
                        .start();
                } else {
                    streamingChatModel.get().chat(messages, new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(final String partial) {
                            fireEvent(EventProperty.CHAT_PARTIAL, partial);
                        }

                        @Override
                        public void onCompleteResponse(final ChatResponse completed) {
                            fireEvent(EventProperty.CHAT_COMPLETED, completed);
                        }

                        @Override
                        public void onError(final Throwable error) {
                            fireEvent(EventProperty.CHAT_ERROR, error);
                        }
                    });
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
            fireEvent(EventProperty.CHAT_ERROR, x);
        }

        LOG.finest(() -> "Returning " + response);

        return response.toString();
    }

    public <T> T pairProgrammer(final PairProgrammer.Specialist specialist) {
        return (T)AgenticServices.agentBuilder(specialist.specialistClass)
                .chatModel(chatModel.get())
                .build();
    }

    private String loadClassData(String prompt, String classDatas) {
        if (classDatas == null || classDatas.isEmpty()) {
            return prompt;
        }
        prompt += "\n\nHere is the context of all classes in the project, including variable names and method signatures (method bodies are excluded to avoid sending unnecessary code):\n"
                + classDatas;
        return prompt;
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
        List<String> comments = JSONUtil.jsonToList(jsonResponse);
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
        List<Snippet> annotations = JSONUtil.jsonToSnippets(jsonResponse);
        return annotations;
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

        return JSONUtil.jsonToSnippets(generate(null, prompt.toString()));
    }

    /**
     * Makes a simple query to explain the provided code.
     *
     * @param text the code to explain
     * @param agentEnabled is agent mode enabled?
     *
     * @return the AI explaination of the given content
     */
    public String aboutCode(final String text, final boolean agentEnabled) {
        final String prompt = new StringBuilder()
            .append("Plese explain the following source code:\n")
            .append(text)
            .toString();

        final String response = generate(null, agentEnabled, prompt.toString());

        logPromptResponse(prompt, response);

        return response;
    }

    public void addProgressListener(final PropertyChangeListener listener) {
        progressListeners.addPropertyChangeListener(listener);
    }

    public void removeProgressListener(final PropertyChangeListener listener) {
        progressListeners.removePropertyChangeListener(listener);
    }

    private void logPromptResponse(final String prompt, final String response) {
        LOG.finest(() -> "===\nprompt:\n-------\n" + prompt + "\nresponse:\n---------\n" + response + "\n===");
    }

    private void fireEvent(EventProperty property, Object value) {
        LOG.finest(() -> "Firing event " + property + " with value " + value);
        progressListeners.firePropertyChange(property.name, null, value);
    }
}

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

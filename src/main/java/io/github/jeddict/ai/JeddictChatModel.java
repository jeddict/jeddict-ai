/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.jeddict.ai;

/**
 *
 * @author Gaurav Gupta
 */
import dev.langchain4j.model.openai.OpenAiChatModel;
import javax.swing.JOptionPane;

public class JeddictChatModel {

    private final OpenAiChatModel aiChatModel;
    private static final String API_KEY_ENV_VAR = "OPENAI_API_KEY";
    private static final String API_KEY_SYS_PROP = "openai.api.key";
    private static final String MODEL_ENV_VAR = "OPENAI_MODEL";
    private static final String MODEL_SYS_PROP = "openai.model";

    public static String getApiKey() {
        // First, try to get the API key from the environment variable
        String apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment variable, try system properties
            apiKey = System.getProperty(API_KEY_SYS_PROP);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            // Show popup with instructions if API key is not found
            JOptionPane.showMessageDialog(null,
                "API key is not set in environment variables or system properties.\n" +
                "Please set the environment variable '" + API_KEY_ENV_VAR + "' or system property '" + API_KEY_SYS_PROP + "'.",
                "API Key Not Found",
                JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("API key is not set in environment variables or system properties.");
        }
        return apiKey;
    }

    public static String getModelName() {
        // Try to get the model name from the environment variable
        String modelName = System.getenv(MODEL_ENV_VAR);
        if (modelName == null || modelName.isEmpty()) {
            // If not found in environment variable, try system properties
            modelName = System.getProperty(MODEL_SYS_PROP);
        }
        if (modelName == null || modelName.isEmpty()) {
            // Fallback to default model name
            modelName = "gpt-4o-mini";
        }
        return modelName;
    }

    public JeddictChatModel() {
        aiChatModel = OpenAiChatModel.builder()
                .apiKey(getApiKey())
                .modelName(getModelName())
                .build();
    }

    public String generateJavadocForClass(String classContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for class not the member of class. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java class not the member of class. Do not include any additional text or explanation.\n\n"
                + classContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateJavadocForMethod(String methodContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for method. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java method. Do not include any additional text or explanation.\n\n"
                + methodContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateJavadocForField(String fieldContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for field. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java variable. Do not include any additional text or explanation.\n\n"
                + fieldContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceJavadocForClass(String existingJavadoc, String classContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a class. "
                + "Given the existing Javadoc comment and the following Java class, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Class Content:\n" + classContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceJavadocForMethod(String existingJavadoc, String methodContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a method. "
                + "Given the existing Javadoc comment and the following Java method, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Method Content:\n" + methodContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceJavadocForField(String existingJavadoc, String fieldContent) {
        String prompt
                = "You are an API server that enhances existing Javadoc comments for a field. "
                + "Given the existing Javadoc comment and the following Java field, enhance the Javadoc comment by adding more details if necessary. "
                + "Do not include any additional text or explanation, just the enhanced Javadoc wrapped with /** ${javadoc} **/.\n\n"
                + "Existing Javadoc:\n" + existingJavadoc + "\n\n"
                + "Java Field Content:\n" + fieldContent;
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateRestEndpointForClass(String classContent) {
        // Define a prompt to generate unique JAX-RS resource methods with necessary imports
        String prompt = "You are an API server that generates JAX-RS REST endpoints based on the provided Java class definition. "
                + "Analyze the context and functionality of the class to create meaningful and relevant REST endpoints. "
                + "Ensure that you create new methods for various HTTP operations (GET, POST, PUT, DELETE) and include all necessary imports for JAX-RS annotations and responses. "
                + "The generated methods should have some basic implementation and should not be empty. Avoid duplicating existing methods from the class content. "
                + "Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methods' (list of newly created annotated methods with basic implementations). "
                + "Include all required imports such as Response, GET, POST, PUT, DELETE, Path, etc. "
                + "Example output:\n"
                + "{\n"
                + "  \"imports\": [\n"
                + "    \"jakarta.ws.rs.GET\",\n"
                + "    \"jakarta.ws.rs.POST\",\n"
                + "    \"jakarta.ws.rs.PUT\",\n"
                + "    \"jakarta.ws.rs.DELETE\",\n"
                + "    \"jakarta.ws.rs.core.Response\"\n"
                + "  ],\n"
                + "  \"methods\": [\n"
                + "    \"@GET public Response getPing() { // implementation }\",\n"
                + "    \"@POST public Response createPing() { // implementation for createPing }\",\n"
                + "    \"@PUT public Response updatePing() { // implementation }\",\n"
                + "    \"@DELETE public Response deletePing() { // implementation for deletePing }\"\n"
                + "  ]\n"
                + "}\n\n"
                + "Only return methods with annotations, implementation details, and necessary imports for the given class. "
                + "Do not include class declarations, constructors, or unnecessary boilerplate code. Ensure the generated methods are unique and not duplicates of existing methods in the class content.\n\n"
                + classContent;

        // Generate the unique JAX-RS methods with imports
        String answer = aiChatModel.generate(prompt);

        // Print and return the generated JAX-RS methods with imports
        System.out.println(answer);
        return answer;
    }

    public String enhanceMethodFromDevQuery(String methodContent, String developerRequest) {
        String prompt
                = "You are an API server that enhances Java methods based on user requests. "
                + "Given the following Java method and the developer's request, modify and enhance the method accordingly. "
                + "Incorporate any specific details or requirements mentioned by the developer. Do not include any additional text or explanation, just return the enhanced Java method source code.\n\n"
                + "Developer Request:\n" + developerRequest + "\n\n"
                + "Java Method Content:\n" + methodContent;

        // Generate the enhanced Java method
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String createMethodFromMethodContent(String javaClassContent, String methodContent) {
        String prompt
                = "You are an API server that enhances or creates Java methods based on the method name, comments, and its content. "
                + "Given the following Java class content and Java method content, modify and enhance the method accordingly. "
                + "Include all necessary imports relevant to the enhanced or newly created method. "
                + "Return only the Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code. "
                + "Do not include full java class, any additional text or explanation, just the imports and the method source code.\n\n"
                + "Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'. "
                + "Java Class Content:\n" + javaClassContent + "\n\n"
                + "Java Method Content:\n" + methodContent;

        // Generate the enhanced or newly created Java method with necessary imports
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceVariableName(String variableContext, String methodContent, String classContent) {
        String prompt
                = "You are an API server that suggests a more meaningful and descriptive name for a specific variable in a given Java class. "
                + "Based on the provided Java class content and the variable context, suggest an improved name for the variable. "
                + "Return only the new variable name. Do not include any additional text or explanation.\n\n"
                + "Variable Context:\n" + variableContext + "\n\n"
                + (methodContent != null ? ("Java Method Content:\n" + methodContent+ "\n\n") : "") 
                + (classContent != null ? ("Java Class Content:\n" + classContent) : "") ;

        // Generate the new variable name
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String fixGrammar(String text, String classContent) {
        String prompt
                = "You are an AI model designed to correct grammar mistakes. "
                + "Given the following text and the context of the Java class, correct any grammar issues in the text. "
                + "Return only the fixed text. Do not include any additional details or explanations.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Text to Fix:\n" + text;

        // Generate the grammar-fixed text
        String answer = aiChatModel.generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceText(String text, String classContent) {
        String prompt = "You are an AI model designed to improve text. "
                + "Given the following text and the context of the Java class, enhance the text to be more engaging, clear, and polished. "
                + "Ensure the text is well-structured and free of any grammatical errors or awkward phrasing. "
                + "Return only the enhanced text. Do not include any additional details or explanations.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Text to Enhance:\n" + text;

        // Generate the enhanced text
        String enhancedText = aiChatModel.generate(prompt);
        System.out.println(enhancedText);
        return enhancedText;
    }
    
       public String enhanceExpressionStatement(String classContent, String parentContent, String expressionStatementContent) {
        // Construct the prompt for enhancing the expression statement
        String prompt = "You are an API server that enhances Java code snippets. "
                + "Given the following Java class content, the parent content of the EXPRESSION_STATEMENT, "
                + "and the content of the EXPRESSION_STATEMENT itself, enhance the EXPRESSION_STATEMENT to be more efficient, "
                + "clear, or follow best practices. Do not include any additional text or explanation, just return the enhanced code snippet.\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Parent Content of EXPRESSION_STATEMENT:\n" + parentContent + "\n\n"
                + "EXPRESSION_STATEMENT Content:\n" + expressionStatementContent;

         String enhanced = aiChatModel.generate(prompt);
        System.out.println(enhanced);
        return enhanced;
    }

}

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
 * @author Shiwani Gupta
 */
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.MimeUtil.MIME_TYPE_DESCRIPTIONS;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.json.JSONArray;
import org.json.JSONObject;

public class JeddictChatModel {

    private final OpenAiChatModel aiChatModel;
    private int cachedClassDatasLength = -1; // Cache the length of classDatas
    PreferencesManager preferencesManager = PreferencesManager.getInstance();

    public JeddictChatModel() {
        aiChatModel = OpenAiChatModel.builder()
                .apiKey(preferencesManager.getApiKey())
                .modelName(preferencesManager.getModelName())
                .build();
    }

    private String generate(String prompt) {
        try {
            return aiChatModel.generate(prompt);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (e.getCause() != null
                    && e.getCause() instanceof dev.ai4j.openai4j.OpenAiHttpException) {
                JSONObject jsonObject = new JSONObject(e.getCause().getMessage());
                if (jsonObject.has("error") && jsonObject.getJSONObject("error").has("message")) {
                    errorMessage = jsonObject.getJSONObject("error").getString("message");
                }
            }
            if (errorMessage != null
                    && errorMessage.toLowerCase().contains("incorrect api key")) {
                JTextField apiKeyField = new JTextField(20);
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Set layout to BoxLayout

                panel.add(new JLabel("Incorrect API key. Please enter a new key:"));
                panel.add(Box.createVerticalStrut(10)); // Add space between label and text field
                panel.add(apiKeyField);

                int option = JOptionPane.showConfirmDialog(null, panel,
                        "API Key Required", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    preferencesManager.setApiKey(apiKeyField.getText().trim());
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "AI assistance failed to generate the requested response: " + errorMessage,
                        "Error in AI Assistance",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    public String generateJavadocForClass(String classContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for class not the member of class. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java class not the member of class. Do not include any additional text or explanation.\n\n"
                + classContent;
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateJavadocForMethod(String methodContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for method. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java method. Do not include any additional text or explanation.\n\n"
                + methodContent;
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateJavadocForField(String fieldContent) {
        String prompt
                = "You are an API server that responds only with Javadoc comments for field. "
                + "Generate only the Javadoc wrapped with in /** ${javadoc} **/ for the following Java variable. Do not include any additional text or explanation.\n\n"
                + fieldContent;
        String answer = generate(prompt);
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
        String answer = generate(prompt);
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
        String answer = generate(prompt);
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
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateRestEndpointForClass(String classContent) {
        // Define a prompt to generate unique JAX-RS resource methods with necessary imports
        String prompt = "You are an API server that generates JAX-RS REST endpoints based on the provided Java class definition. "
                + "Analyze the context and functionality of the class to create meaningful and relevant REST endpoints. "
                + "Ensure that you create new methods for various HTTP operations (GET, POST, PUT, DELETE) and include all necessary imports for JAX-RS annotations and responses. "
                + "The generated methods should have some basic implementation and should not be empty. Avoid duplicating existing methods from the class content. "
                + "Format the output as a JSON object with two fields: 'imports' as array (list of necessary imports) and 'methodContent' as text. "
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
                + "  \"methodContent\": \"@GET public Response getPing() { // implementation }@POST public Response createPing() { // implementation for createPing }@PUT public Response updatePing() { // implementation }@DELETE public Response deletePing() { // implementation for deletePing }\"\n"
                + "}\n\n"
                + "Only return methods with annotations, implementation details, and necessary imports for the given class. "
                + "Do not include class declarations, constructors, or unnecessary boilerplate code. Ensure the generated methods are unique and not duplicates of existing methods in the class content.\n\n"
                + classContent;

        // Generate the unique JAX-RS methods with imports
        String answer = generate(prompt);

        // Print and return the generated JAX-RS methods with imports
        System.out.println(answer);
        return answer;
    }

    public String updateMethodFromDevQuery(String javaClassContent, String methodContent, String developerRequest) {
        String prompt
                = "You are an API server that enhances Java methods based on user requests. "
                + "Given the following Java method and the developer's request, modify and enhance the method accordingly. "
                + "Incorporate any specific details or requirements mentioned by the developer. Do not include any additional text or explanation, just return the enhanced Java method source code.\n\n"
                + "Include all necessary imports relevant to the enhanced or newly created method. "
                + "Return only the Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code. "
                + "Do not include full java class, any additional text or explanation, just the imports and the method source code.\n\n"
                + "Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'. "
                + "Developer Request:\n" + developerRequest + "\n\n"
                + "Java Class Content:\n" + javaClassContent + "\n\n"
                + "Java Method Content:\n" + methodContent;

        // Generate the enhanced Java method
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceMethodFromMethodContent(String javaClassContent, String methodContent) {
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
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String fixMethodCompilationError(String javaClassContent, String methodContent, String errorMessage) {
        String prompt
                = "You are an API server that fixes compilation errors in Java methods based on the provided error messages. "
                + "Given the following Java method content, class content, and the error message, correct the method accordingly. "
                + "Ensure that all compilation errors indicated by the error message are resolved. "
                + "Include any necessary imports relevant to the fixed method. "
                + "Return only the corrected Java method and its necessary imports, without including any class declarations, constructors, or other boilerplate code. "
                + "Do not include full Java class, any additional text, or explanation—just the imports and the corrected method source code.\n\n"
                + "Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'methodContent'. "
                + "Error Message:\n" + errorMessage + "\n\n"
                + "Java Class Content:\n" + javaClassContent + "\n\n"
                + "Java Method Content:\n" + methodContent;

        // Generate the fixed Java method
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String fixVariableError(String javaClassContent, String errorMessage) {
        String prompt
                = "You are an API server that fixes variable-related compilation errors in Java classes based on the provided error messages. "
                + "Given the following Java class content and the error message, correct given variable-related issues based on error message at class level. "
                + "Ensure that all compilation errors indicated by the error message, such as undeclared variables, incorrect variable types, or misuse of variables, are resolved. "
                + "Include any necessary imports relevant to the fixed method or class. "
                + "Return only the corrected variable content and its necessary imports, without including any unnecessary boilerplate code. "
                + "Do not include any additional text or explanation—just the imports and the corrected variable source code.\n\n"
                + "Format the output as a JSON object with two fields: 'imports' (list of necessary imports) and 'variableContent' (corrected variable line or content). "
                + "Error Message:\n" + errorMessage + "\n\n"
                + "Java Class Content:\n" + javaClassContent;

        // Generate the fixed Java class or method
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String enhanceVariableName(String variableContext, String methodContent, String classContent) {
        String prompt
                = "You are an API server that suggests a more meaningful and descriptive name for a specific variable in a given Java class. "
                + "Based on the provided Java class content and the variable context, suggest an improved name for the variable. "
                + "Return only the new variable name. Do not include any additional text or explanation.\n\n"
                + "Variable Context:\n" + variableContext + "\n\n"
                + (methodContent != null ? ("Java Method Content:\n" + methodContent + "\n\n") : "")
                + (classContent != null ? ("Java Class Content:\n" + classContent) : "");

        // Generate the new variable name
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public List<String> suggestVariableNames(String classDatas, String variablePrefix, String classContent, String variableExpression) {
        String prompt;

//    if(variablePrefix == null || variablePrefix.isEmpty()) {
        prompt = "You are an API server that suggests a list of meaningful and descriptive names for a specific variable in a given Java class. "
                + "Based on the provided Java class content, variable prefix, and variable expression, generate a list of improved names for the variable. "
                + "Return only the list of suggested names, one per line, without any additional text or explanation.\n\n"
                + "Variable Prefix: " + variablePrefix + "\n\n"
                + "Variable Expression Line:\n" + variableExpression + "\n\n"
                //            + "Parent Content:\n" + parentContent + "\n\n"
                + "Java Class Content:\n" + classContent + "\n\n"
                + "Here is the context of all classes in the project, including variable names and method signatures (method bodies are excluded to avoid sending unnecessary code):\n" + classDatas;
//    }

        // Generate the list of suggested variable names
        String answer = generate(prompt);
        System.out.println(answer);

        // Split the response into a list and return
        return Arrays.asList(answer.split("\n"));
    }

    private String loadClassData(String prompt, String classDatas) {
        // Check if the length of classDatas has changed
        if (classDatas == null) {
            return prompt;
        }
        if (classDatas.length() != cachedClassDatasLength) {
            // Add classDatas to the prompt if the length has changed
            prompt += "\n\n" + "Here is the context of all classes in the project, including variable names and method signatures (method bodies are excluded to avoid sending unnecessary code):\n"
                    + classDatas;
            // Update cached length
            cachedClassDatasLength = classDatas.length();
        } else {
            // Don't include classDatas if it's unchanged
            prompt += "The class context has not changed.";
        }
        return prompt;
    }

    public List<String> suggestVariableNames(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and descriptive names for a specific variable in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved names for the variable represented by the placeholder ${SUGGEST_VAR_NAMES_LIST} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);

        // Generate the list of new variable names
        String jsonResponse = generate(prompt);

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
        String jsonResponse = generate(prompt);

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
        String jsonResponse = generate(prompt);

        // Parse the JSON response into a List
        List<String> stringLiterals = parseJsonToListWithSplit(jsonResponse);

        return stringLiterals;
    }

    public List<String> suggestMethodInvocations(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests multiple meaningful and appropriate method invocations for a specific context in a given Java class. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a list of improved method invocations represented by the placeholder ${SUGGEST_METHOD_INVOCATION} in Java Class. "
                + "Do not include additional text; return only the suggestions as a JSON array.\n\n"
                + "Java Class Content:\n" + classContent;

        prompt = loadClassData(prompt, classDatas);

        // Generate the list of new method invocations
        String jsonResponse = generate(prompt);

        // Parse the JSON response into a List
        List<String> methodInvocations = parseJsonToList(jsonResponse);

        return methodInvocations;
    }

    public List<Snippet> suggestNextLineCode(String classDatas, String classContent, String lineText, TreePath path) {
        String prompt;

        if (path == null) {
            prompt = "You are an API server that suggests Java code for the outermost context of a Java source file, outside of any existing class. "
                    + "Based on the provided Java source file content, suggest relevant code to be added at the placeholder location ${SUGGEST_CODE_LIST}. "
                    + "Suggest additional classes, interfaces, enums, or other top-level constructs. "
                    + "Ensure that the suggestions fit the context of the entire file. "
                    + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Source File Content:\n" + classContent;
        } else if (path.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
            prompt = "You are an API server that suggests Java code for the outermost context of a Java source file, outside of any existing class. "
                    + "Based on the provided Java source file content, suggest relevant code to be added at the placeholder location ${SUGGEST_CODE_LIST}. "
                    + "Suggest package declarations, import statements, comments, or annotations for public class. "
                    + "Ensure that the suggestions fit the context of the entire file. "
                    + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Source File Content:\n" + classContent;
        } else if (path.getLeaf().getKind() == Tree.Kind.MODIFIERS
                && path.getParentPath() != null
                && path.getParentPath().getLeaf().getKind() == Tree.Kind.CLASS) {
            prompt = "You are an API server that suggests Java code modifications for a class. "
                    + "At the placeholder location ${SUGGEST_CODE_LIST}, suggest either a class-level modifier such as 'public', 'protected', 'private', 'abstract', 'final', or a relevant class-level annotation. "
                    + "Ensure that the suggestions are appropriate for the class context provided. "
                    + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Class Content:\n" + classContent;
        } else if (path.getLeaf().getKind() == Tree.Kind.MODIFIERS
           && path.getParentPath() != null
           && path.getParentPath().getLeaf().getKind() == Tree.Kind.METHOD) {
    prompt = "You are an API server that suggests Java code modifications for a method. "
            + "At the placeholder location ${SUGGEST_CODE_LIST}, suggest method-level modifiers such as 'public', 'protected', 'private', 'abstract', 'static', 'final', 'synchronized', or relevant method-level annotations. "
            + "Additionally, you may suggest method-specific annotations like '@Override', '@Deprecated', '@Transactional', etc. "
            + "Ensure that the suggestions are appropriate for the method context provided. "
            + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
            + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
            + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
            + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
            + "Java Method Content:\n" + classContent;
} else if (path.getLeaf().getKind() == Tree.Kind.CLASS
                && path.getParentPath() != null
                && path.getParentPath().getLeaf().getKind() == Tree.Kind.CLASS) {
            prompt = "You are an API server that suggests Java code for an inner class at the placeholder location ${SUGGEST_CODE_LIST}. "
                    + "Based on the provided Java class content, suggest either relevant inner class modifiers such as 'public', 'private', 'protected', 'static', 'abstract', 'final', or a full inner class definition. "
                    + "Additionally, you may suggest class-level annotations for the inner class. Ensure that the suggestions are contextually appropriate for an inner class. "
                    + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Class Content:\n" + classContent;
        }  else if (path.getLeaf().getKind() == Tree.Kind.CLASS
                && path.getParentPath() != null
                && path.getParentPath().getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
            prompt = "You are an API server that suggests Java code for an class at the placeholder location ${SUGGEST_CODE_LIST}. "
                    + "Based on the provided Java class content, suggest either relevant class level members, attributes, constants, methods or blocks. "
                    + "Ensure that the suggestions are contextually appropriate for an class. "
                    + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Class Content:\n" + classContent;
        } else {
            prompt = "You are an API server that suggests Java code for a specific context in a given Java class at the placeholder location ${SUGGEST_CODE_LIST}. "
                    + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest a relevant single line of code or a multi-line code block as appropriate for the context represented by the placeholder ${SUGGEST_CODE_LIST} in the Java class. "
                    + "Ensure that the suggestions are relevant to the context. "
                    + "Return a JSON array with few best suggestions without any additional text or explanation where each element is an object containing two fields: 'imports' and 'snippet'. "
                    + "'imports' should be an array of required Java import statements for the suggested code (if no imports are required, return an empty array). "
                    + "'snippet' should contain the suggested code as text block, which may include multiple lines formatted as a single string using \\n for line breaks. \n\n"
                    + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
                    + "Java Class Content:\n" + classContent;
        }

        prompt = loadClassData(prompt, classDatas);

        // Generate the list of suggested next lines of code
        String jsonResponse = generate(prompt);
        System.out.println("jsonResponse " + jsonResponse);
        // Parse the JSON response into a List
        List<Snippet> nextLines = parseJsonToSnippets(jsonResponse);
        return nextLines;
    }

    public List<String> suggestJavaComment(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests appropriate Java comments for a specific context in a given Java class at the placeholder location ${SUGGEST_JAVA_COMMENT}. "
                + "Based on the provided Java class content and the line of comment: \"" + lineText + " ${SUGGEST_JAVA_COMMENT} \", suggest relevant Java comment as appropriate for the context represented by the placeholder ${SUGGEST_JAVA_COMMENT} in the Java Class. "
                + "Return a JSON array where each element must be single line comment. \n\n"
                + "Java Class Content:\n" + classContent;
        // Generate the list of suggested Javadoc or comments
        String jsonResponse = generate(prompt);
        System.out.println("jsonResponse " + jsonResponse);
        // Parse the JSON response into a List
        List<String> comments = parseJsonToList(jsonResponse);

        return comments;
    }

    public List<String> suggestJavadocOrComment(String classDatas, String classContent, String lineText) {
        String prompt = "You are an API server that suggests appropriate Javadoc or comments for a specific context in a given Java class at the placeholder location ${SUGGEST_JAVADOC}. "
                + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest relevant Javadoc or a comment block as appropriate for the context represented by the placeholder ${SUGGEST_JAVADOC} in the Java Class. "
                + "Return a JSON array where each element can either be a single-line comment, a multi-line comment block, or a Javadoc comment formatted as a single string using \\n for line breaks. "
                + " Do not split multi line javadoc comments to array, must be at same index in json array. \n\n"
                //            + "Ensure that the suggestions are relevant to the context of com.sun.source.tree.Tree.Kind." + type + ". "
                + "Java Class Content:\n" + classContent;
        // Generate the list of suggested Javadoc or comments
        String jsonResponse = generate(prompt);
        System.out.println("jsonResponse " + jsonResponse);
        // Parse the JSON response into a List
        List<String> comments = parseJsonToList(jsonResponse);
        return comments;
    }

    public List<Snippet> suggestAnnotations(String classDatas, String classContent, String lineText) {
       String prompt = "You are an API server that suggests Java annotations for a specific context in a given Java class at the placeholder location ${SUGGEST_ANNOTATION_LIST}. "
            + "Based on the provided Java class content and the line of code: \"" + lineText + "\", suggest relevant annotations that can be applied at the placeholder location represented by ${SUGGEST_ANNOTATION_LIST} in the Java Class. "
            + "Return a JSON array with a few best suggestions without any additional text or explanation. Each element should be an object containing two fields: 'imports' and 'snippet'. "
            + "'imports' should be an array of required Java import statements (if no imports are required, return an empty array). "
            + "'snippet' should contain the suggested code as a text block, which may include multiple lines formatted as a single string using \\n for line breaks. "
            + "Make sure to escape any double quotes within the snippet using a backslash (\\) so that the JSON remains valid. \n\n"
            + "Ensure that the suggestions are appropriate for the given Java Class Content:\n\n" + classContent;

        // Generate the list of suggested annotations
        String jsonResponse = generate(prompt);
        System.out.println("jsonResponse " + jsonResponse);

        // Parse the JSON response into a List
        List<Snippet> annotations = parseJsonToSnippets(jsonResponse);
        return annotations;
    }

    public List<Snippet> parseJsonToSnippets(String jsonResponse) {
        if(jsonResponse == null) {
            return Collections.EMPTY_LIST;
        }
        List<Snippet> snippets = new ArrayList<>();

        // Parse the JSON response
        JSONArray jsonArray = new JSONArray(removeCodeBlockMarkers(jsonResponse));

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

            // Create a new Snippet object and add it to the list
            Snippet snippetObj = new Snippet(snippet, importsList);
            snippets.add(snippetObj);
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
        if(json == null || json.isEmpty()){
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
        String answer = generate(prompt);
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
        String enhancedText = generate(prompt);
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

        String enhanced = generate(prompt);
        System.out.println(enhanced);
        return enhanced;
    }

    public String generateHtmlDescriptionForClass(String classContent) {
        String prompt = "You are an API server that provides a detailed and interactive HTML-formatted description of a Java class. "
                + "Given the following Java class content, generate an HTML document that includes a clear and engaging description of the class. "
                + "Ensure that the HTML content is visually appealing by utilizing Bootstrap CSS for overall styling and use highlight.js for code examples in the response. "
                + "Wrap the code blocks in <pre> tags to preserve formatting and indentation. "
                + "Do not include additional text or explanations outside of the HTML content.\n\n"
                + "Java Class Content:\n" + classContent;

        // Generate the HTML description
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public String generateHtmlDescriptionForClass(String classContent, String previousChatResponse, String userQuery) {
        String prompt;

        if (previousChatResponse == null) {
            prompt = "You are an API server that provides an interactive HTML-formatted answer to a user's query based on Orignal Java class content. "
                    + "Given the following Java class content, and the user's query, generate an HTML document that directly addresses the specific query. "
                    + "Ensure the HTML content is well-structured, clearly answers the query. "
                    + "Use Bootstrap CSS for overall styling and highlight.js for code examples in the response. "
                    + "Wrap the code blocks in <pre> tags to preserve formatting and indentation. "
                    + "Do not include additional text or explanations outside of the HTML content.\n\n"
                    + "If Full Java Class is in response then wrap it in <code type=\"full\" class=\"java\">. "
                    + "If partial snippet of Java Class are in response then wrap it in <code type=\"snippet\" class=\"java\">. "
                    + "Orignal Java Class Content:\n" + classContent + "\n\n"
                    + "User Query:\n" + userQuery;
        } else {
            prompt = "You are an API server that provides an interactive HTML-formatted answer to a user's query based on Orignal Java class content and Previous Chat Content. "
                    + "Given the following Java class content, the previous chat response, and the user's query, generate an HTML document that directly addresses the specific query. "
                    + "Ensure the HTML content is well-structured, clearly answers the query, and reflects any modifications or updates suggested in the previous chat response. "
                    + "Use Bootstrap CSS for overall styling and highlight.js for code examples in the response. "
                    + "Wrap the code blocks in <pre> tags to preserve formatting and indentation. "
                    + "Do not include additional text or explanations outside of the HTML content.\n\n"
                    + "If Full Java Class is in response then wrap it in <code type=\"full\" class=\"java\">. "
                    + "If partial snippet of Java Class are in response then wrap it in <code type=\"snippet\" class=\"java\">. "
                    + "Orignal Java Class Content:\n" + classContent + "\n\n"
                    + "Previous Chat Response:\n" + previousChatResponse + "\n\n"
                    + "User Query:\n" + userQuery;
        }

        // Generate the HTML description
        String answer = generate(prompt);
        System.out.println(answer);
        return answer;
    }

    public List<Snippet> suggestNextLineCode(String fileContent, String currentLine, String mimeType) {
        StringBuilder description = new StringBuilder(MIME_TYPE_DESCRIPTIONS.getOrDefault(mimeType, "code snippets"));
        StringBuilder prompt = new StringBuilder("You are an API server that provides ").append(description).append(" suggestions based on the file content. ");
        if (currentLine == null || currentLine.isEmpty()) {
            prompt.append("Analyze the content and recommend appropriate additions at the placeholder ${SUGGEST_CODE_LIST}. ");
        } else {
            prompt.append("Analyze the content and the current line: \n")
                    .append(currentLine)
                    .append("\nRecommend appropriate additions at the placeholder ${SUGGEST_CODE_LIST}. ");
        }
        prompt.append("""
                  Ensure the suggestions align with the file's context and structure. 
                  Respond with a JSON array containing a few of the best options. 
                  Each entry should have one field, 'snippet', holding the recommended code block. 
                  The code block can contain multiple lines, formatted as a single string using \\n for line breaks.
                  
                  File Content:
                  """).append(fileContent);
        String jsonResponse = generate(prompt.toString());
        List<Snippet> nextLines = parseJsonToSnippets(jsonResponse);
        return nextLines;
    }

}

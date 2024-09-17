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
package io.github.jeddict.javadoc.ai;

/**
 *
 * @author Gaurav Gupta
 */
import dev.langchain4j.model.openai.OpenAiChatModel;

public class JavaDocChatModel {

    private final OpenAiChatModel aiChatModel;
    private final String gptModel = "gpt-4o-mini";
    private static final String API_KEY_ENV_VAR = "OPENAI_API_KEY";
    private static final String API_KEY_SYS_PROP = "openai.api.key";

    public static String getApiKey() {
        // First, try to get the API key from the environment variable
        String apiKey = System.getenv(API_KEY_ENV_VAR);
        if (apiKey == null || apiKey.isEmpty()) {
            // If not found in environment variable, try system properties
            apiKey = System.getProperty(API_KEY_SYS_PROP);
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API key is not set in environment variables or system properties.");
        }
        return apiKey;
    }

    public JavaDocChatModel() {
        aiChatModel = OpenAiChatModel.builder()
                .apiKey(getApiKey())
                .modelName(gptModel)
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

}

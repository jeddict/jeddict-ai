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
package io.github.jeddict.ai.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * A simple mock server for OpenAI endpoints.
 * <p>
 * This server listens for POST requests on the root path ("/"). It supports two
 * modes of operation: Mock Mode (default) and Chat Mode.
 * <p>
 * <b>Mock Mode (Default):</b>
 * <p>
 * It inspects the request body for an instruction to "use mock <file>", where
 * <file>
 * is the path to a mock file relative to "src/test/resources/mocks". The server
 * will respond with the content of the specified mock file wrapped in an
 * OpenAI-compatible JSON format.
 * <p>
 * The file name can be unquoted or enclosed in double quotes. For example:
 * <ul>
 * <li>use mock my_file.txt</li>
 * <li>use mock "my file with spaces.json"</li>
 * </ul>
 * <p>
 * <b>Chat Mode (enabled with --chat):</b>
 * <p>
 * In this mode, the server simulates a multi-turn conversation. It is triggered
 * by a request containing the instruction "start chat &lt;chat name&gt;". The
 * server loads responses sequentially from the specified chat directory:
 * <code>&lt;directory&gt;/&lt;chat name&gt;/res&lt;n&gt;.json</code>, where
 * <code>n</code> is the sequence number of the response (e.g., res1.json for
 * the first turn, res2.json for the second).
 * <p>
 * If the client requests more turns than available response files, the server
 * returns a 400 Bad Request. If the "start chat" instruction is missing or the
 * directory is not found, it returns a default response with usage
 * instructions.
 * <p>
 * To start the server, run the {@link #main(String[])} method from your IDE or
 * from the command line. By default, the server listens on port 8080. You can
 * specify a different port by passing it as a command line argument.
 * <p>
 * To start the server using Maven (requires test classpath):
 * <pre>
 * mvn install exec:java -DskipTests -Dexec.mainClass="io.github.jeddict.ai.test.AIMockServer" \
 * -Dexec.classpathScope="test" \
 * -Dexec.args="--port 8080 --chat src/test/resources/chat"
 * </pre>
 * <p>
 * To stop the server, interrupt the process (Ctrl+C).
 */
public class AIMockServer implements Runnable {

    @Option(names = "--port", description = "The TCP port to listen to", defaultValue = "8080")
    private int port;

    @Option(names = "--chat", description = "The path of the directory that stores chat mocks", defaultValue = "src/test/resources")
    private Path chatDirectory;

    public static void main(String[] args) {
        new CommandLine(new AIMockServer()).execute(args);
    }

    @Override
    public void run() {
        WireMockConfiguration config = WireMockConfiguration.wireMockConfig()
                .port(port)
                .extensions(new AIMockTransformer(chatDirectory));

        WireMockServer wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        System.out.println("WireMock Server started on port " + port);

        wireMockServer.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withTransformers("ai-mock-transformer")));
    }

    public static class AIMockTransformer extends ResponseDefinitionTransformer {

        private static final String DEFAULT_MOCK_FILE = "src/test/resources/mocks/default.txt";
        private static final String ERROR_MOCK_FILE = "src/test/resources/mocks/error.txt";

        private final Path chatDirectory;

        private static final Pattern MOCK_INSTRUCTION_PATTERN =
            Pattern.compile("use mock\\s+(?:'([^']+)'|(\\S+))", Pattern.CASE_INSENSITIVE);
        private static final Pattern CHAT_INSTRUCTION_PATTERN =
            Pattern.compile("start chat\\s+([^\\s\"]+)", Pattern.CASE_INSENSITIVE);

        public AIMockTransformer(Path chatDirectory) {
            this.chatDirectory = chatDirectory;
        }

        @Override
        public String getName() {
            return "ai-mock-transformer";
        }

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
            System.out.println(">>>>> " + System.currentTimeMillis());

            String body = request.getBodyAsString();
            System.out.println(body + "\n>>>>>\n<<<<< " + System.currentTimeMillis());

            try {
                Thread.sleep(50);

                // Special case: probing tools
                if (body.contains("You are an assistant to probe if a model supports tools")) {
                    return handleChat(body, "probe_tools");
                }

                // Chat Mode
                Matcher chatMatcher = CHAT_INSTRUCTION_PATTERN.matcher(body);
                if (chatMatcher.find()) {
                    return handleChat(body, chatMatcher.group(1));
                }

                // Mock Mode
                Matcher mockMatcher = MOCK_INSTRUCTION_PATTERN.matcher(body);
                Path mockPath = Path.of(DEFAULT_MOCK_FILE);

                if (mockMatcher.find()) {
                    String filename = mockMatcher.group(1) != null ? mockMatcher.group(1) : mockMatcher.group(2);
                    mockPath = Path.of("src/test/resources/mocks").resolve(filename);
                }

                return serveMockFile(mockPath);

            } catch (Exception e) {
                return new ResponseDefinitionBuilder()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Internal Server Error: " + e.getMessage())
                        .build();
            }
        }

        private ResponseDefinition handleChat(String body, String chatName) throws Exception {
            JSONObject json = new JSONObject(body);
            JSONArray messages = json.getJSONArray("messages");
            int n = (int) Math.ceil(messages.length() / 2.0);

            Path responseFile = chatDirectory.resolve(chatName).resolve("res" + n + ".json");

            if (Files.exists(responseFile)) {
                String content = Files.readString(responseFile, StandardCharsets.UTF_8);
                System.out.println(responseFile.toString() + '\n' + content + "\n<<<<<");
                return new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(content)
                        .build();
            } else {
                return new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Response file not found: " + responseFile)
                        .build();
            }
        }

        private ResponseDefinition serveMockFile(Path path) throws Exception {
             String errorMessage = null;
             if (!Files.exists(path)) {
                 errorMessage = "Mock file '" + path.toUri().getPath() + "' not found.";
                 path = Path.of(ERROR_MOCK_FILE);
             }

             String mockContent;
             try {
                 if (Files.exists(path)) {
                     mockContent = Files.readString(path, StandardCharsets.UTF_8);
                     System.out.println(path.toString() + '\n' + mockContent + "\n<<<<<");
                     if (errorMessage != null) {
                         mockContent = mockContent.replaceAll("\\{error}", errorMessage);
                     }
                 } else {
                     mockContent = errorMessage != null ? errorMessage : "Error: Mock file not found.";
                 }
             } catch (Exception x) {
                 mockContent = "Error reading mock file: " + x.getMessage();
             }

             String jsonResponse = wrapInJson(mockContent);
             return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(jsonResponse)
                    .build();
        }

        private String wrapInJson(String content) {
            return String.format("{\n  \"id\": \"chatcmpl-%s\",\n  \"object\": \"chat.completion\",\n  \"created\": %d,\n  \"choices\": [{\n    \"index\": 0,\n    \"message\": {\n      \"role\": \"assistant\",\n      \"content\": \"%s\"\n    },\n    \"finish_reason\": \"stop\"\n  }],\n  \"usage\": {\n    \"prompt_tokens\": 9,\n    \"completion_tokens\": 12,\n    \"total_tokens\": 21\n  }\n}",
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis() / 1000,
                    escapeJson(content)
            );
        }

        private String escapeJson(String text) {
            return text.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\b", "\\b")
                       .replace("\f", "\\f")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
        }
    }
}

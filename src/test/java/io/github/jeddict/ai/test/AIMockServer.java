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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple mock server for OpenAI endpoints.
 * <p>
 * This server listens for POST requests on the root path ("/").
 * It inspects the request body for an instruction to "use mock <file>", where <file>
 * is the path to a mock file relative to "src/test/resources/mocks".
 * <p>
 * The server will respond with the content of the specified mock file.
 * <p>
 * The file name can be unquoted or enclosed in double quotes.
 * For example:
 * <ul>
 *     <li>use mock my_file.txt</li>
 *     <li>use mock "my file with spaces.json"</li>
 * </ul>
 * <p>
 * To start the server, run the {@link #main(String[])} method from your IDE or
 * from the command line. By default, the server listens on port 8080.
 * You can specify a different port by passing it as a command line argument.
 * <p>
 * To stop the server, interrupt the process (Ctrl+C).
 */
public class AIMockServer {

    private static final String DEFAULT_MOCK_FILE = "src/test/resources/mocks/default.txt";
    private static final String ERROR_MOCK_FILE = "src/test/resources/mocks/error.txt";
    private static final Pattern MOCK_INSTRUCTION_PATTERN =
        Pattern.compile("use mock\\s+(?:'([^']+)'|(\\S+))", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Usage: java AIMockServer [port]");
                System.exit(1);
            }
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MockHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class MockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String error = "";

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            //
            // dump the body for inspection
            //
            File bodyFile = File.createTempFile("mockserver-", "-body");
            try (final FileWriter w = new FileWriter(bodyFile)) {
                w.append(body);
            }

            Matcher matcher = MOCK_INSTRUCTION_PATTERN.matcher(body);

            Path mockPath = Path.of(DEFAULT_MOCK_FILE);
            if (matcher.find()) {
                String mockFile = matcher.group(1); // Quoted file name
                if (mockFile == null) {
                    mockFile = matcher.group(2); // Unquoted file name
                }
                mockPath = Path.of("src/test/resources/mocks").resolve(mockFile).normalize();
            }

            if (!Files.exists(mockPath)) {
                error = "Mock file '" + mockPath + "' not found.";
                mockPath = Path.of(ERROR_MOCK_FILE);
            }

            try {
                String mockContent = Files.readString(mockPath, StandardCharsets.UTF_8);
                mockContent = mockContent.replaceAll("\\{error\\}", error);
                String jsonResponse = String.format("{\n  \"id\": \"chatcmpl-%s\",\n  \"object\": \"chat.completion\",\n  \"created\": %d,\n  \"choices\": [{\n    \"index\": 0,\n    \"message\": {\n      \"role\": \"assistant\",\n      \"content\": \"%s\"\n    },\n    \"finish_reason\": \"stop\"\n  }],\n  \"usage\": {\n    \"prompt_tokens\": 9,\n    \"completion_tokens\": 12,\n    \"total_tokens\": 21\n  }\n}",
                        UUID.randomUUID().toString(),
                        System.currentTimeMillis() / 1000,
                        escapeJson(mockContent)
                );

                sendResponse(exchange, 200, jsonResponse, "application/json");
            } catch (IOException ex) {
                sendResponse(exchange, 500, "Error reading mock file: " + ex.getMessage(), "text/plain");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
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
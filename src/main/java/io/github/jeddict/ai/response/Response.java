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
package io.github.jeddict.ai.response;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Gaurav Gupta
 */
public class Response {

    private final String query;
    private List<Block> blocks;
    private final Set<FileObject> messageContext;

    public Response(String query, String response,  Set<FileObject> messageContext) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("query can not be null or empty");
        }
        this.query = query;
        if (response == null || response.isBlank()) {
            response = "";
        }
        if (messageContext == null) {
            throw new IllegalArgumentException("messageContext can not be null");
        }
        this.messageContext = messageContext;

        this.blocks = parseMarkdown(response);
    }

    public String getQuery() {
        return query;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public Set<FileObject> getMessageContext() {
        return messageContext;
    }

    private List<Block> parseMarkdown(String text) {
        List<Block> result = new LinkedList<>();
        StringBuilder buffer = new StringBuilder();
        boolean insideCodeBlock = false;
        String currentFence = null;
        String blockType = "text"; // block type is text by default to match
                                   // heading text

        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher fenceMatcher = Pattern.compile("^(```+)(\\s*\\w+)?\\s*$").matcher(line);

            if (fenceMatcher.matches()) {
                String fence = fenceMatcher.group(1);
                String lang = fenceMatcher.group(2) != null ? fenceMatcher.group(2).trim() : "";

                if (!insideCodeBlock) {
                    // Starting code block
                    if (buffer.length() > 0) {
                        //
                        // NOTE: text outside block shall be trimmed
                        //
                        result.add(new Block(blockType, buffer.toString().trim()));
                        buffer.setLength(0);
                    }
                    insideCodeBlock = true;
                    currentFence = fence;
                    blockType = lang.isEmpty() ? "code" : lang;
                } else if (line.startsWith(currentFence)) {
                    // Ending code block
                    insideCodeBlock = false;
                    //
                    // NOTE: content outside blocks should not be trimmed
                    //
                    result.add(new Block(blockType, buffer.toString()));
                    buffer.setLength(0);
                    blockType = "text";
                }
                // Skip the fence line itself
            } else {
                buffer.append(line).append("\n");
            }
        }

        if (buffer.length() > 0) {
            result.add(new Block(blockType, buffer.toString().trim()));
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder responseBuilder = new StringBuilder();
        for (Block block : blocks) {
            switch (block.getType()) {
                case "text" -> responseBuilder.append(block.getContent()).append("\n");
                default -> responseBuilder.append("```").append(block.getType()).append("\n")
                            .append(block.getContent())
                            .append("```\n");
            }
        }
        return responseBuilder.toString().trim();
    }

}

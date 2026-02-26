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

import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.response.TextBlock;
import static io.github.jeddict.ai.util.EditorUtil.printBlock;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class MarkdownStreamParser {

    private final StringBuilder lineBuffer = new StringBuilder();
    private final StringBuilder blockBuffer = new StringBuilder();

    private boolean insideCodeBlock = false;
    private String currentFence = null;
    private String codeType = null;

    private static final Pattern FENCE_PATTERN = Pattern.compile("^(```+)(\\s*\\w+)?\\s*$");

    // Thread-safe queues for blocks
    private final ConcurrentLinkedQueue<TextBlock> pendingBlocks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TextBlock> doneBlocks = new ConcurrentLinkedQueue<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Listener interface to notify UI about new done blocks
    public interface BlockListener {

        void onBlockDone(TextBlock block);
    }

    private final BlockListener blockListener;
    private final AssistantChat assistantChat;

    public MarkdownStreamParser(BlockListener listener, AssistantChat topComponent) {
        this.blockListener = listener;
        this.assistantChat = topComponent;
        startBlockProcessor();
    }

    public void processToken(String token) {
        lineBuffer.append(token);

        int newlineIndex;
        while ((newlineIndex = findLineEnd(lineBuffer)) >= 0) {
            String line = lineBuffer.substring(0, newlineIndex);

            int removeLength = line.endsWith("\r") ? newlineIndex + 2 : newlineIndex + 1;
            lineBuffer.delete(0, removeLength);

            TextBlock completedBlock = processLine(line);
            if (completedBlock != null) {
                pendingBlocks.offer(completedBlock);
            }
        }
    }

    private int findLineEnd(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\n') {
                return i;
            }
        }
        return -1;
    }

    private TextBlock processLine(String line) {
        Matcher fenceMatcher = FENCE_PATTERN.matcher(line);

        if (fenceMatcher.matches()) {
            String fence = fenceMatcher.group(1);
            String lang = fenceMatcher.group(2) != null ? fenceMatcher.group(2).trim() : "";

            if (!insideCodeBlock) {
                if (blockBuffer.length() > 0) {
                    TextBlock textBlock = new TextBlock("text", blockBuffer.toString().trim());
                    blockBuffer.setLength(0);
                    insideCodeBlock = true;
                    currentFence = fence;
                    codeType = lang.isEmpty() ? "code" : lang;
                    return textBlock;
                } else {
                    insideCodeBlock = true;
                    currentFence = fence;
                    codeType = lang.isEmpty() ? "code" : lang;
                }
            } else if (line.startsWith(currentFence)) {
                TextBlock codeBlock = new TextBlock(codeType, blockBuffer.toString().trim());
                blockBuffer.setLength(0);
                insideCodeBlock = false;
                currentFence = null;
                codeType = null;
                return codeBlock;
            }
        } else {
            blockBuffer.append(line).append("\n");
        }

        return null;
    }

    public void flush() {
        if (blockBuffer.length() > 0) {
            TextBlock block = new TextBlock(insideCodeBlock ? codeType : "text", blockBuffer.toString().trim());
            blockBuffer.setLength(0);
            pendingBlocks.offer(block);
        }
    }

    private void startBlockProcessor() {
        StringBuilder code = new StringBuilder();
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    TextBlock block = pendingBlocks.poll();
                    if (block != null) {
                        // Here you can add processing logic if needed
                        SwingUtilities.invokeLater(() -> {
                            JComponent comp = printBlock(code, null, block, assistantChat);
                            comp.requestFocusInWindow();
                            comp.scrollRectToVisible(comp.getVisibleRect());
                            doneBlocks.offer(block);
                            if (blockListener != null) {
                                blockListener.onBlockDone(block);
                            }
                        });

                    } else {
                        // No blocks, sleep briefly to avoid busy wait
                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("MarkdownStreamParser executor did not terminate in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Optional getters if needed
    public ConcurrentLinkedQueue<TextBlock> getDoneBlocks() {
        return doneBlocks;
    }
}

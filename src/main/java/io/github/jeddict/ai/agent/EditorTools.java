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
package io.github.jeddict.ai.agent;

import dev.langchain4j.agent.tool.Tool;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.swing.text.Element;

/**
 *
 * @author Gaurav Gupta
 */
public class EditorTools extends AbstractCodeTool {

    public EditorTools(final String basedir) throws IOException {
        super(basedir);
    }

    /**
     * Inserts a line at the given line number (0-based).
     *
     * @param path the file path relative to the project
     * @param lineNumber the line number (0-based)
     * @param lineText the text of the new line
     * @return a status message
     */
    @Tool("Insert a line of code at a given line number (0-based) in a file by path")
    public String insertLineInFile(String path, int lineNumber, String lineText)
            throws Exception {
        progress("✏️ Inserting line at " + lineNumber + " in file: " + path);

        return withDocument(path, doc -> {
            try {
                Element root = doc.getDefaultRootElement();
                if (lineNumber < 0 || lineNumber > root.getElementCount()) {
                    progress("⚠️ Invalid line number " + lineNumber + " for file: " + path);
                    return "Invalid line number: " + lineNumber;
                }

                int offset = (lineNumber == root.getElementCount())
                        ? doc.getLength()
                        : root.getElement(lineNumber).getStartOffset();

                doc.insertString(offset, lineText + System.lineSeparator(), null);

                progress("✅ Inserted line at " + lineNumber + " in file: " + path);
                return "Inserted line at " + lineNumber;
            } catch (Exception e) {
                progress("❌ Line insert failed: " + e.getMessage() + " in file: " + path);
                throw e;
            }
        }, true);
    }

    /**
     * Inserts a line after the end of a specified Java method or constructor in
     * the file. Uses findInsertionLineAfterMethod to get a robust insertion
     * point.
     *
     * @param path the file path relative to the project
     * @param methodName the method or constructor name where to insert after
     * @param lineText the text to insert as a new line
     * @return status message
     */
    @Tool("Insert a line of code at a given line number (0-based) in a file by path")
    public String insertLineAfterMethod(String path, String methodName, String lineText)
            throws Exception {
        progress("✏️ Inserting line after method '" + methodName + "' in file: " + path);
        String content = withDocument(path, doc -> doc.getText(0, doc.getLength()), false);
        if (content.startsWith("Could not")) {
            progress("❌ Failed to read file: " + path);
            return "Failed to read file: " + content;
        }
        int insertLine = findInsertionLineAfterMethod(content, methodName);
        if (insertLine < 0) {
            progress("⚠️ Method not found: " + methodName + " in file " + path);
            return "Method not found: " + methodName;
        }
        progress("✅ Inserting text at line " + insertLine + " in file: " + path);
        return insertLineInFile(path, insertLine, lineText);
    }

    /**
     * Find a line number to insert code after the end of a Java method or
     * constructor. This method heuristically scans the file content lines to
     * find the closing brace of the method. It accounts for Javadoc and nested
     * braces for accurate placement.
     *
     * @param fileContent the full Java source code as a String
     * @param methodName the method or constructor name to find
     * @return the line number after the method ends, or -1 if not found
     */
    private int findInsertionLineAfterMethod(String fileContent, String methodName) {
        String[] lines = fileContent.split("\r?\n");
        int braceDepth = 0;
        boolean inMethod = false;

        Pattern methodPattern = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(.*\\)\\s*\\{\\s*$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!inMethod) {
                // Detect method declaration line
                if (methodPattern.matcher(line).find()) {
                    inMethod = true;
                    // Count opening brace
                    braceDepth = 1;
                }
            } else {
                // Inside method, track braces to find method end
                braceDepth += countChar(line, '{');
                braceDepth -= countChar(line, '}');
                if (braceDepth == 0) {
                    // Method ends here
                    return i + 1; // return line after method end
                }
            }
        }
        return -1;
    }

    /**
     * Counts occurrences of a character in a string.
     *
     * @param line the string to search
     * @param ch the character to count
     * @return count of characters found
     */
    private int countChar(String line, char ch) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ch) {
                count++;
            }
        }
        return count;
    }
}

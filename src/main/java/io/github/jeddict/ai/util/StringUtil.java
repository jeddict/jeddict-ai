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
package io.github.jeddict.ai.util;

/**
 *
 * @author Gaurav Gupta
 */
public class StringUtil {

    public static String convertToCapitalized(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public static String removeCodeBlockMarkers(String input) {
        // Check if the input starts and ends with the markers
        input = input.trim();
        if (input.startsWith("```java") && input.endsWith("```")) {
            String content = input.substring(7);  // Remove ```java\n (7 characters)
            content = content.substring(0, content.length() - 3);  // Remove ```
            input = content.trim();
        } else if (input.startsWith("```json") && input.endsWith("```")) {
            String content = input.substring(7);  // Remove ```java\n (7 characters)
            content = content.substring(0, content.length() - 3);  // Remove ```
            input = content.trim();
        } else if (input.startsWith("```html") && input.endsWith("```")) {
            String content = input.substring(7);  // Remove ```java\n (7 characters)
            content = content.substring(0, content.length() - 3);  // Remove ```
            input = content.trim();
        } else if (input.startsWith("```") && input.endsWith("```")) {
            String content = input.substring(3);  // Remove ```java\n (7 characters)
            content = content.substring(0, content.length() - 3);  // Remove ```
            input = content.trim();
        }
        input = input.trim();
        if (input.startsWith("/**") && input.endsWith("*/")) {
            input = input.substring(3, input.length() - 2).trim();
        }
        return input;  // Return the original input if it does not match the expected format
    }

    public static String trimLeadingSpaces(String str) {
        if (str == null) {
            return null;
        }
        int start = 0;
        while (start < str.length() && Character.isWhitespace(str.charAt(start))) {
            start++;
        }
        return str.substring(start);
    }

    public static String trimTrailingSpaces(String str) {
        if (str == null) {
            return null;
        }
        int end = str.length();
        while (end > 0 && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }
        return str.substring(0, end);
    }

    // remove all space from string
    public static String removeAllSpaces(String str) {
        if (str == null) {
            return null;
        } else {
            return str.replaceAll("\\s+", "");
        }
    }

}

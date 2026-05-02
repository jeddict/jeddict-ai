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
package io.github.jeddict.ai.settings;

/**
 *
 * @author Gaurav Gupta
 */
public enum AIClassContext {
    CURRENT_CLASS("Current Class", "Focuses on the current class being analyzed."),
    REFERENCED_CLASSES("Referenced Classes", "Includes all classes that are referenced from the current class."),
    CURRENT_PACKAGE("Current Package", "Examines all classes within the current package, as well as those referenced by the current class."),
    ENTIRE_PROJECT("Entire Project", "Covers all classes in the entire project.");
//    ACTIVE_TEXT_EDITOR("Currently Opened Editor", "Includes all classes that are currently open in the editor.");

    private final String displayName;
    private final String description;

    AIClassContext(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }

}

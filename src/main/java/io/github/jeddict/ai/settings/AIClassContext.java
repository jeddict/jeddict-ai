/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.settings;

/**
 * Enumeration for GPT models used in AI analysis.
 *
 * Author: Gaurav Gupta
 */
public enum GPTModel {
    GPT_4O_MINI("gpt-4o-mini", "Cost-effective and fast, making it ideal for quick assessments. Highly recommended!"),
    GPT_4_TURBO("gpt-4-turbo", "Balanced cost and performance."),
    GPT_4O("gpt-4o", "High-cost and best for in-depth analysis.");

    private final String displayName;
    private final String description;

    GPTModel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedInfo() {
        return String.format("%s: %s", displayName, description);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import java.awt.BorderLayout;
import java.util.prefs.Preferences;
import javax.swing.JEditorPane;
import org.openide.windows.TopComponent;

/**
 *
 * @author Gaurav Gupta
 */
public class AssistantTopComponent extends TopComponent {

    public static final String PREFERENCE_KEY = "AssistantTopComponentOpen";
    private final JEditorPane editorPane;

    public AssistantTopComponent(String name) {
        setName(name);
        setLayout(new BorderLayout());

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        add(editorPane, BorderLayout.CENTER);
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        boolean shouldOpen = prefs.getBoolean(PREFERENCE_KEY, true);
        if (!shouldOpen) {
            this.close();
        }
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.putBoolean(PREFERENCE_KEY, false);
    }

    public JEditorPane getEditorPane() {
        return editorPane;
    }
}

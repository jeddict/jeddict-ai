/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.util;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Gaurav Gupta
 */
public class UIUtil {
         public  static String askQuery() {
        // Create a JTextArea for multiline input
        JTextArea textArea = new JTextArea(10, 30); // 10 rows, 30 columns
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        // Add the text area to a JScrollPane
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Create a JPanel to hold the scroll pane
        JPanel panel = new JPanel();
        panel.add(scrollPane);

        // Show the custom dialog
        int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Please provide details about what to update in this method:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Check the user's choice
        if (option != JOptionPane.OK_OPTION) {
            return null; // Exit if the user cancels the input
        }

        String query = textArea.getText().trim();

        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Update details are required. Operation aborted.",
                    "No Input",
                    JOptionPane.ERROR_MESSAGE
            );
            return null; // Exit if no input is provided
        }
        return query;
    }
        
         public  static String askQueryAboutClass() {
        // Create a JTextArea for multiline input
        JTextArea textArea = new JTextArea(10, 30); // 10 rows, 30 columns
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        // Add the text area to a JScrollPane
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Create a JPanel to hold the scroll pane
        JPanel panel = new JPanel();
        panel.add(scrollPane);

        // Show the custom dialog
        int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Please ask the query about this class.",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Check the user's choice
        if (option != JOptionPane.OK_OPTION) {
            return null; // Exit if the user cancels the input
        }

        String query = textArea.getText().trim();

        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Query is required. Operation aborted.",
                    "No Input",
                    JOptionPane.ERROR_MESSAGE
            );
            return null; // Exit if no input is provided
        }
        return query;
    }
}

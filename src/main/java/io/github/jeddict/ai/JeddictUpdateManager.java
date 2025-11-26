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
package io.github.jeddict.ai;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.swing.*;
import javax.xml.parsers.*;
import org.openide.util.NbPreferences;
import org.w3c.dom.*;

public class JeddictUpdateManager {

    private static final String PREF_KEY_SHOW_POPUP = "jeddict.showUpdatePopup" + getCurrentJeddictVersion();
    private static boolean checked = false;
    private static final String PORTAL_URL = "https://plugins.netbeans.apache.org/catalogue/?id=103";
    private static final String LEARN_URL = "https://jeddict.github.io/page.html?l=tutorial/AI";

    private static String getCurrentNetBeansVersion() {
        return "28";
    }

    private static String getCurrentJeddictVersion() {
        return "3.3.0";
    }

    private File saveFile;

    public void checkForJeddictUpdate() {
        if (checked) {
            return;
        } else {
            checked = true;
        }
        String currentNetBeansVersion = getCurrentNetBeansVersion(); // Detect current NetBeans version
        String xmlUrl = "https://jeddict.github.io/release/jeddict-ai.xml"; // URL to the XML file

        try {
            URL url = new URL(xmlUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.out.println("Failed to fetch XML: " + connection.getResponseCode());
                return;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(connection.getInputStream());
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("release");
            String nbmFile = null;
            String version = null;
            String releaseNotes = null;
            // Iterate through each release node
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    version = element.getElementsByTagName("version").item(0).getTextContent();
                    String compatibleNetBeansVersion = element.getElementsByTagName("compatibleNetBeansVersion").item(0).getTextContent();
                    nbmFile = element.getElementsByTagName("nbmFile").item(0).getTextContent();
                    releaseNotes = element.getElementsByTagName("releaseNotes").item(0).getTextContent();
                    if (compatibleNetBeansVersion.equals(currentNetBeansVersion)
                            && isVersionGreater(version, getCurrentJeddictVersion())) {
                        break;
                    } else {
                        nbmFile = null;
                        version = null;
                    }
                }
            }

            if (nbmFile != null) {
                showInstallPopup(version, releaseNotes, nbmFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to compare version strings
    private boolean isVersionGreater(String newVersion, String currentVersion) {
        String[] newVersionParts = newVersion.split("\\.");
        String[] currentVersionParts = currentVersion.split("\\.");

        for (int i = 0; i < Math.max(newVersionParts.length, currentVersionParts.length); i++) {
            int newPart = (i < newVersionParts.length) ? Integer.parseInt(newVersionParts[i]) : 0;
            int currentPart = (i < currentVersionParts.length) ? Integer.parseInt(currentVersionParts[i]) : 0;

            if (newPart > currentPart) {
                return true;
            } else if (newPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private void showInstallPopup(String version, String releaseNotes, String nbmUrl) {

        if (loadPreference(PREF_KEY_SHOW_POPUP)) {
            return;
        }

        JDialog dialog = new JDialog();
        dialog.setTitle("Update Available");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        dialog.setPreferredSize(new Dimension(450, 300)); // Slightly increased height for better spacing

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("<html><h2>A new update is available for Jeddict AI Assistant!</h2></html>");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel infoLabel = new JLabel("<html>Version: " + version + "<br>"
                +  ((releaseNotes != null && !releaseNotes.trim().isEmpty()) ? (releaseNotes + "<br>") : "")
                + "Click below to download the update or learn more.</html>");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox doNotShowAgainCheckbox = new JCheckBox("Do not show again");
        doNotShowAgainCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align checkbox

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0)); // Increased horizontal spacing
        JButton downloadButton = new JButton("Download Now");
        downloadButton.addActionListener(e -> {
            downloadAndInstallModule(version, nbmUrl);
        });

        JButton whatsNewButton = createStyledButton("What's New", LEARN_URL);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(downloadButton);
        buttonPanel.add(whatsNewButton);

        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.setLayout(new GridLayout(1, 2, 10, 0)); // Increased horizontal spacing
        JButton viewButton = createStyledButton("Visit Plugin Portal", PORTAL_URL);
        buttonPanel2.add(viewButton);
        buttonPanel2.add(cancelButton);

        // Add components to main panel
        mainPanel.add(titleLabel);
        mainPanel.add(infoLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(doNotShowAgainCheckbox);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15))); // More space after checkbox
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15))); // Space before second button panel
        mainPanel.add(buttonPanel2);

        dialog.getContentPane().add(mainPanel);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (doNotShowAgainCheckbox.isSelected()) {
                    savePreference(PREF_KEY_SHOW_POPUP, true);
                }
            }
        });

        dialog.pack();
        dialog.setVisible(true);
    }

    private boolean loadPreference(String key) {
        return NbPreferences.forModule(JeddictUpdateManager.class).getBoolean(key, false);
    }

    private void savePreference(String key, boolean value) {
        NbPreferences.forModule(JeddictUpdateManager.class).putBoolean(key, value);
    }

    private JButton createStyledButton(String text, String url) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(60, 20)); // Set the preferred size of the buttons
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Optional: Set rounded border
        button.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return button;
    }

    private void downloadAndInstallModule(String version, String nbmUrl) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    URL url = new URL(nbmUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        System.out.println("Failed to download NBM: " + connection.getResponseCode());
                        return null;
                    }

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save NBM Module");
                    fileChooser.setSelectedFile(new File("Jeddict-AI-Assistant-" + version + ".nbm"));

                    int userSelection = fileChooser.showSaveDialog(null);
                    if (userSelection != JFileChooser.APPROVE_OPTION) {
                        System.out.println("Save operation was canceled.");
                        return null;
                    }

                    saveFile = fileChooser.getSelectedFile();

                    try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(saveFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    System.out.println("Module downloaded successfully: " + saveFile.getAbsolutePath());

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(null,
                        "The NBM file has been downloaded successfully to:\n"
                        + saveFile.getAbsolutePath() + "\n\n"
                        + "To install the downloaded module, please follow these steps:\n"
                        + "1. Open NetBeans.\n"
                        + "2. Navigate to the 'Tools' > 'Plugins' window.\n"
                        + "3. Click on 'Downloaded' tab.\n"
                        + "4. Click 'Add Plugins...' and select the downloaded file.\n"
                        + "5. Follow the installation prompts.",
                        "Download Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        };

        worker.execute();
    }

}

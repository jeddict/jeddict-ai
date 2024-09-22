/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/NetBeansModuleDevelopment-files/template_mypluginPanel.java to edit this template
 */
package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.scanner.ProjectClassScanner;
import javax.swing.JOptionPane;

final class AIAssistancePanel extends javax.swing.JPanel {

    private final AIAssistanceOptionsPanelController controller;

    AIAssistancePanel(AIAssistanceOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jLayeredPane7 = new javax.swing.JLayeredPane();
        classContextLabel = new javax.swing.JLabel();
        classContextHelp = new javax.swing.JLabel();
        classContextComboBox = new javax.swing.JComboBox<>();
        jLayeredPane5 = new javax.swing.JLayeredPane();
        jLayeredPane6 = new javax.swing.JLayeredPane();
        gptModelLabel = new javax.swing.JLabel();
        gptModelHelp = new javax.swing.JLabel();
        gptModelComboBox = new javax.swing.JComboBox<>();
        jLayeredPane4 = new javax.swing.JLayeredPane();
        aiAssistantActivationCheckBox = new javax.swing.JCheckBox();
        enableHintCheckBox = new javax.swing.JCheckBox();
        enableSmartCodeCheckBox = new javax.swing.JCheckBox();
        jLayeredPane3 = new javax.swing.JLayeredPane();
        resetKeyButton = new javax.swing.JButton();
        cleanDataButton = new javax.swing.JButton();
        jLayeredPane8 = new javax.swing.JLayeredPane();

        jLayeredPane1.setLayout(new java.awt.GridLayout(0, 1, 0, 15));

        jLayeredPane2.setLayout(new java.awt.GridLayout(0, 1));

        jLayeredPane7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.openide.awt.Mnemonics.setLocalizedText(classContextLabel, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.classContextLabel.text")); // NOI18N
        jLayeredPane7.add(classContextLabel);

        classContextHelp.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        classContextHelp.setForeground(new java.awt.Color(100, 100, 100));
        org.openide.awt.Mnemonics.setLocalizedText(classContextHelp, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.classContextHelp.text")); // NOI18N
        jLayeredPane7.add(classContextHelp);

        jLayeredPane2.add(jLayeredPane7);

        classContextComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(AIClassContext.values()));
        classContextComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.classContextComboBox.toolTipText")); // NOI18N
        classContextComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classContextComboBoxActionPerformed(evt);
            }
        });
        jLayeredPane2.add(classContextComboBox);

        jLayeredPane1.add(jLayeredPane2);

        jLayeredPane5.setLayout(new java.awt.GridLayout(0, 1));

        jLayeredPane6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.openide.awt.Mnemonics.setLocalizedText(gptModelLabel, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.gptModelLabel.text")); // NOI18N
        jLayeredPane6.add(gptModelLabel);

        gptModelHelp.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        gptModelHelp.setForeground(new java.awt.Color(100, 100, 100));
        org.openide.awt.Mnemonics.setLocalizedText(gptModelHelp, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.gptModelHelp.text")); // NOI18N
        jLayeredPane6.add(gptModelHelp);

        jLayeredPane5.add(jLayeredPane6);

        gptModelComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(GPTModel.values()));
        gptModelComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.gptModelComboBox.toolTipText")); // NOI18N
        gptModelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gptModelComboBoxActionPerformed(evt);
            }
        });
        jLayeredPane5.add(gptModelComboBox);

        jLayeredPane1.add(jLayeredPane5);

        jLayeredPane4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.openide.awt.Mnemonics.setLocalizedText(aiAssistantActivationCheckBox, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.aiAssistantActivationCheckBox.text")); // NOI18N
        aiAssistantActivationCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.aiAssistantActivationCheckBox.toolTipText")); // NOI18N
        aiAssistantActivationCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aiAssistantActivationCheckBoxActionPerformed(evt);
            }
        });
        jLayeredPane4.add(aiAssistantActivationCheckBox);

        org.openide.awt.Mnemonics.setLocalizedText(enableHintCheckBox, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.enableHintCheckBox.text")); // NOI18N
        enableHintCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.enableHintCheckBox.toolTipText")); // NOI18N
        enableHintCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableHintCheckBoxActionPerformed(evt);
            }
        });
        jLayeredPane4.add(enableHintCheckBox);

        org.openide.awt.Mnemonics.setLocalizedText(enableSmartCodeCheckBox, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.enableSmartCodeCheckBox.text")); // NOI18N
        enableSmartCodeCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.enableSmartCodeCheckBox.toolTipText")); // NOI18N
        jLayeredPane4.add(enableSmartCodeCheckBox);

        jLayeredPane1.add(jLayeredPane4);

        jLayeredPane3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.openide.awt.Mnemonics.setLocalizedText(resetKeyButton, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.resetKeyButton.text")); // NOI18N
        resetKeyButton.setToolTipText(org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.resetKeyButton.toolTipText")); // NOI18N
        resetKeyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetKeyButtonActionPerformed(evt);
            }
        });
        jLayeredPane3.add(resetKeyButton);

        org.openide.awt.Mnemonics.setLocalizedText(cleanDataButton, org.openide.util.NbBundle.getMessage(AIAssistancePanel.class, "AIAssistancePanel.cleanDataButton.text")); // NOI18N
        cleanDataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanDataButtonActionPerformed(evt);
            }
        });
        jLayeredPane3.add(cleanDataButton);

        jLayeredPane1.add(jLayeredPane3);

        javax.swing.GroupLayout jLayeredPane8Layout = new javax.swing.GroupLayout(jLayeredPane8);
        jLayeredPane8.setLayout(jLayeredPane8Layout);
        jLayeredPane8Layout.setHorizontalGroup(
            jLayeredPane8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jLayeredPane8Layout.setVerticalGroup(
            jLayeredPane8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLayeredPane1.add(jLayeredPane8);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 613, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(90, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void classContextComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classContextComboBoxActionPerformed
        AIClassContext selectedContext = (AIClassContext) classContextComboBox.getSelectedItem();
        if (selectedContext != null) {
            classContextHelp.setText(selectedContext.getDescription());
        }
    }//GEN-LAST:event_classContextComboBoxActionPerformed

    private void aiAssistantActivationCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aiAssistantActivationCheckBoxActionPerformed
        // if unchecked then disable the hint and smart checkbox
        if (!aiAssistantActivationCheckBox.isSelected()) {
            enableHintCheckBox.setEnabled(false);
            enableSmartCodeCheckBox.setEnabled(false);
        } else {
            enableHintCheckBox.setEnabled(true);
            enableSmartCodeCheckBox.setEnabled(true);
        }
    }//GEN-LAST:event_aiAssistantActivationCheckBoxActionPerformed

    private void enableHintCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableHintCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enableHintCheckBoxActionPerformed

    private void gptModelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gptModelComboBoxActionPerformed
        GPTModel selectedContext = (GPTModel) gptModelComboBox.getSelectedItem();
        if (selectedContext != null) {
            gptModelHelp.setText(selectedContext.getDescription());
        }
    }//GEN-LAST:event_gptModelComboBoxActionPerformed

    private void resetKeyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetKeyButtonActionPerformed
        preferencesManager.clearApiKey();
        JOptionPane.showMessageDialog(this, "Your API key has been reset successfully!", "Information", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_resetKeyButtonActionPerformed

    private void cleanDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanDataButtonActionPerformed
        ProjectClassScanner.clear();
        JOptionPane.showMessageDialog(this, "Cache has been cleared successfully!", "Information", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_cleanDataButtonActionPerformed

    private final PreferencesManager preferencesManager = PreferencesManager.getInstance();

    void load() {
        aiAssistantActivationCheckBox.setSelected(preferencesManager.isAiAssistantActivated());
        classContextComboBox.setSelectedItem(preferencesManager.getClassContext());
        gptModelComboBox.setSelectedItem(preferencesManager.getGptModel());
        enableHintCheckBox.setSelected(preferencesManager.isHintsEnabled());
        enableSmartCodeCheckBox.setSelected(preferencesManager.isSmartCodeEnabled());

        if (!aiAssistantActivationCheckBox.isSelected()) {
            enableHintCheckBox.setEnabled(false);
            enableSmartCodeCheckBox.setEnabled(false);
        } else {
            enableHintCheckBox.setEnabled(true);
            enableSmartCodeCheckBox.setEnabled(true);
        }
    }

    void store() {
        preferencesManager.setAiAssistantActivated(aiAssistantActivationCheckBox.isSelected());
        preferencesManager.setClassContext((AIClassContext) classContextComboBox.getSelectedItem());
        preferencesManager.setGptModel((GPTModel) gptModelComboBox.getSelectedItem());
        preferencesManager.setHintsEnabled(enableHintCheckBox.isSelected());
        preferencesManager.setSmartCodeEnabled(enableSmartCodeCheckBox.isSelected());
    }

    boolean valid() {
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox aiAssistantActivationCheckBox;
    private javax.swing.JComboBox<AIClassContext> classContextComboBox;
    private javax.swing.JLabel classContextHelp;
    private javax.swing.JLabel classContextLabel;
    private javax.swing.JButton cleanDataButton;
    private javax.swing.JCheckBox enableHintCheckBox;
    private javax.swing.JCheckBox enableSmartCodeCheckBox;
    private javax.swing.JComboBox<GPTModel> gptModelComboBox;
    private javax.swing.JLabel gptModelHelp;
    private javax.swing.JLabel gptModelLabel;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JLayeredPane jLayeredPane4;
    private javax.swing.JLayeredPane jLayeredPane5;
    private javax.swing.JLayeredPane jLayeredPane6;
    private javax.swing.JLayeredPane jLayeredPane7;
    private javax.swing.JLayeredPane jLayeredPane8;
    private javax.swing.JButton resetKeyButton;
    // End of variables declaration//GEN-END:variables
}
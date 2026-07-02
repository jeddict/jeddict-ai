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
package io.github.jeddict.ai.project;

import io.github.jeddict.ai.settings.PreferencesManager;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;

/**
 *
 * @author Gaurav Gupta
 */
public class JeddictAIPropertiesPanel extends JPanel {

    private final Project project;
    private final PreferencesManager pm = PreferencesManager.getInstance();

    public JeddictAIPropertiesPanel(ModelHandle2 handle, Project project) {
        this.project = project;
        initComponents();
        projectRulesTextArea.setText(pm.getProjectRules(project));
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectRulesLayeredPane = new javax.swing.JLayeredPane();
        projectRulesLabel = new javax.swing.JLabel();
        projectRulesScrollPane = new javax.swing.JScrollPane();
        projectRulesTextArea = new javax.swing.JTextArea();

        setLayout(new java.awt.GridLayout(0, 1));

        org.openide.awt.Mnemonics.setLocalizedText(projectRulesLabel, org.openide.util.NbBundle.getMessage(JeddictAIPropertiesPanel.class, "JeddictAIPropertiesPanel.projectRulesLabel.text")); // NOI18N

        projectRulesTextArea.setColumns(20);
        projectRulesTextArea.setRows(5);
        projectRulesScrollPane.setViewportView(projectRulesTextArea);

        projectRulesLayeredPane.setLayer(projectRulesLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
        projectRulesLayeredPane.setLayer(projectRulesScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout projectRulesLayeredPaneLayout = new javax.swing.GroupLayout(projectRulesLayeredPane);
        projectRulesLayeredPane.setLayout(projectRulesLayeredPaneLayout);
        projectRulesLayeredPaneLayout.setHorizontalGroup(
            projectRulesLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectRulesLayeredPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectRulesLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectRulesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE)
                    .addGroup(projectRulesLayeredPaneLayout.createSequentialGroup()
                        .addComponent(projectRulesLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        projectRulesLayeredPaneLayout.setVerticalGroup(
            projectRulesLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectRulesLayeredPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectRulesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(projectRulesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );

        add(projectRulesLayeredPane);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel projectRulesLabel;
    private javax.swing.JLayeredPane projectRulesLayeredPane;
    private javax.swing.JScrollPane projectRulesScrollPane;
    private javax.swing.JTextArea projectRulesTextArea;
    // End of variables declaration//GEN-END:variables

    public void applyChanges() {
        pm.setProjectRules(project, projectRulesTextArea.getText());
    }

}

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
package io.github.jeddict.ai.file;

import io.github.jeddict.ai.util.SourceGroupUISupport;
import static io.github.jeddict.ai.util.ProjectUtil.getFolderSourceGroup;
import static io.github.jeddict.ai.util.ProjectUtil.getJavaSourceGroups;
import static io.github.jeddict.ai.util.ProjectUtil.getPackageForFolder;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import static org.openide.util.NbBundle.getMessage;

public class GenerateFilePanelVisual extends javax.swing.JPanel implements DocumentListener {

    private final WizardDescriptor wizard;
    private final Project project;
    private final JTextComponent packageComboBoxEditor;
    private final ChangeSupport changeSupport = new ChangeSupport(this);

    private static String previousDirectory;
    private static final PreferencesManager prefsManager = PreferencesManager.getInstance();

    /**
     * Creates new form CrudSetupPanel
     */
    public GenerateFilePanelVisual(Project project, WizardDescriptor wizard) {
        this.project = project;
        this.wizard = wizard;
        initComponents();

        packageComboBoxEditor = ((JTextComponent) packageComboBox.getEditor().getEditorComponent());
        Document packageComboBoxDocument = packageComboBoxEditor.getDocument();
        packageComboBoxDocument.addDocumentListener(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        headerLabel = new javax.swing.JLabel();
        projectLabel = new javax.swing.JLabel();
        projectTextField = new javax.swing.JTextField();
        locationLabel = new javax.swing.JLabel();
        locationComboBox = new javax.swing.JComboBox();
        packageLabel = new javax.swing.JLabel();
        packageComboBox = new javax.swing.JComboBox();
        fileNameLabel = new javax.swing.JLabel();
        fileNameTextField = new javax.swing.JTextField();
        contextFileLabel = new javax.swing.JLabel();
        contextFileTextField = new javax.swing.JTextField();
        jBtnBrowse = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        promptBox = new javax.swing.JComboBox<>();
        contextLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        contextArea = new javax.swing.JTextArea();

        setName(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "DocWizardDescriptor_displayName")); // NOI18N

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("io/github/jeddict/ai/file/Bundle"); // NOI18N
        headerLabel.setText(bundle.getString("GenerateFileWizardDescriptor_displayDescription")); // NOI18N
        headerLabel.setToolTipText(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "GenerateFileWizardDescriptor_displayDescription")); // NOI18N

        projectLabel.setLabelFor(projectTextField);
        projectLabel.setText(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Project")); // NOI18N

        projectTextField.setEditable(false);

        locationLabel.setLabelFor(locationComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(locationLabel, org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_SrcLocation")); // NOI18N

        locationComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationComboBoxActionPerformed(evt);
            }
        });

        packageLabel.setLabelFor(packageComboBox);
        packageLabel.setText(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Package")); // NOI18N
        packageLabel.setToolTipText("Define the package where this file will be created. Helps with organization and imports.");

        packageComboBox.setEditable(true);

        fileNameLabel.setText(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_FileName")); // NOI18N
        fileNameLabel.setToolTipText("Specify the name of the new file. Ensure it follows standard naming conventions.");

        fileNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fileNameTextFieldKeyReleased(evt);
            }
        });

        contextFileLabel.setText(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_JSONFile")); // NOI18N
        contextFileLabel.setToolTipText("Choose an existing file to provide context for AI generation. The AI will analyze this file to generate relevant code.");

        contextFileTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                contextFileTextFieldKeyReleased(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jBtnBrowse, org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Browse")); // NOI18N
        jBtnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnBrowseActionPerformed(evt);
            }
        });

        jLabel1.setText("Prompt :");

        promptBox.setModel(getPromptModel());

        contextLabel.setText("Context :");
        contextLabel.setToolTipText("Describe the purpose of the new file. You can specify design patterns, functionality, or special requirements.");

        contextArea.setColumns(20);
        contextArea.setRows(5);
        jScrollPane1.setViewportView(contextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerLabel)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(contextLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(locationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(packageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(contextFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(fileNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(locationComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projectTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(packageComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(contextFileTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnBrowse))
                    .addComponent(fileNameTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(promptBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(headerLabel)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectLabel)
                    .addComponent(projectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(locationLabel)
                    .addComponent(locationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(packageComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(packageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fileNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(contextFileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(contextFileLabel)
                    .addComponent(jBtnBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(promptBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contextLabel)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        headerLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "DocWizardDescriptor_displayName")); // NOI18N
        headerLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "DocWizardDescriptor_displayName")); // NOI18N
        projectLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Project")); // NOI18N
        projectLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Project")); // NOI18N
        projectTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "ACSD_Project")); // NOI18N
        locationLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_SrcLocation")); // NOI18N
        locationLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_SrcLocation")); // NOI18N
        locationComboBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "ACSD_Location")); // NOI18N
        packageLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Package")); // NOI18N
        packageLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "LBL_Package")); // NOI18N
        packageComboBox.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "ACSD_Package")); // NOI18N
        fileNameTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "ACSD_FileName")); // NOI18N
        contextFileTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(GenerateFilePanelVisual.class, "ACSD_JSONFile")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void locationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationComboBoxActionPerformed
        locationChanged();
    }//GEN-LAST:event_locationComboBoxActionPerformed

    private void fileNameTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fileNameTextFieldKeyReleased
        changeSupport.fireChange();
    }//GEN-LAST:event_fileNameTextFieldKeyReleased

    private void contextFileTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contextFileTextFieldKeyReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_contextFileTextFieldKeyReleased

    private void jBtnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnBrowseActionPerformed
        SourceGroup sourceGroup = (SourceGroup) locationComboBox.getSelectedItem();
        JFileChooser chooser = new JFileChooser(previousDirectory == null ? sourceGroup.getRootFolder().getPath() : previousDirectory);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            contextFileTextField.setText(selectedFile.getAbsolutePath());
            previousDirectory = selectedFile.getPath();
        }
    }//GEN-LAST:event_jBtnBrowseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea contextArea;
    private javax.swing.JLabel contextFileLabel;
    private javax.swing.JTextField contextFileTextField;
    private javax.swing.JLabel contextLabel;
    private javax.swing.JLabel fileNameLabel;
    private javax.swing.JTextField fileNameTextField;
    private javax.swing.JLabel headerLabel;
    private javax.swing.JButton jBtnBrowse;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox locationComboBox;
    private javax.swing.JLabel locationLabel;
    private javax.swing.JComboBox packageComboBox;
    private javax.swing.JLabel packageLabel;
    private javax.swing.JLabel projectLabel;
    private javax.swing.JTextField projectTextField;
    private javax.swing.JComboBox<String> promptBox;
    // End of variables declaration//GEN-END:variables

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    boolean valid(WizardDescriptor wizard) {
        String fileName = getFileName();
        if (fileName.trim().equals("")) { // NOI18N
            wizard.putProperty("WizardPanel_errorMessage", getMessage(GenerateFilePanelVisual.class, "ERR_JavaTargetChooser_CantUseEmptyFileName"));
            return false;
        }

        String packageName = getPackage();
        if (packageName.trim().equals("")) { // NOI18N
            wizard.putProperty("WizardPanel_errorMessage", getMessage(GenerateFilePanelVisual.class, "ERR_JavaTargetChooser_CantUseDefaultPackage"));
            return false;
        }

//        if (!JavaIdentifiers.isValidPackageName(packageName)) {
//            wizard.putProperty("WizardPanel_errorMessage", getMessage(DocSetupPanelVisual.class, "ERR_JavaTargetChooser_InvalidPackage")); //NOI18N
//            return false;
//        }
//
//        if (!isFolderWritable(getLocationValue(), packageName)) {
//            wizard.putProperty("WizardPanel_errorMessage", getMessage(DocSetupPanelVisual.class, "ERR_JavaTargetChooser_UnwritablePackage")); //NOI18N
//            return false;
//        }
        String contextFileLocation = contextFileTextField.getText().trim();
        File contextFile = new File(contextFileLocation);
        if (contextFileLocation.length() == 0) {
            wizard.putProperty("WizardPanel_errorMessage", getMessage(GenerateFilePanelVisual.class, "EMPTY_FILE"));
        } else if (!contextFile.exists()) {
            wizard.putProperty("WizardPanel_errorMessage", getMessage(GenerateFilePanelVisual.class, "INVALID_FILE_NOT_FOUND"));
        } else if (!contextFile.isFile()) {
            wizard.putProperty("WizardPanel_errorMessage", getMessage(GenerateFilePanelVisual.class, "INVALID_FILE_NOT_FILE"));
        }

        wizard.putProperty("WizardPanel_errorMessage", ""); // NOI18N
        return true;
    }

    public SourceGroup getLocationValue() {
        return (SourceGroup) locationComboBox.getSelectedItem();
    }

    public String getFileName() {
        return fileNameTextField.getText();
    }

    public String getPackage() {
        return packageComboBoxEditor.getText();
    }

    private void locationChanged() {
        updateSourceGroupPackages();
//        changeSupport.fireChange();
    }

    void read(WizardDescriptor settings) {
        FileObject targetFolder = Templates.getTargetFolder(settings);
        projectTextField.setText(ProjectUtils.getInformation(project).getDisplayName());
        SourceGroup[] sourceGroups = getJavaSourceGroups(project);
        SourceGroupUISupport.connect(locationComboBox, sourceGroups);
        packageComboBox.setRenderer(PackageView.listRenderer());
        updateSourceGroupPackages();

        // set default source group and package cf. targetFolder
        if (targetFolder != null) {
            SourceGroup targetSourceGroup = getFolderSourceGroup(sourceGroups, targetFolder);
            if (targetSourceGroup != null) {
                locationComboBox.setSelectedItem(targetSourceGroup);
                String targetPackage = getPackageForFolder(targetSourceGroup, targetFolder);
                if (targetPackage != null) {
                    packageComboBoxEditor.setText(targetPackage);
                }
            }
        }
    }

    void store(WizardDescriptor settings) {
        String pkg = getPackage();
        try {
            FileObject fo = null;
            if (getLocationValue() != null) {
                fo = getLocationValue().getRootFolder();
            }
            if (fo == null) {
                FileObject targetFolder = Templates.getTargetFolder(settings);
                if (targetFolder != null) {
                    SourceGroup targetSourceGroup = getFolderSourceGroup(targetFolder);
                    fo = targetSourceGroup.getRootFolder();
                }
            }
            String pkgSlashes = pkg.replace('.', '/');
            FileObject targetFolder = fo.getFileObject(pkgSlashes);
            if (targetFolder == null) {
                targetFolder = FileUtil.createFolder(fo, pkgSlashes);
            }
            Templates.setTargetFolder(settings, targetFolder);
            Templates.setTargetName(wizard, this.getFileName());

            wizard.putProperty("CONTEXT_FILE", contextFileTextField.getText().trim());
            wizard.putProperty("PROMPT", promptBox.getSelectedItem().toString());
            wizard.putProperty("CONTEXT", contextArea.getText().trim());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updateSourceGroupPackages() {
        SourceGroup sourceGroup = (SourceGroup) locationComboBox.getSelectedItem();
        ComboBoxModel model = PackageView.createListView(sourceGroup);
        if (model.getSelectedItem() != null && model.getSelectedItem().toString().startsWith("META-INF")
                && model.getSize() > 1) { // NOI18N
            model.setSelectedItem(model.getElementAt(1));
        }
        packageComboBox.setModel(model);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        changeSupport.fireChange();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changeSupport.fireChange();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        changeSupport.fireChange();
    }

    private ComboBoxModel<String> getPromptModel() {
        List<String> prompts = new ArrayList<>();
        prompts.add(""); // Add an empty item
        prompts.addAll(prefsManager.getPrompts().keySet());

        return new DefaultComboBoxModel<>(prompts.toArray(new String[0]));
    }
}

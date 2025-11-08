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

//import static io.github.jeddict.ai.file.DocSetupPanelVisual.JAXB_SUPPORT;
//import static io.github.jeddict.ai.file.DocSetupPanelVisual.JPA_SUPPORT;
//import static io.github.jeddict.ai.file.DocSetupPanelVisual.JSONB_SUPPORT;
//import static io.github.jeddict.ai.file.DocSetupPanelVisual.JSON_FILE;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static java.util.Objects.nonNull;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.aggregate.AggregateProgressFactory;
import org.netbeans.api.progress.aggregate.AggregateProgressHandle;
import org.netbeans.api.progress.aggregate.ProgressContributor;
import org.netbeans.api.project.Project;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.util.NbBundle;
import static org.openide.util.NbBundle.getMessage;
import org.openide.util.RequestProcessor;

@TemplateRegistration(
        folder = "Other",
        position = 10,
        displayName = "#DocWizardDescriptor_displayName",
        iconBase = "io/github/jeddict/ai/file/GEN_AI_ICON.png",
        description = "GEN_AI_DESC.html")
public final class GenerateFileWizardDescriptor extends BaseWizardDescriptor {

    private WizardDescriptor wizard;
    private Project project;

    private FileObject packageFileObject;
    private String fileName, context, contextFile, prompt;
    private static final PreferencesManager prefsManager = PreferencesManager.getInstance();

    public GenerateFileWizardDescriptor() {
    }

    public GenerateFileWizardDescriptor(
            Project project,
            String contextFile,
            boolean jpaSupport,
            boolean jsonbSupport,
            boolean jaxbSupport) {
        this.project = project;
        this.contextFile = contextFile;
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        index = 0;
        if (project == null) {
            project = Templates.getProject(wizard);
        }
        WizardDescriptor.Panel secondPanel = new GenerateFilePanel(project, wizard);
        String names[];

        panels = new ArrayList<>();
        panels.add(secondPanel);
        names = new String[]{
            getMessage(GenerateFileWizardDescriptor.class, "DocWizardDescriptor_displayName")
        };

        wizard.putProperty("DocWizardDescriptor_displayName", getMessage(GenerateFileWizardDescriptor.class, "DocWizardDescriptor_displayName"));
        mergeSteps(wizard, panels.toArray(new WizardDescriptor.Panel[0]), names);
    }

    @Override
    public Set<?> instantiate() throws IOException {
        if (project == null) {
            project = Templates.getProject(wizard);
        }
        packageFileObject = Templates.getTargetFolder(wizard);
        fileName = Templates.getTargetName(wizard);
        contextFile = (String) wizard.getProperty("CONTEXT_FILE");
        prompt = (String) wizard.getProperty("PROMPT");
        context = (String) wizard.getProperty("CONTEXT");
        instantiateProcess(null);
        return Collections.singleton(DataFolder.findFolder(packageFileObject));
    }

    @Override
    public String name() {
        return NbBundle.getMessage(GenerateFileWizardDescriptor.class, "DocWizardDescriptor_displayName");
    }

    public void instantiateProcess(final Runnable runnable) throws IOException {
        final String title = NbBundle.getMessage(GenerateFileWizardDescriptor.class, "TITLE_Progress_AI_Generation"); //NOI18N
        final ProgressContributor progressContributor = AggregateProgressFactory.createProgressContributor(title);
        final AggregateProgressHandle handle = AggregateProgressFactory.createHandle(title, new ProgressContributor[]{progressContributor}, null, null);
        final Runnable r = () -> {
            try {
                handle.start();
                int progressStepCount = getProgressStepCount(10);
                progressContributor.start(progressStepCount);
                FileObject fileObject = packageFileObject.getFileObject(fileName);
                if (fileObject == null) {
                    fileObject = packageFileObject.createData(fileName);
                }
                StringBuilder inputBuilder = new StringBuilder();
                inputBuilder.append("Generate only the content of ").append(fileName).append(" without any explanation.\n");
                if (prompt != null && !prompt.isEmpty()) {
                    inputBuilder.append(prefsManager.getPrompts().get(prompt)).append("\n");
                }
                if (context != null && !context.isEmpty()) {
                    inputBuilder.append(context).append("\n");
                }
                if (contextFile != null && !contextFile.isEmpty()) {
                    FileObject contextFileObject = FileUtil.toFileObject(new File(contextFile));
                    if (contextFileObject == null || !contextFileObject.isValid()) {
                        throw new IOException("Invalid FileObject");
                    }
                    try (InputStream is = contextFileObject.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        inputBuilder.append(reader.lines().collect(Collectors.joining("\n"))).append("\n");
                    }
                }

                String responseText = new JeddictBrain(prefsManager.getModelName(), false, List.of()).generate(project, inputBuilder.toString());
                try (OutputStream os = fileObject.getOutputStream()) {
                    os.write(removeCodeBlockMarkers(responseText).getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                progressContributor.progress(progressStepCount);
            } catch (Exception ioe) {
                Logger.getLogger(GenerateFileWizardDescriptor.class.getName()).log(Level.INFO, null, ioe);
                NotifyDescriptor nd = new NotifyDescriptor.Message(ioe.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
            } finally {
                progressContributor.finish();
                handle.finish();
            }
        };

        SwingUtilities.invokeLater(new Runnable() {
            private boolean first = true;

            @Override
            public void run() {
                if (!first) {
                    RequestProcessor.getDefault().post(r);
//                    progressPanel.open(progressComponent, title);
                    if (nonNull(runnable)) {
                        runnable.run();
                    }
                } else {
                    first = false;
                    SwingUtilities.invokeLater(this);
                }
            }
        });
    }

    public static int getProgressStepCount(int baseCount) {
        return baseCount + 2;
    }

}

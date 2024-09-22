/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.scanner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 *
 * @author Gaurav Gupta
 */
public class ProjectClassListener {

    private Project project;

    private Map<FileObject, ClassData> classDatas;
    private Set<FileObject> pending = new HashSet<>();

    public ProjectClassListener(Project project, Map<FileObject, ClassData> classDatas) {
        this.project = project;
        this.classDatas = classDatas;
    }

    public Set<FileObject> getPending() {
        return pending;
    }

    public void register() {
        // Get source groups from the project (Java source folders)
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(Sources.TYPE_GENERIC);

        FileObject javaFolder = null;
        for (SourceGroup group : sourceGroups) {
            // Get the root folder of the source group
            FileObject rootFolder = group.getRootFolder();

            // Check if the root folder has a 'src/main/java' folder
            FileObject srcFolder = rootFolder.getFileObject("src");
            if (srcFolder != null) {
                FileObject mainFolder = srcFolder.getFileObject("main");
                if (mainFolder != null) {
                    javaFolder = mainFolder.getFileObject("java");
                }
            }
        }

        FileObject javaFolder2 = javaFolder;
        // Listen for changes in the DataObject Registry
        DataObject.Registry registry = DataObject.getRegistry();
        registry.addChangeListener((e) -> {
            DataObject[] modifiedObjects = registry.getModified();
            for (DataObject dataObj : modifiedObjects) {
                if (FileUtil.isParentOf(javaFolder2, dataObj.getPrimaryFile())) {
                    pending.add(dataObj.getPrimaryFile());
                    classDatas.remove(dataObj.getPrimaryFile());
                    System.out.println("Modified DataObject: " + dataObj.getName());
                } else {
                    System.out.println("");
                }
            }
        });
    }

}

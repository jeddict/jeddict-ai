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
package io.github.jeddict.ai.scanner;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.settings.AIClassContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public class ProjectClassScanner {

    public static Map<FileObject, ClassData> scanProjectClasses(Project project) throws IOException {
        Map<FileObject, ClassData> classList = new HashMap<>();

        if (project != null) {
            // Get source groups from the project (Java source folders)
            Sources sources = ProjectUtils.getSources(project);
            SourceGroup[] sourceGroups = sources.getSourceGroups(Sources.TYPE_GENERIC);

            for (SourceGroup group : sourceGroups) {
                // Get the root folder of the source group
                FileObject rootFolder = group.getRootFolder();

                // Check if the root folder has a 'src/main/java' folder
                FileObject srcFolder = rootFolder.getFileObject("src");
                if (srcFolder != null) {
                    FileObject mainFolder = srcFolder.getFileObject("main");
                    if (mainFolder != null) {
                        FileObject javaFolder = mainFolder.getFileObject("java");
                        if (javaFolder != null) {
                            scanFolder(javaFolder, classList); // Scan the 'src/main/java' folder
                        }
                    }
                }
            }
        }

        return classList;
    }

    // Recursively scan folders for .java files
    private static void scanFolder(FileObject folder, Map<FileObject, ClassData> classList) throws IOException {
        for (FileObject file : folder.getChildren()) {
            if (file.isFolder()) {
                scanFolder(file, classList);
            } else if (file.getExt().equals("java")) {
                scanJavaFile(file, classList);
            }
        }
    }

    public static void scanJavaFile(DataObject javaFile, Map<FileObject, ClassData> classList) throws IOException {
        scanJavaFile(javaFile.getPrimaryFile(), classList);
    }

    public static void scanJavaFile(FileObject javaFile, Map<FileObject, ClassData> classList) throws IOException {
        JavaSource javaSource = JavaSource.forFileObject(javaFile);

        if (javaSource != null) {
            javaSource.runUserActionTask((CompilationController cc) -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                // Iterate through class declarations in the file
                List<? extends Tree> typeDecls = cc.getCompilationUnit().getTypeDecls();
                for (Tree typeDecl : typeDecls) {
                    if (typeDecl.getKind() == Tree.Kind.CLASS) {
                        ClassTree classTree = (ClassTree) typeDecl;
//                            String classSource = cc.getText().substring(classTree.getStartPosition(), classTree.getEndPosition() + 1);
// Store or use the classWithoutMethods as needed
                        TypeElement classElement = (TypeElement) cc.getTrees()
                                .getElement(cc.getTrees().getPath(cc.getCompilationUnit(), classTree));

// Get the package name
                        Element packageElement = classElement.getEnclosingElement();
                        String packageName = ((PackageElement) packageElement).getQualifiedName().toString();

                        String classWithoutMethodsBody = removeMethodBodies(cc, classTree, packageName);
                        ClassData classData1 = new ClassData(packageName, classElement.getSimpleName().toString(), classWithoutMethodsBody);
                        classList.put(javaFile, classData1);
                        List<Map<String, String>> attributes = new ArrayList<>();
                        for (Element element : classElement.getEnclosedElements()) {
                            if (element.getKind() == ElementKind.FIELD) {
                                classData1.addSubTree(element.asType().toString());
                            } else if (element.getKind() == ElementKind.METHOD) {
                                String type = element.asType().toString();
                                if (type.startsWith("()")) {
                                    type = type.substring(2);
                                }
                                classData1.addSubTree(type);
                            }
                        }

                    }
                }
            }, true);
        }
    }

    private static String removeMethodBodies(CompilationController cc, ClassTree classTree, String packageName) {
        StringBuilder sb = new StringBuilder();

        // Add package declaration
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // Add annotations
        for (Tree annotation : classTree.getModifiers().getAnnotations()) {
            int start = (int) cc.getTrees().getSourcePositions().getStartPosition(cc.getCompilationUnit(), annotation);
            int end = (int) cc.getTrees().getSourcePositions().getEndPosition(cc.getCompilationUnit(), annotation);
            sb.append(cc.getText().substring(start, end + 1)).append("\n");
        }

        // Add class declaration
        sb.append("public class ").append(classTree.getSimpleName());
        // Add extends clause if there is a superclass
        Tree superclass = classTree.getExtendsClause();
        if (superclass != null) {
            int start = (int) cc.getTrees().getSourcePositions().getStartPosition(cc.getCompilationUnit(), superclass);
            int end = (int) cc.getTrees().getSourcePositions().getEndPosition(cc.getCompilationUnit(), superclass);
            sb.append(" extends ").append(cc.getText().substring(start, end + 1));
        }

        // Add implements clause if there are interfaces
        List<? extends Tree> interfaces = classTree.getImplementsClause();
        if (!interfaces.isEmpty()) {
            sb.append(" implements ");
            for (int i = 0; i < interfaces.size(); i++) {
                Tree iface = interfaces.get(i);
                int start = (int) cc.getTrees().getSourcePositions().getStartPosition(cc.getCompilationUnit(), iface);
                int end = (int) cc.getTrees().getSourcePositions().getEndPosition(cc.getCompilationUnit(), iface);
                sb.append(cc.getText().substring(start, end + 1));
                if (i < interfaces.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        sb.append(" {\n"); // Opening brace for the class

        // Add member variables and methods
        for (Tree member : classTree.getMembers()) {
            if (member instanceof MethodTree) {
                MethodTree method = (MethodTree) member;
                if (method.getName().toString().equals("<init>")) {
                    continue;
                }
                ModifiersTree modifiersTree = ((ModifiersTree) method.getModifiers());
                if (!modifiersTree.getFlags().contains(Modifier.PRIVATE)) {
                    // Use the method itself to get the start position
                    int start = (int) cc.getTrees().getSourcePositions().getStartPosition(cc.getCompilationUnit(), method);
                    int end = method.getBody() != null
                            ? (int) cc.getTrees().getSourcePositions().getStartPosition(cc.getCompilationUnit(), method.getBody())
                            : (int) cc.getTrees().getSourcePositions().getEndPosition(cc.getCompilationUnit(), method);

                    // Append method declaration without the body
                    sb.append(cc.getText().substring(start, end)).append(";\n"); // Placeholder for method body
                }
            } else if (member instanceof VariableTree) {
                VariableTree var = (VariableTree) member;
                ModifiersTree modifiersTree = ((ModifiersTree) var.getModifiers());
                if (!modifiersTree.getFlags().contains(Modifier.PRIVATE)) {
                    sb.append(var.toString()).append("\n");
                }

            }
        }

        // Closing brace for the class
        sb.append("}\n");

        return sb.toString()
                .replace(") ;", ");")
                .replace("    ", "")
                .replace("\n\n", "\n");
    }

    private static final Map<String, Map<FileObject, ClassData>> classData = new HashMap<>(); // project is key
    private static final Map<String, ProjectClassListener> projectClassListeners = new HashMap<>(); // project is key
    private static final Map<String, JeddictBrain> models = new HashMap<>(); // class file is key

    public static void clear() {
        classData.clear();
        projectClassListeners.clear();
        models.clear();
    }

    public static JeddictBrain getJeddictChatModel(FileObject fileObject) {
//        String key = fileObject.toURL().toString();
//        if (models.get(key) == null) {
//            models.put(key, new JeddictChatModel());
//        }
//        return models.get(key);
        return new JeddictBrain();
    }

    public static FileObject getFileObjectFromEditor(Document document) {
        if (document == null) {
            JTextComponent editor = EditorRegistry.lastFocusedComponent();
            if (editor != null) {
                document = editor.getDocument();
            }
        }
        if (document != null) {
            DataObject dataObject = (DataObject) document.getProperty(Document.StreamDescriptionProperty);
            if (dataObject != null) {
                return dataObject.getPrimaryFile();
            }
        }
        return null;
    }

    public static List<ClassData> getClassData(FileObject fileObject, Set<String> findReferencedClasses, AIClassContext classAnalysisContext) {

        if (classAnalysisContext == AIClassContext.CURRENT_CLASS
                || fileObject == null) {
            return Collections.emptyList();
        }
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project != null) {
            try {
                String key = project.getProjectDirectory().toString();
                if (classData.get(key) == null) {
                    Map<FileObject, ClassData> classDataList = scanProjectClasses(project);
                    classData.put(key, classDataList);
                    ProjectClassListener projectClassListener = new ProjectClassListener(project, classDataList);
                    projectClassListener.register();
                    projectClassListeners.put(key, projectClassListener);
                }
                if (projectClassListeners.get(key) != null) {
                    Iterator<DataObject> iterator = projectClassListeners.get(key).getPendingDataObject().iterator();
                    while (iterator.hasNext()) {
                        DataObject javaFile = iterator.next();
                        if (javaFile.getPrimaryFile().equals(fileObject)) {
                            // Ignore current editor
                            System.out.println("Ignoring " + fileObject.getName());
                        } else {
                            // Remove safely using the iterator
                            iterator.remove();
                            System.out.println("Rescanning " + javaFile.getName());
                            scanJavaFile(javaFile, classData.get(key));
                        }
                    }
                }

                if (classAnalysisContext == AIClassContext.REFERENCED_CLASSES) {
                    return classData.get(key).entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(fileObject))
                            .filter(entry -> findReferencedClasses != null && findReferencedClasses.contains(entry.getKey().getName()))
                            .map(entry -> entry.getValue())
                            .collect(toList());
                } else if (classAnalysisContext == AIClassContext.CURRENT_PACKAGE) {
                    return classData.get(key).entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(fileObject))
                            .filter(entry
                                    -> (findReferencedClasses != null && findReferencedClasses.contains(entry.getKey().getName()))
                            || (entry.getKey().getParent().equals(fileObject.getParent()))
                            )
                            .map(entry -> entry.getValue())
                            .collect(toList());
                } else if (classAnalysisContext == AIClassContext.ENTIRE_PROJECT) {
                    return classData.get(key).entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(fileObject))
                            .map(entry -> entry.getValue())
                            .collect(toList());
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return Collections.emptyList();
    }

    public static Set<String> getReferencedClasses(CompilationUnitTree compilationUnit) throws IOException {
        return findReferencedClasses(compilationUnit);
    }

    private static Set<String> findReferencedClasses(CompilationUnitTree compilationUnit) {
        Set<String> referencedClasses = new HashSet<>();

        for (Tree tree : compilationUnit.getTypeDecls()) {
            if (tree instanceof ClassTree) {
                ClassTree classTree = (ClassTree) tree;
                Tree superclass = classTree.getExtendsClause();
                if (superclass != null) {
                    referencedClasses.add(superclass.toString());
                }
                for (Tree member : classTree.getMembers()) {
                    if (member instanceof VariableTree) {
                        VariableTree variable = (VariableTree) member;
                        referencedClasses.add(variable.getType().toString());
                    } else if (member instanceof MethodTree) {
                        MethodTree method = (MethodTree) member;
                        if (method.getReturnType() != null) {
                            referencedClasses.add(method.getReturnType().toString());
                        }
                    }
                }
            }
        }

        return referencedClasses;
    }

    public static String getClassDataContent(FileObject fileObject, CompilationUnitTree compilationUnit, AIClassContext activeClassContext) {
        Set<String> findReferencedClasses = findReferencedClasses(compilationUnit);
        List<ClassData> classDatas = getClassData(fileObject, findReferencedClasses, activeClassContext);
        return classDatas.stream()
                .map(cd -> cd.toString())
                .collect(Collectors.joining("\n------------\n"));
    }
}

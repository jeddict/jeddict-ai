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

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.settings.AIClassContext;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
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
                        // Store or use the classWithoutMethods as needed
                        TypeElement classElement = (TypeElement) cc.getTrees()
                                .getElement(cc.getTrees().getPath(cc.getCompilationUnit(), classTree));

                        // Get the package name
                        Element packageElement = classElement.getEnclosingElement();
                        String packageName = ((PackageElement) packageElement).getQualifiedName().toString();

                        String classWithoutMethodsBody = removeMethodBodies(cc, classTree, packageName);
                        ClassData classData1 = new ClassData(packageName, classElement.getSimpleName().toString(), classWithoutMethodsBody);
                        classList.put(javaFile, classData1);
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

    private static String removeMethodBodies(CompilationController cc,
            ClassTree classTree,
            String packageName) {

        String source = cc.getText();
        StringBuilder sb = new StringBuilder();

        // ---------- helpers ----------
        final java.util.function.BiPredicate<Long, Long> validPos
                = (start, end) -> start >= 0 && end >= 0 && end >= start;

        // ---------- package ----------
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // ---------- annotations ----------
        for (Tree annotation : classTree.getModifiers().getAnnotations()) {
            long start = cc.getTrees().getSourcePositions()
                    .getStartPosition(cc.getCompilationUnit(), annotation);
            long end = cc.getTrees().getSourcePositions()
                    .getEndPosition(cc.getCompilationUnit(), annotation);

            if (validPos.test(start, end)) {
                sb.append(source.substring((int) start, (int) end + 1)).append("\n");
            }
        }

        // ---------- class declaration ----------
        sb.append("public class ").append(classTree.getSimpleName());

        // extends
        Tree superclass = classTree.getExtendsClause();
        if (superclass != null) {
            long start = cc.getTrees().getSourcePositions()
                    .getStartPosition(cc.getCompilationUnit(), superclass);
            long end = cc.getTrees().getSourcePositions()
                    .getEndPosition(cc.getCompilationUnit(), superclass);

            if (validPos.test(start, end)) {
                sb.append(" extends ")
                        .append(source.substring((int) start, (int) end + 1));
            }
        }

        // implements
        List<? extends Tree> interfaces = classTree.getImplementsClause();
        if (!interfaces.isEmpty()) {
            sb.append(" implements ");
            boolean first = true;
            for (Tree iface : interfaces) {
                long start = cc.getTrees().getSourcePositions()
                        .getStartPosition(cc.getCompilationUnit(), iface);
                long end = cc.getTrees().getSourcePositions()
                        .getEndPosition(cc.getCompilationUnit(), iface);

                if (validPos.test(start, end)) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(source.substring((int) start, (int) end + 1));
                    first = false;
                }
            }
        }

        sb.append(" {\n");

        // ---------- members ----------
        for (Tree member : classTree.getMembers()) {

            // ----- methods -----
            if (member instanceof MethodTree method) {

                // skip private methods/constructors
                if (method.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
                    continue;
                }

                boolean isConstructor = method.getName().contentEquals("<init>");

                long start = cc.getTrees().getSourcePositions()
                        .getStartPosition(cc.getCompilationUnit(), method);

                long end;
                if (method.getBody() != null) {
                    end = cc.getTrees().getSourcePositions()
                            .getStartPosition(cc.getCompilationUnit(), method.getBody());
                } else {
                    end = cc.getTrees().getSourcePositions()
                            .getEndPosition(cc.getCompilationUnit(), method);
                }

                if (validPos.test(start, end)) {
                    sb.append(source.substring((int) start, (int) end))
                            .append(";\n");
                } else {
                    // ---------- AST fallback ----------
                    sb.append(method.getModifiers()).append(" ");

                    if (!isConstructor && method.getReturnType() != null) {
                        sb.append(method.getReturnType()).append(" ");
                    }

                    // constructor name = class name
                    sb.append(isConstructor
                            ? classTree.getSimpleName()
                            : method.getName());

                    sb.append(method.getParameters());

                    if (!method.getThrows().isEmpty()) {
                        sb.append(" throws ");
                        for (int i = 0; i < method.getThrows().size(); i++) {
                            if (i > 0) {
                                sb.append(", ");
                            }
                            sb.append(method.getThrows().get(i));
                        }
                    }

                    sb.append(";\n");
                }
            } // ----- fields -----
            else if (member instanceof VariableTree var) {
                if (!var.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
                    sb.append(var).append("\n");
                }
            }
        }

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

    public static List<ClassData> getClassData(
            final FileObject fileObject, final Set<String> findReferencedClasses, final AIClassContext classAnalysisContext
    ) {
        final Logger LOG = Logger.getLogger(ProjectClassScanner.class.getCanonicalName());

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
                            LOG.finest(() -> "Ignoring " + fileObject.getName());
                        } else {
                            // Remove safely using the iterator
                            iterator.remove();
                            LOG.finest(() -> "Rescanning " + javaFile.getName());
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
            if (!(tree instanceof ClassTree classTree)) {
                continue;
            }

            // extends clause
            collectTypes(classTree.getExtendsClause(), referencedClasses);

            // implements clause
            for (Tree impl : classTree.getImplementsClause()) {
                collectTypes(impl, referencedClasses);
            }

            for (Tree member : classTree.getMembers()) {

                // fields
                if (member instanceof VariableTree variable) {
                    collectTypes(variable.getType(), referencedClasses);
                } // methods
                else if (member instanceof MethodTree method) {

                    // return type
                    collectTypes(method.getReturnType(), referencedClasses);

                    // parameters
                    for (VariableTree param : method.getParameters()) {
                        collectTypes(param.getType(), referencedClasses);
                    }

                    // thrown exceptions
                    for (ExpressionTree thr : method.getThrows()) {
                        collectTypes(thr, referencedClasses);
                    }
                }
            }
        }

        return referencedClasses;
    }

    private static void collectTypes(Tree type, Set<String> referencedClasses) {
        if (type == null) {
            return;
        }

        // Handle wildcards safely
        if (type instanceof WildcardTree wt) {
            collectTypes(wt.getBound(), referencedClasses);
            return;
        }

        switch (type.getKind()) {

            case PARAMETERIZED_TYPE -> {
                ParameterizedTypeTree ptt = (ParameterizedTypeTree) type;
                collectTypes(ptt.getType(), referencedClasses);
                for (Tree arg : ptt.getTypeArguments()) {
                    collectTypes(arg, referencedClasses);
                }
            }

            case IDENTIFIER -> {
                IdentifierTree it = (IdentifierTree) type;
                referencedClasses.add(it.getName().toString());
            }

            case MEMBER_SELECT -> {
                MemberSelectTree mst = (MemberSelectTree) type;
                referencedClasses.add(mst.getIdentifier().toString());
            }

            case ARRAY_TYPE -> {
                ArrayTypeTree att = (ArrayTypeTree) type;
                collectTypes(att.getType(), referencedClasses);
            }

            default -> {
                // primitives, void, literals → ignore
            }
        }
    }

    public static String getClassDataContent(FileObject fileObject, CompilationUnitTree compilationUnit, AIClassContext activeClassContext) {
        Set<String> findReferencedClasses = findReferencedClasses(compilationUnit);
        List<ClassData> classDatas = getClassData(fileObject, findReferencedClasses, activeClassContext);
        return classDatas.stream()
                .map(cd -> cd.toString())
                .collect(Collectors.joining("\n------------\n"));
    }
}

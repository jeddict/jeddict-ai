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
package io.github.jeddict.ai.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import io.github.jeddict.ai.scanner.MyTreePathScanner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.json.JSONArray;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Exceptions;

/**
 *
 * @author Shiwani Gupta
 */
public class SourceUtil {

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public static void printSource(FileObject fileObject) {
        if (fileObject != null && "java".equals(fileObject.getExt())) {
            try (InputStream is = fileObject.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
        }
    }

    public static void addImports(WorkingCopy copy, JSONArray imports) {
        CompilationUnitTree compilationUnit = copy.getCompilationUnit();
        TreeMaker make = copy.getTreeMaker();
        CompilationUnitTree newcompilationUnit = compilationUnit;
        for (int i = 0; i < imports.length(); i++) {
            String importToAdd = imports.getString(i);
            if (importToAdd.startsWith("import ") && importToAdd.endsWith(";")) {
                importToAdd = importToAdd.substring(7, importToAdd.length() - 1).trim();  // Extract the class name
            }
            if (!isImportPresent(copy, compilationUnit, importToAdd)) {
                ImportTree newImport = make.Import(make.QualIdent(importToAdd), false);
                newcompilationUnit = make.addCompUnitImport(newcompilationUnit, newImport);
            }
        }
        copy.rewrite(compilationUnit, newcompilationUnit);
    }

    public static void addImports(WorkingCopy copy, List<String> imports) {
        CompilationUnitTree compilationUnit = copy.getCompilationUnit();
        TreeMaker make = copy.getTreeMaker();
        CompilationUnitTree newcompilationUnit = compilationUnit;
        for (int i = 0; i < imports.size(); i++) {
            String importToAdd = imports.get(i);
            if (importToAdd.startsWith("import ") && importToAdd.endsWith(";")) {
                importToAdd = importToAdd.substring(7, importToAdd.length() - 1).trim();  // Extract the class name
            }
            if (!isImportPresent(copy, compilationUnit, importToAdd)) {
                ImportTree newImport = make.Import(make.QualIdent(importToAdd), false);
                newcompilationUnit = make.addCompUnitImport(newcompilationUnit, newImport);
            }
        }
        copy.rewrite(compilationUnit, newcompilationUnit);
    }

    private static boolean isImportPresent(WorkingCopy copy, CompilationUnitTree compilationUnit, String importName) {
        for (ImportTree importTree : compilationUnit.getImports()) {
            if (importTree.getQualifiedIdentifier().toString().equals(importName)) {
                return true; // Import already exists
            }
        }
        return false;
    }

    public static void reformatClass(WorkingCopy copy, ClassTree classTree) throws Exception {
        // NetBeans does formatting on save or during 'fix imports', but you can also trigger formatting like this:
        copy.toPhase(JavaSource.Phase.UP_TO_DATE); // Move to the UP_TO_DATE phase

        // Create a rewriter for formatting
        copy.rewrite(classTree, classTree);  // This rewrites the class and lets NetBeans apply default formatting

        // Alternatively, save changes and let the IDE apply auto-format on save if configured
    }

    public static String geIndentaion(WorkingCopy copy, Tree leaf) {
        try {
            if (copy.getDocument() != null) {
                String sourceCode = copy.getDocument().getText(0, copy.getDocument().getEndPosition().getOffset());
                int startPos = (int) copy.getTrees().getSourcePositions().getStartPosition(copy.getCompilationUnit(), leaf);
                String[] lines = sourceCode.substring(0, startPos).split("\n"); // Zero-based index
                String lastLine = lines[lines.length - 1];
                return lastLine;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return "";
    }

    public static String removeJavadoc(String content) {
        return content.replaceAll("/\\*{1,2}[\\s\\S]*?\\*/|//.*|^\\s*///.*$", "");
    }

    public static void updateMethodInSource(FileObject fileObject, String sourceMethodSignature, String methodContent) {
        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        try {
            javaSource.runModificationTask(copy -> {
                copy.toPhase(JavaSource.Phase.RESOLVED);
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree methodTree, Void v) {
                        Name name = methodTree.getName();
                        String targetMethodSignature = name.toString() + "("
                                + methodTree.getParameters().stream()
                                        .map(param -> param.getType().toString())
                                        .collect(Collectors.joining(",")) + ")";

                        // Compare the signature with the method being updated
                        if (targetMethodSignature.equals(sourceMethodSignature)) {
                            long startPos = copy.getTrees().getSourcePositions().getStartPosition(copy.getCompilationUnit(), methodTree);
                            long endPos = copy.getTrees().getSourcePositions().getEndPosition(copy.getCompilationUnit(), methodTree);

                            try {
                                if (copy.getDocument() == null) {
                                    openFileInEditor(fileObject);
                                }
                                insertAndReformat(copy.getDocument(), methodContent, (int) startPos, (int) endPos - (int) startPos);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                        return super.visitMethod(methodTree, v);
                    }
                }.scan(copy.getCompilationUnit(), null);

            }).commit();
        } catch (IOException e) {
            System.out.println("Error updating method " + sourceMethodSignature + " in file " + fileObject.getName() + ": " + e.getMessage());
        }
    }

    public static void updateFullSourceInFile(FileObject fileObject, String newSourceContent) {
        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        try {
            javaSource.runModificationTask(copy -> {
                copy.toPhase(JavaSource.Phase.RESOLVED);
                Document document = copy.getDocument();

                try {
                    // Open the file in the editor if it is not already open
                    if (document == null) {
                        openFileInEditor(fileObject);
                        document = copy.getDocument(); // Re-fetch the document after opening
                    }

                    // Replace the entire document content with the new source content
                    document.remove(0, document.getLength());
                    document.insertString(0, newSourceContent, null);

                } catch (BadLocationException | IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }).commit();
        } catch (IOException e) {
            System.out.println("Error updating source in file " + fileObject.getName() + ": " + e.getMessage());
        }
    }

    public static String findMethodSourceInFileObject(FileObject fileObject, String sourceMethodSignature) {
        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        StringBuilder methodSource = new StringBuilder();

        try {
            javaSource.runModificationTask(copy -> {
                copy.toPhase(JavaSource.Phase.RESOLVED);
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree methodTree, Void v) {
                        Name name = methodTree.getName();
                        String targetMethodSignature = name.toString() + "("
                                + methodTree.getParameters().stream()
                                        .map(param -> param.getType().toString())
                                        .collect(Collectors.joining(",")) + ")";

                        // Check if the signatures match
                        if (targetMethodSignature.equals(sourceMethodSignature)) {
                            // Construct the method source code
                            methodSource.append(methodTree.toString());
                        }
                        return super.visitMethod(methodTree, v);
                    }
                }.scan(copy.getCompilationUnit(), null);
            }).commit();
        } catch (IOException e) {
            System.out.println("Error finding method " + sourceMethodSignature + " in file " + fileObject.getName() + ": " + e.getMessage());
        }

        if (methodSource.isEmpty()) {
            try {
                javaSource.runModificationTask(copy -> {
                    copy.toPhase(JavaSource.Phase.RESOLVED);
                    new TreePathScanner<Void, Void>() {
                        @Override
                        public Void visitMethod(MethodTree methodTree, Void v) {
                            Name name = methodTree.getName();
                            String targetMethodSignature = name.toString();

                            // Check if the signatures match
                            if (targetMethodSignature.equals(sourceMethodSignature)) {
                                // Construct the method source code
                                methodSource.append(methodTree.toString());
                            }
                            return super.visitMethod(methodTree, v);
                        }
                    }.scan(copy.getCompilationUnit(), null);
                }).commit();
            } catch (IOException e) {
                System.out.println("Error finding method " + sourceMethodSignature + " in file " + fileObject.getName() + ": " + e.getMessage());
            }
        }
        return methodSource.toString(); // Return the method source code
    }

    public static void openFileInEditor(FileObject fileObject) {
        try {
            // Get the DataObject associated with the FileObject
            DataObject dataObject = DataObject.find(fileObject);

            // Lookup for the EditorCookie from the DataObject
            EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);

            if (editorCookie != null) {
                // Open the file in the editor
                editorCookie.open();
                StatusDisplayer.getDefault().setStatusText("File opened in editor: " + fileObject.getNameExt());
            } else {
                StatusDisplayer.getDefault().setStatusText("Failed to find EditorCookie for file: " + fileObject.getNameExt());
            }
        } catch (DataObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

public static void openFileInEditorAtLine(FileObject fileObject, int lineNumber) {
    try {
        // Find DataObject for the file
        DataObject dataObject = DataObject.find(fileObject);

        // Lookup for EditorCookie
        EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);

        if (editorCookie != null) {
            // Open the file in editor
            editorCookie.open();

            // Lookup for LineCookie to access lines of the file
            LineCookie lineCookie = dataObject.getLookup().lookup(LineCookie.class);
            if (lineCookie != null) {
                Line.Set lineSet = lineCookie.getLineSet();
                if (lineNumber >= 1 && lineNumber <= lineSet.getLines().size()) {
                    Line line = lineSet.getCurrent(lineNumber - 1); // lines are zero-based internally

                    // Show and focus on the line
                    line.show(Line.ShowOpenType.REUSE, Line.ShowVisibilityType.FOCUS);
                } else {
                    // Invalid line number, just show status
                    StatusDisplayer.getDefault().setStatusText("Invalid line number: " + lineNumber);
                }
            } else {
                StatusDisplayer.getDefault().setStatusText("Failed to find LineCookie for file: " + fileObject.getNameExt());
            }
        } else {
            StatusDisplayer.getDefault().setStatusText("Failed to find EditorCookie for file: " + fileObject.getNameExt());
        }
    } catch (DataObjectNotFoundException e) {
        Exceptions.printStackTrace(e);
    }
}

    private static void insertAndReformat(Document document, String content, int startPosition, int lengthToRemove) {
        try {
            if (lengthToRemove > 0) {
                document.remove(startPosition, lengthToRemove);
            }
            document.insertString(startPosition, content, null);
            Reformat reformat = Reformat.get(document);
            reformat.lock();
            try {
                reformat.reformat(startPosition, startPosition + content.length());
            } finally {
                reformat.unlock();
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void updateMethod(FileObject javaFile, String methodSignature, String newMethodText) {
        try {
            JavaSource javaSource = JavaSource.forFileObject(javaFile);
            javaSource.runModificationTask(copy -> {
                copy.toPhase(JavaSource.Phase.RESOLVED);

                CompilationUnitTree cut = copy.getCompilationUnit();
                TreeMaker maker = copy.getTreeMaker();
                ClassTree classTree = (ClassTree) cut.getTypeDecls().get(0); // First class

                MethodTree newMethodTree = parseMethodTree(copy, methodSignature, newMethodText);
                if (newMethodTree == null) {
                    return;
                }

                for (Tree member : classTree.getMembers()) {
                    if (member instanceof MethodTree method) {
                        String sig = method.getName() + method.getParameters().stream()
                                .map(p -> p.getType().toString())
                                .collect(java.util.stream.Collectors.joining(",", "(", ")"));
                        if (sig.equals(methodSignature)) {
                            copy.rewrite(method, newMethodTree);
                            return;
                        }
                    }
                }
            }).commit();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static MethodTree parseMethodTree(WorkingCopy copy, String expectedSignature, String methodText) throws IOException {
        String dummyClass = "public class Dummy { " + methodText + " }";
        FileObject tempFile = org.openide.filesystems.FileUtil.createMemoryFileSystem().getRoot().createData("Dummy.java");
        try (OutputStream os = tempFile.getOutputStream(); Writer writer = new OutputStreamWriter(os)) {
            writer.write(dummyClass);
        }

        JavaSource source = JavaSource.forFileObject(tempFile);
        final MethodTree[] result = new MethodTree[1];

        source.runUserActionTask(controller -> {
            controller.toPhase(JavaSource.Phase.RESOLVED);
            CompilationUnitTree cut = controller.getCompilationUnit();
            for (Tree typeDecl : cut.getTypeDecls()) {
                if (typeDecl instanceof ClassTree classTree) {
                    for (Tree member : classTree.getMembers()) {
                        if (member instanceof MethodTree method) {
                            String sig = method.getName() + method.getParameters().stream()
                                    .map(p -> p.getType().toString())
                                    .collect(java.util.stream.Collectors.joining(",", "(", ")"));

                            if (sig.equals(expectedSignature)) {
                                result[0] = method;
                                return;
                            }
                        }
                    }
                }
            }
        }, true);
        tempFile.delete();

        if (result[0] == null) {
            return null;
        }

        TreeMaker maker = copy.getTreeMaker();
        MethodTree mt = result[0];

        // Correcting annotations
        List<? extends AnnotationTree> correctedAnnotations = mt.getModifiers().getAnnotations().stream()
                .map(annotation -> {
                    if (annotation.getArguments() != null && !annotation.getArguments().isEmpty()) {
                        List<ExpressionTree> args = new ArrayList<>();
                        for (ExpressionTree argument : annotation.getArguments()) {
                            if (argument instanceof AssignmentTree assignment) {
                                if ("value".equals(assignment.getVariable().toString())) {
                                    ExpressionTree valueExpr = assignment.getExpression();
                                    ExpressionTree safeExpr = valueExpr;

                                    if (valueExpr instanceof LiteralTree literal) {
                                        Object val = literal.getValue();
                                        // Unquote if needed, though usually it's already clean
                                        if (val instanceof String strVal) {
                                            safeExpr = maker.Literal(strVal);
                                        }
                                    }

                                    args.add(safeExpr);
                                } else {
                                    args.add(argument);
                                }

                            } else {
                                args.add(argument);
                            }
                        }
                        return maker.Annotation(annotation.getAnnotationType(), args);
                    }
                    return annotation;
                }).collect(Collectors.toList());

        // Update the method's modifiers with the corrected annotations
        ModifiersTree updatedModifiers = maker.Modifiers(
                mt.getModifiers().getFlags(), correctedAnnotations);

        return maker.Method(
                updatedModifiers,
                mt.getName(),
                mt.getReturnType(),
                mt.getTypeParameters(),
                mt.getParameters(),
                mt.getThrows(),
                mt.getBody(),
                (ExpressionTree) mt.getDefaultValue()
        );
    }

    private String getMethodContentFromSource(FileObject fileObject, String sourceMethodSignature) {
        JavaSource javaSource = JavaSource.forFileObject(fileObject);
        final StringBuilder methodContent = new StringBuilder();

        try {
            javaSource.runUserActionTask(copy -> {
                copy.toPhase(JavaSource.Phase.RESOLVED);
                CompilationUnitTree cu = copy.getCompilationUnit();
                Trees trees = copy.getTrees();
                SourcePositions sourcePositions = trees.getSourcePositions();

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree methodTree, Void v) {
                        String currentSignature = methodTree.getName().toString() + "("
                                + methodTree.getParameters().stream()
                                        .map(param -> param.getType().toString())
                                        .collect(Collectors.joining(",")) + ")";

                        if (currentSignature.equals(sourceMethodSignature)) {
                            long start = sourcePositions.getStartPosition(cu, methodTree);
                            long end = sourcePositions.getEndPosition(cu, methodTree);

                            try {
                                String fullText = copy.getText();
                                methodContent.append(fullText.substring((int) start, (int) end));
                            } catch (Exception e) {
                                Exceptions.printStackTrace(e);
                            }
                        }
                        return super.visitMethod(methodTree, v);
                    }
                }.scan(cu, null);
            }, true);
        } catch (IOException e) {
            System.out.println("Error retrieving method " + sourceMethodSignature + " from file " + fileObject.getName() + ": " + e.getMessage());
        }

        return methodContent.toString();
    }

    public static FileObject findFileInProjects(String relativePath) {
        if (!relativePath.startsWith("src")) {
            return null; // Only proceed if path starts with "src"
        }

        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            FileObject projectDir = project.getProjectDirectory();
            FileObject fileToOpen = projectDir.getFileObject(relativePath);

            if (fileToOpen != null && fileToOpen.isData()) {
                return fileToOpen;
            }
        }
        return null;
    }

    /**
     * Searches all open projects' source classpaths for a class with the given
     * simple name or fully qualified.
     *
     * @param className simple class name to search for (without package)
     * @return full path
     */
    public static FileObject findClassInSourcePath(String className, boolean searchSourcePath) {
        boolean isFullyQualified = className.contains(".");
        String relativePath = null;

        if (isFullyQualified) {
            // Convert fully qualified class name to relative path
            // e.g. com.example.MyClass -> com/example/MyClass.java
            relativePath = className.replace('.', '/') + ".java";
        }

        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            for (var sourceGroup : ProjectUtils.getSources(project).getSourceGroups("java")) {
                ClassPath classPath = ClassPath.getClassPath(sourceGroup.getRootFolder(), searchSourcePath ? ClassPath.SOURCE : ClassPath.BOOT);
                if (classPath != null) {
                    for (ClassPath.Entry entry : classPath.entries()) {
                        FileObject root = entry.getRoot();
                        if (root != null) {
                            if (isFullyQualified) {
                                // Directly look up the relative path
                                FileObject file = root.getFileObject(relativePath);
                                if (file != null && "java".equals(file.getExt()) && file.getName().equals(getSimpleName(className))) {
                                    return file;
                                }
                            } else {
                                // Fallback to recursive search by simple class name
                                FileObject found = findClassFile(root, className, "");
                                if (found != null) {
                                    return found;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String getSimpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    private static FileObject findClassFile(FileObject file, String className, String relativePath) {
        if (file.isFolder()) {
            for (FileObject child : file.getChildren()) {
                FileObject found = findClassFile(child, className, relativePath.isEmpty() ? child.getName() : relativePath + "/" + child.getName());
                if (found != null) {
                    return found;
                }
            }
        } else if ("java".equals(file.getExt())) {
            if (file.getName().equals(className)) {
                return file;
            }
        }
        return null;
    }

    private String getFilePathFromEditor(JTextComponent editor) {
        try {
            org.openide.loaders.DataObject dobj = NbEditorUtilities.getDataObject(editor.getDocument());
            if (dobj != null) {
                return dobj.getPrimaryFile().getPath();
            }
        } catch (Exception ex) {
            // Handle exception
        }
        return null;
    }

    public static TreePath findTreePathAtCaret(
        final CompilationUnitTree compilationUnit, final JavacTask task, final int offset
    ) throws IOException {
        Trees trees = Trees.instance(task);
        DocTrees docTrees = DocTrees.instance(task);  // Get the instance of DocTrees
        MyTreePathScanner treePathScanner = new MyTreePathScanner(trees, docTrees, offset, compilationUnit);
        treePathScanner.scan(compilationUnit, null);
        TreePath resultPath = treePathScanner.getTargetPath();
        return resultPath;
    }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.jeddict.javadoc.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import static io.github.jeddict.javadoc.ai.FileUtil.saveOpenEditor;
import static io.github.jeddict.javadoc.ai.JavadocAction.ENHANCE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Kind;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;

@Hint(displayName = "#DN_JavaDoc", description = "#DESC_JavaDoc",
        id = "io.github.jeddict.javadoc.ai.JavaDocHint",
        category = "suggestions",
        enabled = true,
        options = Options.QUERY,
        hintKind = Kind.ACTION,
        severity = org.netbeans.spi.editor.hints.Severity.HINT)
public class JavaDocHint {

    protected final WorkingCopy copy = null;

    public JavaDocHint() {
    }

    @TriggerTreeKind({Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.METHOD, Tree.Kind.VARIABLE})
    public static ErrorDescription run(HintContext ctx) {
        CompilationInfo compilationInfo = ctx.getInfo();
        TreePath treePath = ctx.getPath();
        if (treePath == null) {
            return null;
        }

        Fix[] fixes = new Fix[2];
        TreePathHandle tpHandle = TreePathHandle.create(treePath, compilationInfo);

        Tree.Kind kind = treePath.getLeaf().getKind();
        switch (kind) {
            case CLASS:
            case INTERFACE:
                TypeElement type = (TypeElement) compilationInfo.getTrees().getElement(treePath);
                ElementHandle<TypeElement> elementHandle = ElementHandle.create(type);
                fixes[0] = new JavaFixImpl(tpHandle, JavadocAction.CREATE, elementHandle).toEditorFix();
                fixes[1] = new JavaFixImpl(tpHandle, JavadocAction.ENHANCE, elementHandle).toEditorFix();
                break;

            case METHOD:
                Element methodElement = compilationInfo.getTrees().getElement(treePath);
                if (methodElement != null && methodElement.getKind() == ElementKind.METHOD) {
                    ElementHandle<ExecutableElement> methodHandle = ElementHandle.create((ExecutableElement) methodElement);
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes[0] = new JavaFixImpl(tpHandle, JavadocAction.CREATE, methodHandle).toEditorFix();
                    fixes[1] = new JavaFixImpl(tpHandle, JavadocAction.ENHANCE, methodHandle).toEditorFix();
                }
                break;

            case VARIABLE:
                Element fieldElement = compilationInfo.getTrees().getElement(treePath);
                if (fieldElement != null && fieldElement.getKind() == ElementKind.FIELD) {
                    ElementHandle<VariableElement> fieldHandle = ElementHandle.create((VariableElement) fieldElement);
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes[0] = new JavaFixImpl(tpHandle, JavadocAction.CREATE, fieldHandle).toEditorFix();
                    fixes[1] = new JavaFixImpl(tpHandle, JavadocAction.ENHANCE, fieldHandle).toEditorFix();
                }
                break;

            default:
                return null;
        }
        String desc = NbBundle.getMessage(JavaDocHint.class, "ERR_JavaDoc"); //NOI18N
        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), desc, fixes); //NOI18N
    }

    private static final class JavaFixImpl extends JavaFix {

        private final JavadocAction action;
        private final ElementHandle classType;

        public JavaFixImpl(TreePathHandle tpHandle, JavadocAction type,
                ElementHandle classType) {
            super(tpHandle);
            this.action = type;
            this.classType = classType;
        }

        @Override
        protected String getText() {
            switch (action) {
                case ENHANCE:
                    return NbBundle.getMessage(getClass(), "HINT_JavaDoc_Generated", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
                default:
                    return NbBundle.getMessage(getClass(), "HINT_JavaDoc", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            }
        }


        @Override
        protected void performRewrite(TransformationContext tc) throws Exception {
            WorkingCopy copy = tc.getWorkingCopy();
            if (copy.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                return;
            }
            saveOpenEditor();

            TreePath treePath = tc.getPath();
            Tree leaf = treePath.getLeaf();
            Element elm = copy.getTrees().getElement(treePath);
            if (elm == null) {
                return;
            }

            String javadocContent;
             DocCommentTree oldDocCommentTree = ((DocTrees) copy.getTrees()).getDocCommentTree(treePath);

            switch (leaf.getKind()) {
                case CLASS:
                case INTERFACE:
                    if (action == ENHANCE) {
                        javadocContent = new JavaDocChatModel().enhanceJavadocForClass(oldDocCommentTree.toString(), leaf.toString());
                    } else {
                        javadocContent = new JavaDocChatModel().generateJavadocForClass(leaf.toString());
                    }
                    break;
                case METHOD:
                    if (action == ENHANCE) {
                        javadocContent = new JavaDocChatModel().enhanceJavadocForMethod(oldDocCommentTree.toString(), ((MethodTree) leaf).getName().toString());
                    } else {
                        javadocContent = new JavaDocChatModel().generateJavadocForMethod(((MethodTree) leaf).getName().toString());
                    }
                    break;
                case VARIABLE:
                    if (action == ENHANCE) {
                        javadocContent = new JavaDocChatModel().enhanceJavadocForField(oldDocCommentTree.toString(), ((VariableTree) leaf).getName().toString());
                    } else {
                        javadocContent = new JavaDocChatModel().generateJavadocForField(((VariableTree) leaf).getName().toString());
                    }
                    break;
                default:
                    return;
            }

            Path filePath = Paths.get(copy.getFileObject().toURI());

            String sourceCode = new String(Files.readAllBytes(filePath));
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(sourceCode).getResult().orElse(null);

            if (cu == null) {
                return;
            }

           
            // Add the Javadoc comment to the appropriate location
            switch (leaf.getKind()) {
                case CLASS:
                case INTERFACE:
                    String className = ((ClassTree) leaf).getSimpleName().toString();
                    cu.findFirst(ClassOrInterfaceDeclaration.class)
                            .filter(decl -> decl.getNameAsString().equals(className))
                            .ifPresent(classDecl -> {
                                JavadocComment comment = new JavadocComment(removeCodeBlockMarkers(javadocContent));
                                classDecl.setComment(comment);
                            });
                    break;
                case METHOD:
                    MethodTree methodTree = (MethodTree) leaf;
                    String methodName = methodTree.getName().toString();

                    cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
                        classDecl.findAll(MethodDeclaration.class).stream()
                                .filter(methodDecl -> methodDecl.getNameAsString().equals(methodName))
                                .findFirst()
                                .ifPresent(methodDecl -> {
                                    JavadocComment comment = new JavadocComment(removeCodeBlockMarkers(javadocContent));
                                    methodDecl.setComment(comment);
                                });
                    });
                    break;
                case VARIABLE:
                    cu.findFirst(FieldDeclaration.class).ifPresent(fieldDecl -> {
                        fieldDecl.getVariables().forEach(varDecl -> {
                            if (varDecl.getNameAsString().equals(((VariableTree) leaf).getName().toString())) {
                                JavadocComment comment = new JavadocComment(removeCodeBlockMarkers(javadocContent));
                                fieldDecl.setComment(comment);
                            }
                        });
                    });
                    break;
                default:
                    return;
            }

            // Write the modified code back to the file
            String modifiedCode = cu.toString();
            Files.write(filePath, modifiedCode.getBytes());
        }

        private static String removeCodeBlockMarkers(String input) {
            // Check if the input starts and ends with the markers
            input = input.trim();
            if (input.startsWith("```java") && input.endsWith("```")) {
                // Remove the starting ```java\n and the ending ```
                String content = input.substring(7);  // Remove ```java\n (7 characters)
                content = content.substring(0, content.length() - 3);  // Remove ```
                input = content.trim();
            } else if (input.startsWith("```") && input.endsWith("```")) {
                // Remove the starting ```java\n and the ending ```
                String content = input.substring(3);  // Remove ```java\n (7 characters)
                content = content.substring(0, content.length() - 3);  // Remove ```
                input = content.trim();
            }
            input = input.trim();
            if (input.startsWith("/**") && input.endsWith("*/")) {
                input = input.substring(3, input.length() - 2).trim();
            }
            return input;  // Return the original input if it does not match the expected format
        }

        public static String trimLeadingSpaces(String str) {
            if (str == null) {
                return null;
            }
            int start = 0;
            while (start < str.length() && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
            return str.substring(start);
        }

        private void updateJavadoc(WorkingCopy copy, TreePath treePath, TreeMaker make, String javadocContent, ElementKind elementKind) {
            List<DocTree> firstSentence = new LinkedList<>();
            List<DocTree> tags = new LinkedList<>();
            List<DocTree> body = new LinkedList<>();
            boolean ci = treePath.getLeaf().getKind() == Tree.Kind.CLASS || treePath.getLeaf().getKind() == Tree.Kind.INTERFACE;

            // Split the content into lines for simulation
            String[] lines = javadocContent.split("\n");
            for (String line : lines) {
                line = trimLeadingSpaces(line);
                if (line.startsWith("/**") || line.startsWith("*/")) {
                } else if (line.startsWith("* @")) {
                    tags.add(make.Text(line.substring(1)));
                } else if (line.startsWith("*") || line.isEmpty()) {
                    if (line.startsWith("*")) {
                        firstSentence.add(make.Text(line.substring(1)));
                        if (ci) {
                            firstSentence.add(make.Text("\n * "));
                        } else {
                            firstSentence.add(make.Text("\n     * "));
                        }
                    } else if (line.isEmpty()) {
                        firstSentence.add(make.Text(""));
                    }
                }
            }
            if (!firstSentence.isEmpty() && firstSentence.get(firstSentence.size() - 1).toString().equals("\n * ")) {
                firstSentence.remove(firstSentence.size() - 1);
            }
            if (!firstSentence.isEmpty() && firstSentence.get(firstSentence.size() - 1).toString().equals("\n     * ")) {
                firstSentence.remove(firstSentence.size() - 1);
            }

            DocCommentTree oldDocCommentTree = ((DocTrees) copy.getTrees()).getDocCommentTree(treePath);
            DocCommentTree newDocCommentTree = make.DocComment(firstSentence, body, tags);
            copy.rewrite(treePath.getLeaf(), oldDocCommentTree, newDocCommentTree);
        }

    }
}

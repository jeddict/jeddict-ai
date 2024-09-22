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
package io.github.jeddict.ai.fix;

import io.github.jeddict.ai.util.StringUtil;
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
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.Action;
import io.github.jeddict.ai.JeddictChatModel;
import static io.github.jeddict.ai.Action.ENHANCE;
import static io.github.jeddict.ai.util.FileUtil.saveOpenEditor;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import static io.github.jeddict.ai.util.StringUtil.trimLeadingSpaces;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class JavaDocFixImpl extends JavaFix {

    private final Action action;
    private final ElementHandle classType;

    public JavaDocFixImpl(TreePathHandle tpHandle, Action type,
            ElementHandle classType) {
        super(tpHandle);
        this.action = type;
        this.classType = classType;
    }

    @Override
    protected String getText() {
        switch (action) {
            case ENHANCE:
                return NbBundle.getMessage(JeddictChatModel.class, "HINT_JAVADOC_GENERATED", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            default:
                return NbBundle.getMessage(JeddictChatModel.class, "HINT_JAVADOC", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            }
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
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
                    javadocContent = new JeddictChatModel().enhanceJavadocForClass(oldDocCommentTree.toString(), leaf.toString());
                } else {
                    javadocContent = new JeddictChatModel().generateJavadocForClass(leaf.toString());
                }
                break;
            case METHOD:
                if (action == ENHANCE) {
                    javadocContent = new JeddictChatModel().enhanceJavadocForMethod(oldDocCommentTree.toString(), ((MethodTree) leaf).getName().toString());
                } else {
                    javadocContent = new JeddictChatModel().generateJavadocForMethod(((MethodTree) leaf).getName().toString());
                }
                break;
            case VARIABLE:
                if (action == ENHANCE) {
                    javadocContent = new JeddictChatModel().enhanceJavadocForField(oldDocCommentTree.toString(), ((VariableTree) leaf).getName().toString());
                } else {
                    javadocContent = new JeddictChatModel().generateJavadocForField(((VariableTree) leaf).getName().toString());
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

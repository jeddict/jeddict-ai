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
package io.github.jeddict.ai.hints;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.lang.JeddictBrain;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
 */
public class VariableNameFix extends JavaFix {

    private final TreePath treePath;
    private final Action action;

    public VariableNameFix(TreePathHandle tpHandle, Action action, TreePath treePath) {
        super(tpHandle);
        this.treePath = treePath;
        this.action = action;
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_VARIABLE_NAME_ENHANCE");
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }

        TreePath path = tc.getPath();
        Tree leaf = path.getLeaf();
        Element elm = copy.getTrees().getElement(path);

        if (elm instanceof VariableElement) {
            String oldName = elm.toString();
            String newName = new JeddictBrain().enhanceVariableName(
                    oldName,
                    path.getParentPath().getLeaf().toString(),
                    copy.getCompilationUnit().toString()
            );

            if (leaf.getKind() == Tree.Kind.VARIABLE) {
                VariableTree oldVarTree = (VariableTree) leaf;
                TreeMaker maker = copy.getTreeMaker();

                VariableTree newVarTree = maker.Variable(
                        oldVarTree.getModifiers(),
                        newName,
                        oldVarTree.getType(),
                        oldVarTree.getInitializer()
                );

                handleLocalVariable(copy, path, oldVarTree, newVarTree);
                handleMethodParameter(copy, path, oldVarTree, newVarTree, oldName, newName);
                handleClassField(copy, path, oldVarTree, newVarTree, oldName, newName);
            }
        }
    }

    private void handleLocalVariable(WorkingCopy copy, TreePath path, VariableTree oldVarTree, VariableTree newVarTree) {
        Tree parent = path.getParentPath().getLeaf();
        if (parent instanceof BlockTree) {
            BlockTree blockTree = (BlockTree) parent;
            List<? extends StatementTree> statements = blockTree.getStatements();
            List<StatementTree> newStatements = statements.stream()
                    .map(s -> s.equals(oldVarTree) ? newVarTree : s)
                    .collect(Collectors.toList());

            BlockTree newBlockTree = copy.getTreeMaker().Block(newStatements, blockTree.isStatic());
            newBlockTree = updateBody(copy, newBlockTree, copy.getElements().getName(oldVarTree.getName().toString()), copy.getElements().getName(newVarTree.getName().toString()));
            copy.rewrite(blockTree, newBlockTree);
        }
    }

    private void handleMethodParameter(WorkingCopy copy, TreePath path, VariableTree oldVarTree, VariableTree newVarTree, String oldName, String newName) {
        Tree parent = path.getParentPath().getLeaf();
        if (parent instanceof MethodTree) {
            MethodTree methodTree = (MethodTree) parent;
            List<? extends VariableTree> newParams = methodTree.getParameters().stream()
                    .map(p -> p.equals(oldVarTree) ? newVarTree : p)
                    .collect(Collectors.toList());

            BlockTree oldBody = methodTree.getBody();
            BlockTree newBody = updateBody(copy, oldBody, copy.getElements().getName(oldName), copy.getElements().getName(newName));
            MethodTree newMethodTree = copy.getTreeMaker().Method(
                    methodTree.getModifiers(),
                    methodTree.getName().toString(),
                    methodTree.getReturnType(),
                    methodTree.getTypeParameters(),
                    newParams,
                    methodTree.getThrows(),
                    newBody,
                    (methodTree.getDefaultValue() instanceof ExpressionTree)
                    ? (ExpressionTree) methodTree.getDefaultValue()
                    : null
            );

            copy.rewrite(methodTree, newMethodTree);
        }
    }

    private void handleClassField(WorkingCopy copy, TreePath path, VariableTree oldVarTree, VariableTree newVarTree, String oldName, String newName) {
        Tree parent = path.getParentPath().getLeaf();
        if (parent instanceof ClassTree) {
            ClassTree classTree = (ClassTree) parent;

            // Update class fields
            List<? extends Tree> members = classTree.getMembers();
            List<Tree> newMembers = members.stream()
                    .map(m -> m.equals(oldVarTree) ? newVarTree : m)
                    .collect(Collectors.toList());

            ClassTree newClassTree = copy.getTreeMaker().Class(
                    classTree.getModifiers(),
                    classTree.getSimpleName(),
                    classTree.getTypeParameters(),
                    classTree.getExtendsClause(),
                    classTree.getImplementsClause(),
                    newMembers
            );

            copy.rewrite(classTree, newClassTree);

            // Traverse the class to update variable names inside methods and blocks
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitMethod(MethodTree methodTree, Void p) {
                    // Update variables in method parameters and bodies
                    List<? extends VariableTree> newParams = methodTree.getParameters().stream()
                            .map(param -> param.getName().contentEquals(oldName) ? copy.getTreeMaker().Variable(
                            param.getModifiers(), copy.getElements().getName(newName), param.getType(), param.getInitializer()) : param)
                            .collect(Collectors.toList());

                    BlockTree oldBody = methodTree.getBody();
                    if (oldBody != null) {
                        BlockTree newBody = updateBody(copy, oldBody, copy.getElements().getName(oldName), copy.getElements().getName(newName));
                        MethodTree newMethodTree = copy.getTreeMaker().Method(
                                methodTree.getModifiers(),
                                methodTree.getName(),
                                methodTree.getReturnType(),
                                methodTree.getTypeParameters(),
                                newParams,
                                methodTree.getThrows(),
                                newBody,
                                (methodTree.getDefaultValue() instanceof ExpressionTree)
                                ? (ExpressionTree) methodTree.getDefaultValue()
                                : null
                        );
                        copy.rewrite(methodTree, newMethodTree);
                    }
                    return super.visitMethod(methodTree, p);
                }

                @Override
                public Void visitVariable(VariableTree variableTree, Void p) {
                    if (variableTree.getName().contentEquals(oldName)) {
                        VariableTree newVar = copy.getTreeMaker().Variable(
                                variableTree.getModifiers(),
                                copy.getElements().getName(newName),
                                variableTree.getType(),
                                variableTree.getInitializer());
                        copy.rewrite(variableTree, newVar);
                    }
                    return super.visitVariable(variableTree, p);
                }

                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    if (node.getName().contentEquals(oldName)) {
                        copy.rewrite(node, copy.getTreeMaker().Identifier(copy.getElements().getName(newName)));
                    }
                    return super.visitIdentifier(node, p);
                }
            }.scan(newClassTree, null);
        }
    }

    private BlockTree updateBody(WorkingCopy copy, BlockTree oldBody, Name oldName, Name newName) {
        TreeMaker maker = copy.getTreeMaker();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().contentEquals(oldName)) {
                    copy.rewrite(node, maker.Identifier(newName));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(oldBody, null);
        return oldBody;
    }

}

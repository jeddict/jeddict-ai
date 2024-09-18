/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
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
 * @author Gaurav Gupta
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
        return NbBundle.getMessage(getClass(), "HINT_VARIABLE_NAME_ENHANCE");
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
            String newName = new JeddictChatModel().enhanceVariableName(
                    oldName,
                    path.getParentPath().getLeaf().toString(),
                    copy.getCompilationUnit().toString()
            );

            if (leaf.getKind() == Tree.Kind.VARIABLE) {
                VariableTree oldVarTree = (VariableTree) leaf;
                TreeMaker maker = copy.getTreeMaker();

                // Create a new VariableTree with the updated name
                VariableTree newVarTree = maker.Variable(
                        oldVarTree.getModifiers(),
                        newName,
                        oldVarTree.getType(),
                        oldVarTree.getInitializer()
                );

                // Handle local variables
                Tree parent = path.getParentPath().getLeaf();
                if (parent instanceof BlockTree) {
                    BlockTree blockTree = (BlockTree) parent;
                    List<? extends StatementTree> statements = blockTree.getStatements();

                    // Replace the old variable tree with the new one in the list of statements
                    List<StatementTree> newStatements = statements.stream()
                            .map(s -> s.equals(oldVarTree) ? newVarTree : s)
                            .collect(Collectors.toList());

                    // Create a new BlockTree with the updated statements
                    BlockTree newBlockTree = maker.Block(newStatements, blockTree.isStatic());
                    newBlockTree = updateBody(copy, newBlockTree, copy.getElements().getName(oldName), copy.getElements().getName(newName));
                   

                    // Rewrite the old block with the new block
                    copy.rewrite(blockTree, newBlockTree);
                } else if (parent instanceof MethodTree) {
                    MethodTree methodTree = (MethodTree) parent;
                    List<? extends VariableTree> newParams = methodTree.getParameters().stream()
                            .map(p -> p.equals(oldVarTree) ? newVarTree : p)
                            .collect(Collectors.toList());

                    BlockTree oldBody = methodTree.getBody();
                    BlockTree newBody = updateBody(copy, oldBody, copy.getElements().getName(oldName), copy.getElements().getName(newName));
                    MethodTree newMethodTree = maker.Method(
                            methodTree.getModifiers(),
                            methodTree.getName().toString(), // Convert Name to String
                            methodTree.getReturnType(),
                            methodTree.getTypeParameters(),
                            newParams, // Updated parameter list
                            methodTree.getThrows(),
                            newBody,
                            (methodTree.getDefaultValue() instanceof ExpressionTree)
                            ? (ExpressionTree) methodTree.getDefaultValue()
                            : null // Handle default value if necessary
                    );

                    copy.rewrite(methodTree, newMethodTree);
                }
            }
        }
    }

    private BlockTree updateBody(WorkingCopy copy, BlockTree oldBody, Name oldName, Name newName) {
        TreeMaker maker = copy.getTreeMaker();

        // Create a new BlockTree by rewriting the old body
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().contentEquals(oldName)) {
                    // Rewrite the identifier with the new parameter name
                    copy.rewrite(node, maker.Identifier(newName));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(oldBody, null);

        // Return the (potentially modified) oldBody, as modifications are done in-place
        return oldBody;
    }

}

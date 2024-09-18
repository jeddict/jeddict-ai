/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.javadoc.ai;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import static io.github.jeddict.javadoc.ai.FileUtil.saveOpenEditor;
import javax.tools.ToolProvider;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.Context;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class ExpressionFix extends JavaFix {

    private final TreePath treePath;
    private final Action action;

    public ExpressionFix(TreePathHandle tpHandle, Action action, TreePath treePath) {
        super(tpHandle);
        this.treePath = treePath;
        this.action = action;
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(getClass(), "HINT_ENHANCE_EXPRESSION");
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        saveOpenEditor();

        Tree leaf = treePath.getLeaf();
        String content;
        com.sun.source.tree.ExpressionStatementTree expressionStatement = (com.sun.source.tree.ExpressionStatementTree) leaf;
        content = new JeddictChatModel().enhanceExpressionStatement(
                treePath.getCompilationUnit().toString(),
                treePath.getParentPath().getLeaf().toString(), 
                treePath.getLeaf().toString()
        );
        content = StringUtil.removeCodeBlockMarkers(content);
        if (content.endsWith(";")) {
            content = content.substring(0, content.length() - 1).trim();
        }
        ExpressionTree newExpressionTree = copy.getTreeMaker().QualIdent(content);
        ExpressionStatementTree newExpressionStatement = copy.getTreeMaker().ExpressionStatement(newExpressionTree);
        copy.rewrite(expressionStatement, newExpressionStatement);
    }

}

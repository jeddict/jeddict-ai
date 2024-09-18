/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.fix;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.Action;
import io.github.jeddict.ai.JeddictChatModel;
import static io.github.jeddict.ai.util.FileUtil.saveOpenEditor;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class TextFix extends JavaFix {

    private final TreePath treePath;
    private final Action action;

    public TextFix(TreePathHandle tpHandle, Action action, TreePath treePath) {
        super(tpHandle);
        this.treePath = treePath;
        this.action = action;
    }

    @Override
    protected String getText() {
        if (action == Action.ENHANCE) {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_ENHANCE_TEXT");
        } else {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_FIX_GRAMMAR");
        }
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        saveOpenEditor();

        Tree leaf = treePath.getLeaf();
        if (leaf.getKind() != STRING_LITERAL) {
            return;
        }
        String content;
        if (action == Action.ENHANCE) {
            content = new JeddictChatModel().enhanceText(treePath.getLeaf().toString(), treePath.getCompilationUnit().toString());
        } else {
            content = new JeddictChatModel().fixGrammar(treePath.getLeaf().toString(), treePath.getCompilationUnit().toString());
        }
        if (content != null && content.length() > 1 && content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        LiteralTree oldLiteral = (LiteralTree) treePath.getLeaf();
        LiteralTree newLiteral = copy.getTreeMaker().Literal(content);
        copy.rewrite(oldLiteral, newLiteral);
    }
}

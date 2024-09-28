/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.fix;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.Action;
import io.github.jeddict.ai.JeddictChatModel;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.FileUtil.saveOpenEditor;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import javax.lang.model.element.Element;
import org.json.JSONArray;
import org.json.JSONObject;
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
public class VariableFix extends JavaFix {

    private ElementHandle classType;
    private final Action action;
    private String actionTitleParam;
    private String compliationError;

    public VariableFix(TreePathHandle tpHandle, Action action, ElementHandle classType) {
        super(tpHandle);
        this.classType = classType;
        this.action = action;
    }

    public VariableFix(TreePathHandle tpHandle, String compliationError, String actionTitleParam) {
        super(tpHandle);
        this.compliationError = compliationError;
        this.actionTitleParam = actionTitleParam;
        this.action = Action.COMPILATION_ERROR;
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(JeddictChatModel.class, "HINT_VARIABLE_COMPILATION_ERROR", actionTitleParam);
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }

        TreePath treePath = tc.getPath();
        Tree leaf = treePath.getLeaf();

        Element elm = copy.getTrees().getElement(treePath);
        if (elm == null) {
            return;
        }

        String content = null;

        // Check if it's a variable and there's an error to fix
        if (leaf.getKind() == Tree.Kind.VARIABLE && action == Action.COMPILATION_ERROR) {
            content = new JeddictChatModel().fixVariableError(leaf.toString(), compliationError);
        }

        if (content == null) {
            return;
        }

        // Parse the content as JSON
        JSONObject json = new JSONObject(removeCodeBlockMarkers(content));
        JSONArray imports = json.getJSONArray("imports");
        String variableContent = json.getString("variableContent");

        SourceUtil.addImports(copy, imports);
        copy.rewrite(leaf, copy.getTreeMaker().QualIdent(variableContent));
    }

}

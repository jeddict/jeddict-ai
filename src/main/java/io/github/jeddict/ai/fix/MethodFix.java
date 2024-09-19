/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.fix;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.Action;
import io.github.jeddict.ai.JeddictChatModel;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.FileUtil.saveOpenEditor;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import static io.github.jeddict.ai.util.UIUtil.askQuery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.lang.model.element.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class MethodFix extends JavaFix {

    private ElementHandle classType;
    private final Action action;
    private String actionTitleParam;
    private String compliationError;

    public MethodFix(TreePathHandle tpHandle, Action action, ElementHandle classType) {
        super(tpHandle);
        this.classType = classType;
        this.action = action;
    }

    public MethodFix(TreePathHandle tpHandle, String compliationError, String actionTitleParam) {
        super(tpHandle);
        this.compliationError = compliationError;
        this.actionTitleParam = actionTitleParam;
        this.action = Action.COMPILATION_ERROR;
    }

    @Override
    protected String getText() {
        if (action == Action.COMPILATION_ERROR) {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_METHOD_COMPILATION_ERROR", actionTitleParam);
        } else if (action == Action.ENHANCE) {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_METHOD_ENHANCE");
        } else {
            return NbBundle.getMessage(JeddictChatModel.class, "HINT_METHOD_QUERY");
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

        String content = null;

        if (leaf.getKind() == METHOD) {
            if (action == Action.COMPILATION_ERROR) {
                content = new JeddictChatModel().fixMethodCompilationError(treePath.getParentPath().getLeaf().toString(), leaf.toString(), compliationError);
            } else if (action == Action.ENHANCE) {
                content = new JeddictChatModel().enhanceMethodFromMethodContent(treePath.getParentPath().getLeaf().toString(), leaf.toString());
            } else {
                String query = askQuery();
                if (query == null) {
                    return;
                }
                content = new JeddictChatModel().updateMethodFromDevQuery(treePath.getParentPath().getLeaf().toString(), leaf.toString(), query);
            }
        }

        Path filePath = Paths.get(copy.getFileObject().toURI());

        String sourceCode = new String(Files.readAllBytes(filePath));
        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = javaParser.parse(sourceCode).getResult().orElse(null);

        if (cu == null || content == null) {
            return;
        }

        JSONObject json = new JSONObject(removeCodeBlockMarkers(content));
        JSONArray imports = json.getJSONArray("imports");
        String methodContent = json.getString("methodContent");
        SourceUtil.addImports(copy, imports);
        copy.rewrite(leaf, copy.getTreeMaker().QualIdent(methodContent));
    }


}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.fix;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
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
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Gaurav Gupta
 */
public class RestEndpointFix extends JavaFix {

    private final ElementHandle classType;
    private final Action action;

    public RestEndpointFix(TreePathHandle tpHandle, Action action, ElementHandle classType) {
        super(tpHandle);
        this.classType = classType;
        this.action = action;
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(JeddictChatModel.class, "HINT_REST_ENDPOINT");
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

        if (leaf.getKind() == CLASS || leaf.getKind() == INTERFACE) {

            String javadocContent = new JeddictChatModel().generateRestEndpointForClass(leaf.toString());
            JSONObject json = new JSONObject(removeCodeBlockMarkers(javadocContent));
            JSONArray imports = json.getJSONArray("imports");
            String methodContent = json.getString("methodContent");

            String halfTab = "    ";
            String[] lines = methodContent.split("\n");
            StringBuilder modifiedContent = new StringBuilder("\n");
            for (String line : lines) {
                if (line.isBlank()) {
                    modifiedContent.append(line).append("\n");
                } else {
                    modifiedContent.append(halfTab).append(line).append("\n");
                }
            }

            methodContent = modifiedContent.toString();
            SourceUtil.addImports(copy, imports);
            ClassTree classTree = copy.getTreeMaker().addClassMember((ClassTree) leaf, copy.getTreeMaker().QualIdent(methodContent));
            copy.rewrite(leaf, classTree);
        }
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.javadoc.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.util.TreePath;
import static io.github.jeddict.javadoc.ai.FileUtil.saveOpenEditor;
import static io.github.jeddict.javadoc.ai.JavaParserUtil.updateMethods;
import static io.github.jeddict.javadoc.ai.StringUtil.removeCodeBlockMarkers;
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

    private final ElementHandle classType;
    private final Action action;

    public MethodFix(TreePathHandle tpHandle, Action action, ElementHandle classType) {
        super(tpHandle);
        this.classType = classType;
        this.action = action;
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(getClass(), "HINT_METHOD_ENHANCE");
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

        switch (leaf.getKind()) {
            case METHOD:
                content = new JeddictChatModel().createMethodFromMethodContent(treePath.getParentPath().getLeaf().toString(), leaf.toString());
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
        switch (leaf.getKind()) {
            case METHOD:
                JavaParserUtil.addImports(cu, imports);
                updateMethods(cu, (MethodTree) leaf, imports, methodContent, javaParser);
                break;
        }

        String modifiedCode = cu.toString();
        Files.write(filePath, modifiedCode.getBytes());
        
        SourceUtil.fixImports(copy.getFileObject());
    }
}

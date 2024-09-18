/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.javadoc.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.sun.source.doctree.DocCommentTree;
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
import static io.github.jeddict.javadoc.ai.Action.ENHANCE;
import static io.github.jeddict.javadoc.ai.FileUtil.saveOpenEditor;
import static io.github.jeddict.javadoc.ai.JavaParserUtil.addImports;
import static io.github.jeddict.javadoc.ai.JavaParserUtil.addMethods;
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
        return NbBundle.getMessage(getClass(), "HINT_REST_ENDPOINT");
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

        String javadocContent = null;

        switch (leaf.getKind()) {
            case CLASS:
            case INTERFACE:
                javadocContent = new JeddictChatModel().generateRestEndpointForClass(leaf.toString());
        }

        Path filePath = Paths.get(copy.getFileObject().toURI());

        String sourceCode = new String(Files.readAllBytes(filePath));
        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = javaParser.parse(sourceCode).getResult().orElse(null);

        if (cu == null || javadocContent == null) {
            return;
        }

        JSONObject json = new JSONObject(removeCodeBlockMarkers(javadocContent));
        JSONArray imports = json.getJSONArray("imports");
        JSONArray methods = json.getJSONArray("methods");

        switch (leaf.getKind()) {
            case CLASS:
            case INTERFACE:
                addImports(cu, imports);
                addMethods(cu, methods, javaParser);
                break;
            default:
                return;
        }

        String modifiedCode = cu.toString();
        Files.write(filePath, modifiedCode.getBytes());
        
        SourceUtil.fixImports(copy.getFileObject());

    }
}

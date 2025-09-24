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

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.completion.Action;
import static io.github.jeddict.ai.completion.Action.ENHANCE;
import io.github.jeddict.ai.lang.JeddictBrain;
import static io.github.jeddict.ai.util.SourceUtil.geIndentaion;
import io.github.jeddict.ai.util.StringUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import javax.lang.model.element.Element;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
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
                return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_JAVADOC_GENERATED", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            default:
                return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_JAVADOC", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            }
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }

        TreePath treePath = tc.getPath();
        Tree tree = treePath.getLeaf();
        Element elm = copy.getTrees().getElement(treePath);
        if (elm == null) {
            return;
        }

        Document document = copy.getDocument();

        String javadocContent;
        DocCommentTree oldDocCommentTree = copy.getDocTrees().getDocCommentTree(treePath);

        switch (tree.getKind()) {
            case CLASS:
            case INTERFACE:
                if (action == ENHANCE) {
                    javadocContent = new JeddictBrain().enhanceJavadocForClass(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                oldDocCommentTree.toString(), tree.toString());
                } else {
                    javadocContent = new JeddictBrain().generateJavadocForClass(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                tree.toString());
                }
                break;
            case METHOD:
                if (action == ENHANCE) {
                    javadocContent = new JeddictBrain().enhanceJavadocForMethod(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                oldDocCommentTree.toString(), ((MethodTree) tree).toString());
                } else {
                    javadocContent = new JeddictBrain().generateJavadocForMethod(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                ((MethodTree) tree).toString());
                }
                break;
            case VARIABLE:
                if (action == ENHANCE) {
                    javadocContent = new JeddictBrain().enhanceJavadocForField(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                oldDocCommentTree.toString(), ((VariableTree) tree).toString());
                } else {
                    javadocContent = new JeddictBrain().generateJavadocForField(
                FileOwnerQuery.getOwner(copy.getFileObject()), 
                ((VariableTree) tree).toString());
                }
                break;
            default:
                return;
        }
        javadocContent = removeCodeBlockMarkers(javadocContent);

        int startOffset = (int) copy.getTrees().getSourcePositions()
                .getStartPosition(copy.getCompilationUnit(), tree);

        if (document != null) {
            String lastLine = geIndentaion(copy, tree);
            if (lastLine.isBlank() && lastLine.length() <= 12) {
                StringBuilder indentedContent = new StringBuilder();

                boolean ignore = true;
                for (String line : javadocContent.split("\n")) {
                    if (ignore) {
                        ignore = false;
                        indentedContent.append(line).append("\n").append(lastLine);
                    } else {
                        indentedContent.append(line).append("\n").append(lastLine);
                    }
                }
                javadocContent = indentedContent.toString();
            } else {
                javadocContent = javadocContent + '\n';
            }
            document.insertString(startOffset, javadocContent, null);
        }

        if (action == ENHANCE && oldDocCommentTree != null && document != null) {
            DocTrees docTrees = copy.getDocTrees();
            CompilationUnitTree cuTree = copy.getCompilationUnit();

            long start = docTrees.getSourcePositions().getStartPosition(cuTree, oldDocCommentTree, oldDocCommentTree);
            long end = docTrees.getSourcePositions().getEndPosition(cuTree, oldDocCommentTree, oldDocCommentTree);

            int startPos = (int) start;
            int endPos = (int) end;

            try {
                // Search for '*/' after the end position of the current comment
                String content = document.getText(endPos, document.getLength() - endPos);
                int afterEndPos = content.indexOf("*/") + endPos + 2; // Position after the '*/'

                // Search for '/**' before the start position of the current comment
                content = document.getText(0, startPos);
                int beforeStartPos = content.lastIndexOf("/**");

                // Remove all space until a newline character is found before '/**'
                while (beforeStartPos > 0 && content.charAt(beforeStartPos) != '\n') {
                    beforeStartPos--; // Move backward to include spaces before the newline
                }

                // Remove the entire Javadoc, from 'beforeStartPos' to 'afterEndPos'
                if (beforeStartPos >= 0 && afterEndPos > beforeStartPos) { // Ensure valid positions
                    document.remove(beforeStartPos, afterEndPos - beforeStartPos);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

    }

}

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

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.lang.JeddictBrain;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
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
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_ENHANCE_TEXT");
        } else {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_FIX_GRAMMAR");
        }
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }

        Tree leaf = treePath.getLeaf();
        if (leaf.getKind() != STRING_LITERAL) {
            return;
        }
        String content;
        if (action == Action.ENHANCE) {
            content = new JeddictBrain().enhanceText(treePath.getLeaf().toString(), treePath.getCompilationUnit().toString());
        } else {
            content = new JeddictBrain().fixGrammar(treePath.getLeaf().toString(), treePath.getCompilationUnit().toString());
        }
        if (content != null && content.length() > 1 && content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        LiteralTree oldLiteral = (LiteralTree) treePath.getLeaf();
        LiteralTree newLiteral = copy.getTreeMaker().Literal(content);
        copy.rewrite(oldLiteral, newLiteral);
    }
}

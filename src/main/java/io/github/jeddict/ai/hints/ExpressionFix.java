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

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.agent.pair.RefactorSpecialist;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.util.StringUtil;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
 */
public class ExpressionFix extends TreePathAIFix {

    public ExpressionFix(final TreePathHandle treePathHandle, final Action action, final TreePath treePath) {
        super(treePathHandle, action, treePath);
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_ENHANCE_EXPRESSION");
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }

        final Tree leaf = treePath.getLeaf();
        final com.sun.source.tree.ExpressionStatementTree expressionStatement = (com.sun.source.tree.ExpressionStatementTree) leaf;

        final RefactorSpecialist pair = newJeddictBrain().pairProgrammer(PairProgrammer.Specialist.REFACTOR);

        String content = pair.enhanceExpressionStatement(
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

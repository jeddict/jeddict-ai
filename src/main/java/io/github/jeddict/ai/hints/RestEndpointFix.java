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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.agent.pair.RestSpecialist;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import javax.lang.model.element.Element;
import javax.swing.SwingUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Shiwani Gupta
 */
public class RestEndpointFix extends BaseAIFix {

    public RestEndpointFix(final TreePathHandle treePathHandle, final Action action) {
        super(treePathHandle, action);
    }

    @Override
    protected String getText() {
        return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_REST_ENDPOINT");
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        try (
            ProgressHandle progress = ProgressHandle.createHandle(NbBundle.getMessage(JeddictUpdateManager.class, "PROGRESS_TASK_1"))
        ) {
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

            SwingUtilities.invokeLater(() -> progress.start());

            if (leaf.getKind() == CLASS || leaf.getKind() == INTERFACE) {
                RestSpecialist pair = newJeddictBrain().pairProgrammer(PairProgrammer.Specialist.REST);
                final Project project = FileOwnerQuery.getOwner(copy.getFileObject());

                JSONObject json = new JSONObject(
                    removeCodeBlockMarkers(
                        pair.generateEndpointForClass(leaf.toString(), globalRules(), projectRules(project))
                    )
                );
                JSONArray imports = json.getJSONArray("imports");
                String methodContent = json.getString("content");

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

}

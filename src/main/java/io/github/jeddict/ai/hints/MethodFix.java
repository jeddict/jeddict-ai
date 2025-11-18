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

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.util.TreePath;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.completion.Action;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getClassDataContent;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import static io.github.jeddict.ai.util.UIUtil.queryToEnhance;
import java.io.IOException;
import javax.lang.model.element.Element;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import io.github.jeddict.ai.agent.pair.RefactorSpecialist;

/**
 *
 * @author Shiwani Gupta
 */
public class MethodFix extends BaseAIFix {

    private String actionTitleParam;
    private String compliationError;

    public MethodFix(final TreePathHandle treePathHandle, final Action action) {
        super(treePathHandle, action);
    }

    public MethodFix(final TreePathHandle treePathHandle, final String compliationError, final String actionTitleParam) {
        super(treePathHandle, Action.COMPILATION_ERROR);
        this.compliationError = compliationError;
        this.actionTitleParam = actionTitleParam;
    }

    @Override
    protected String getText() {
        if (action == Action.COMPILATION_ERROR) {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_METHOD_COMPILATION_ERROR", actionTitleParam);
        } else if (action == Action.ENHANCE) {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_METHOD_ENHANCE");
        } else {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_METHOD_QUERY");
        }
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
        if (leaf.getKind() == METHOD) {
            final Project project = FileOwnerQuery.getOwner(copy.getFileObject());
            final RefactorSpecialist pair = newJeddictBrain().pairProgrammer(PairProgrammer.Specialist.REFACTOR);
            final String classSource = treePath.getParentPath().getLeaf().toString();
            final String methodSource = leaf.toString();

            content = switch (action) {
                case COMPILATION_ERROR -> {
                    final String classDataContent = getClassDataContent(
                        copy.getFileObject(), copy.getCompilationUnit(), pm.getClassContext()
                    );

                    LOG.finest(() -> "\nmethodSource: " + StringUtils.abbreviate(methodSource, 80)
                        + "\nclassSource: " + StringUtils.abbreviate(classSource, 80)
                        + "\nclassDataContent: " + StringUtils.abbreviate(classDataContent, 80)
                        + "\nglobalRules: " + globalRules()
                        + "\nprohectRules: " + projectRules(project)
                    );
                    yield pair.fixMethodCompilationError(
                        compliationError, classSource + "\n" + classDataContent, methodSource, globalRules(), projectRules(project)
                    );
                }
                case ENHANCE -> {
                    LOG.finest(() -> "\nmethodSource: " + StringUtils.abbreviate(methodSource, 80)
                        + "\nclassSource: " + StringUtils.abbreviate(classSource, 80)
                        + "\nglobalRules: " + globalRules()
                        + "\nprohectRules: " + projectRules(project)
                    );
                    yield pair.enhanceMethodFromMethodContent(
                        classSource, methodSource, globalRules(), projectRules(project)
                    );
                }
                default -> {
                    String query = queryToEnhance();
                    if (query == null) {
                        yield "";
                    }
                    LOG.finest(() -> "prompt: " + query
                        + "\nmethodSource: " + StringUtils.abbreviate(methodSource, 80)
                        + "\nclassSource: " + StringUtils.abbreviate(classSource, 80)
                        + "\nglobalRules: " + globalRules()
                        + "\nprohectRules: " + projectRules(project)
                    );
                    yield pair.refactor(
                        query, classSource, methodSource, globalRules(), projectRules(project)
                    );
                }
            };
        }

        if (content == null) {
            return;
        }

        JSONObject json = new JSONObject(removeCodeBlockMarkers(content));
        JSONArray imports = json.getJSONArray("imports");
        String methodContent = json.getString("content");
        SourceUtil.addImports(copy, imports);
        if (leaf instanceof MethodTree methodTree) {
            long startPos = copy.getTrees().getSourcePositions().getStartPosition(copy.getCompilationUnit(), methodTree);
            long endPos = copy.getTrees().getSourcePositions().getEndPosition(copy.getCompilationUnit(), methodTree);
            try {
                insertAndReformat(copy.getDocument(), methodContent, (int) startPos, (int) endPos - (int) startPos);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }


    private void insertAndReformat(Document document, String content, int startPosition, int lengthToRemove) {
    try {
        if (lengthToRemove > 0) {
            document.remove(startPosition, lengthToRemove);
        }
        document.insertString(startPosition, content, null);
        Reformat reformat = Reformat.get(document);
        reformat.lock();
        try {
            reformat.reformat(startPosition, startPosition + content.length());
        } finally {
            reformat.unlock();
        }
    } catch (BadLocationException ex) {
        Exceptions.printStackTrace(ex);
    }
}

}

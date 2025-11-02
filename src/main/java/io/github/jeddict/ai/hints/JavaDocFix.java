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
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.completion.Action;
import static io.github.jeddict.ai.completion.Action.ENHANCE;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.SourceUtil.geIndentaion;
import io.github.jeddict.ai.util.StringUtil;
import static io.github.jeddict.ai.util.StringUtil.removeCodeBlockMarkers;
import javax.lang.model.element.Element;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.util.NbBundle;
import io.github.jeddict.ai.agent.pair.TechWriter;

/**
 * AI-powered Javadoc generation and enhancement implementation that integrates with NetBeans' refactoring infrastructure.
 * This class provides automated Javadoc documentation services by either generating new Javadoc comments from scratch
 * or enhancing existing ones based on contextual analysis of Java elements (classes, interfaces, methods, and fields).
 *
 * <p>The implementation leverages an AI pair programmer ({@link PairProgrammer}) to generate contextually relevant
 * documentation that adheres to both global coding standards and project-specific rules. The class handles different
 * types of Java elements (determined by {@link Tree.Kind}) and supports two primary operations:
 * <ul>
 *   <li><b>Generate</b> - Creates new Javadoc comments when none exist</li>
 *   <li><b>Enhance</b> - Improves existing Javadoc comments while preserving their structure</li>
 * </ul>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Context-aware documentation generation based on element type and surrounding code</li>
 *   <li>Intelligent indentation matching to maintain code style consistency</li>
 *   <li>Seamless integration with NetBeans' WorkingCopy API for safe source modifications</li>
 *   <li>Project-specific rule application through {@link PreferencesManager}</li>
 *   <li>Automatic cleanup of existing Javadoc when enhancing documentation</li>
 *   <li>Support for internationalization through NetBeans bundle system</li>
 * </ul>
 *
 * <p>The class operates in two phases:
 * <ol>
 *   <li>Analysis: Examines the target element and any existing documentation</li>
 *   <li>Transformation: Applies the AI-generated documentation while maintaining proper source formatting</li>
 * </ol>
 *
 * <p>Usage note: This implementation automatically handles source positioning and formatting,
 * ensuring generated documentation appears in the correct location with proper indentation.
 *
 * @see BaseAIFix
 * @see PairProgrammer
 * @see PreferencesManager
 * @see JavaFix.TransformationContext
 */
public class JavaDocFix extends BaseAIFix {

    private final PreferencesManager pm = PreferencesManager.getInstance();

    private final ElementHandle classType;

    public JavaDocFix(final TreePathHandle treePathHandle, final Action action, final ElementHandle classType) {
        super(treePathHandle, action);
        this.classType = classType;
    }

    /**
     * Retrieves the hint text to be displayed based on the current action type.
     * The text varies depending on whether the action is an enhancement or a default operation.
     *
     * <p>For the {@code ENHANCE} action, this method returns a localized message
     * indicating that Javadoc has been generated for the specified class type.
     * For all other actions, it returns a generic localized message prompting
     * the user to add Javadoc for the specified class type.</p>
     *
     * @return a localized hint string corresponding to the current action,
     *         with the class type name properly capitalized for display
     *
     */
    @Override
    protected String getText() {
        switch (action) {
            case ENHANCE:
                return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_JAVADOC_GENERATED", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            default:
                return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_JAVADOC", StringUtil.convertToCapitalized(classType.getKind().toString()));//NOI18N
            }
    }

    //
    // TODO: shall this be done in a separate thread?
    //
    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {

        try (
            ProgressHandle progress = ProgressHandle.createHandle(NbBundle.getMessage(JeddictUpdateManager.class, "ProgressHandle", 0))
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

            Document document = copy.getDocument();

            String javadocContent;
            DocCommentTree oldDocCommentTree = copy.getDocTrees().getDocCommentTree(treePath);

            final TechWriter pair = newJeddictBrain().pairProgrammer(PairProgrammer.Specialist.TECHWRITER);
            final Project project = FileOwnerQuery.getOwner(copy.getFileObject());

            SwingUtilities.invokeLater(() -> progress.start());

            switch (leaf.getKind()) {
                case CLASS, INTERFACE -> {
                    if (action == ENHANCE) {
                        javadocContent = pair.enhanceClassJavadoc(
                                leaf.toString(), oldDocCommentTree.toString(),
                                globalRules(), projectRules(project)
                        );
                    } else {
                        javadocContent = pair.generateClassJavadoc(
                                leaf.toString(), globalRules(), projectRules(project)
                        );
                    }
                }
                case METHOD -> {
                    if (action == ENHANCE) {
                        javadocContent = pair.enhanceMethodJavadoc(
                            ((MethodTree) leaf).toString(), oldDocCommentTree.toString(),
                            globalRules(), projectRules(project)
                        );
                    } else {
                        javadocContent = pair.generateMethodJavadoc(
                            ((MethodTree) leaf).toString(), globalRules(), projectRules(project)
                        );
                    }
                }
                case VARIABLE -> {
                    if (action == ENHANCE) {
                        javadocContent = pair.enhanceMemberJavadoc(
                            ((VariableTree) leaf).toString(), oldDocCommentTree.toString(),
                            globalRules(), projectRules(project)
                        );
                    } else {
                        javadocContent = pair.generateMemberJavadoc(
                            ((VariableTree) leaf).toString(),
                            globalRules(), projectRules(project)
                        );
                    }
                }
                default -> {
                    progress.finish();
                    return;
                }
            }

            final StringBuilder sb = new StringBuilder("AI returned content: ")
                .append(javadocContent);
            LOG.finest(() -> sb.toString());

            javadocContent = removeCodeBlockMarkers(javadocContent);

            int startOffset = (int) copy.getTrees().getSourcePositions()
                    .getStartPosition(copy.getCompilationUnit(), leaf);

            if (document != null) {
                String lastLine = geIndentaion(copy, leaf);
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
}

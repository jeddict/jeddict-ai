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
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Kind;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;

@Hint(displayName = "DN_HINT", description = "DESC_HINT",
        id = "io.github.jeddict.ai.JeddictHint",
        category = "suggestions",
        enabled = true,
        options = Options.QUERY,
        hintKind = Kind.ACTION,
        severity = org.netbeans.spi.editor.hints.Severity.HINT)
public class JeddictHint {

    protected final WorkingCopy copy = null;
    private static final Logger LOGGER = Logger.getLogger(JeddictHint.class.getName());

    private static final PreferencesManager prefsManager = PreferencesManager.getInstance();

    public JeddictHint() {
    }

    @TriggerTreeKind({Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.METHOD, Tree.Kind.VARIABLE,
        Tree.Kind.BLOCK,
        Tree.Kind.STRING_LITERAL,
        Tree.Kind.EXPRESSION_STATEMENT,
        Tree.Kind.EMPTY_STATEMENT})
    public static ErrorDescription run(HintContext ctx) {

        if (!prefsManager.isAiAssistantActivated()) {
            return null;
        }
        if (!prefsManager.isHintsEnabled()) {
            return null;
        }

        CompilationInfo compilationInfo = ctx.getInfo();
        TreePath treePath = ctx.getPath();
        if (treePath == null) {
            return null;
        }

        List<Fix> fixes = new ArrayList<>(); // Define fixes as List
        TreePathHandle tpHandle = TreePathHandle.create(treePath, compilationInfo);

        Tree.Kind kind = treePath.getLeaf().getKind();
        DocCommentTree oldDocCommentTree = ((DocTrees) compilationInfo.getTrees()).getDocCommentTree(treePath);

        switch (kind) {
            case CLASS:
            case INTERFACE:
                TypeElement type = (TypeElement) compilationInfo.getTrees().getElement(treePath);
                ElementHandle<TypeElement> elementHandle = ElementHandle.create(type);
                fixes.add(new JavaDocFixImpl(tpHandle, Action.CREATE, elementHandle).toEditorFix());
                if (oldDocCommentTree != null) {
                    fixes.add(new JavaDocFixImpl(tpHandle, Action.ENHANCE, elementHandle).toEditorFix());
                }
                if (type.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals("jakarta.ws.rs.Path"))) {
                    fixes.add(new RestEndpointFix(tpHandle, Action.CREATE, elementHandle).toEditorFix());
                }
                fixes.add(new AssistantChatManager(tpHandle, Action.LEARN, treePath).toEditorFix());
                fixes.add(new AssistantChatManager(tpHandle, Action.TEST, treePath).toEditorFix());
                break;
//            case IDENTIFIER:
//            case BLOCK:
            case STRING_LITERAL:
                if (treePath.getLeaf() != null) {
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes.add(new TextFix(tpHandle, Action.CREATE, treePath).toEditorFix());
                    fixes.add(new TextFix(tpHandle, Action.ENHANCE, treePath).toEditorFix());
                }
                break;
//            case EMPTY_STATEMENT:
            case EXPRESSION_STATEMENT:
                if (treePath.getLeaf() != null) {
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes.add(new ExpressionFix(tpHandle, Action.ENHANCE, treePath).toEditorFix());
                }
                break;
            case METHOD:
                Element methodElement = compilationInfo.getTrees().getElement(treePath);
                if (methodElement != null && methodElement.getKind() == ElementKind.METHOD) {
                    ElementHandle<ExecutableElement> methodHandle = ElementHandle.create((ExecutableElement) methodElement);
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes.add(new JavaDocFixImpl(tpHandle, Action.CREATE, methodHandle).toEditorFix());
                    if (oldDocCommentTree != null) {
                        fixes.add(new JavaDocFixImpl(tpHandle, Action.ENHANCE, methodHandle).toEditorFix());
                    }
                    fixes.add(new MethodFix(tpHandle, Action.CREATE, methodHandle).toEditorFix());
                    fixes.add(new MethodFix(tpHandle, Action.ENHANCE, methodHandle).toEditorFix());
                    for (Diagnostic<?> d : ctx.getInfo().getDiagnostics()) {
                        if (isDiagnosticRelatedToMethod(d, compilationInfo, treePath.getLeaf())) {
                            if (d.getKind() == Diagnostic.Kind.ERROR) {
                                fixes.add(new MethodFix(tpHandle,
                                        (d.getMessage(Locale.getDefault()) + '\n' + d.toString()),
                                        "Line:" + d.getLineNumber() + " " + d.getMessage(Locale.getDefault())).toEditorFix());
                            } else {
                                fixes.add(new MethodFix(tpHandle,
                                        (d.getMessage(Locale.getDefault()) + '\n' + d.toString()),
                                        "Line:" + d.getLineNumber() + " WARNING: " + d.getMessage(Locale.getDefault())).toEditorFix());
                            }
                        }
                    }
                    fixes.add(new AssistantChatManager(tpHandle, Action.LEARN, treePath).toEditorFix());
                    fixes.add(new AssistantChatManager(tpHandle, Action.TEST, treePath).toEditorFix());
                }
                break;
            case VARIABLE:
                Element fieldElement = compilationInfo.getTrees().getElement(treePath);
                if (fieldElement != null && fieldElement.getKind() == ElementKind.FIELD) {
                    ElementHandle<VariableElement> fieldHandle = ElementHandle.create((VariableElement) fieldElement);
                    tpHandle = TreePathHandle.create(treePath, compilationInfo);
                    fixes.add(new JavaDocFixImpl(tpHandle, Action.CREATE, fieldHandle).toEditorFix());
                    if (oldDocCommentTree != null) {
                        fixes.add(new JavaDocFixImpl(tpHandle, Action.ENHANCE, fieldHandle).toEditorFix());
                    }
                }
                fixes.add(new VariableNameFix(tpHandle, Action.ENHANCE, treePath).toEditorFix());
                for (Diagnostic<?> d : ctx.getInfo().getDiagnostics()) {
                    if (isDiagnosticRelatedToVariable(d, compilationInfo, treePath.getLeaf())) {
                        if (d.getKind() == Diagnostic.Kind.ERROR) {
                            fixes.add(new VariableFix(tpHandle,
                                    (d.getMessage(Locale.getDefault()) + '\n' + d.toString()),
                                    "Line:" + d.getLineNumber() + " " + d.getMessage(Locale.getDefault())).toEditorFix());
                        } else {
                            fixes.add(new VariableFix(tpHandle,
                                    (d.getMessage(Locale.getDefault()) + '\n' + d.toString()),
                                    "Line:" + d.getLineNumber() + " WARNING: " + d.getMessage(Locale.getDefault())).toEditorFix());
                        }
                    }
                }
                break;
            default:
                return null;
        }
        Fix[] fixesArray = fixes.toArray(new Fix[0]); // Convert to array
        String desc = NbBundle.getMessage(JeddictUpdateManager.class, "ERR_HINT"); //NOI18N
        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), desc, fixesArray); //NOI18N
    }

    private static boolean isDiagnosticRelatedToMethod(Diagnostic<?> diagnostic, CompilationInfo compilationInfo, Tree methodElement) {
        long startPosition = compilationInfo.getTrees().getSourcePositions().getStartPosition(compilationInfo.getCompilationUnit(), methodElement);
        long endPosition = compilationInfo.getTrees().getSourcePositions().getEndPosition(compilationInfo.getCompilationUnit(), methodElement);
        long diagnosticPosition = diagnostic.getPosition();
        return diagnosticPosition >= startPosition && diagnosticPosition <= endPosition;
    }

    public static boolean isDiagnosticRelatedToVariable(Diagnostic<?> diagnostic, CompilationInfo compilationInfo, Tree variableElement) {
        // Ensure the variableElement is of the correct kind (e.g., VARIABLE)
        if (variableElement == null || !(variableElement instanceof VariableTree)) {
            return false;
        }

        // Get the variable's position range
        Trees trees = compilationInfo.getTrees();
        SourcePositions sourcePositions = trees.getSourcePositions();
        long startPosition = sourcePositions.getStartPosition(compilationInfo.getCompilationUnit(), variableElement);
        long endPosition = sourcePositions.getEndPosition(compilationInfo.getCompilationUnit(), variableElement);

        // Get the diagnostic's position
        long diagnosticPosition = diagnostic.getPosition();

        // Check if the diagnostic's position is within the variable's position range
        return diagnosticPosition >= startPosition && diagnosticPosition <= endPosition;
    }

//    @TriggerPatterns({
////        @TriggerPattern("$mods$ $type $var = $init"), //NOI18N
//        @TriggerPattern("$mods$ $type $var"), //NOI18N
//    })
}

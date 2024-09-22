package io.github.jeddict.ai.scanner;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.source.tree.ExportsTree;
import com.sun.source.util.DocTrees;

public class MyTreePathScanner extends TreePathScanner<TreePath, Void> {

    private final Trees trees;
    private final DocTrees docTrees;
    private final int caretOffset;
    private final CompilationUnitTree compilationUnit;
    private TreePath targetPath;

    public MyTreePathScanner(Trees trees, DocTrees docTrees, int caretOffset, CompilationUnitTree compilationUnit) {
        this.trees = trees;
        this.docTrees = docTrees;
        this.caretOffset = caretOffset;
        this.compilationUnit = compilationUnit;
    }

    private void checkPosition(Tree node) {
        if (node != null && (trees.getSourcePositions().getStartPosition(compilationUnit, node) <= caretOffset
                && caretOffset <= trees.getSourcePositions().getEndPosition(compilationUnit, node))) {
            targetPath = getCurrentPath();
        }
    }

    @Override
    public TreePath visitAnnotatedType(AnnotatedTypeTree node, Void p) {
        checkPosition(node);
        return super.visitAnnotatedType(node, p);
    }

    @Override
    public TreePath visitAnnotation(AnnotationTree node, Void p) {
        checkPosition(node);
        return super.visitAnnotation(node, p);
    }

    @Override
    public TreePath visitMethodInvocation(MethodInvocationTree node, Void p) {
        checkPosition(node);
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public TreePath visitAssert(AssertTree node, Void p) {
        checkPosition(node);
        return super.visitAssert(node, p);
    }

    @Override
    public TreePath visitAssignment(AssignmentTree node, Void p) {
        checkPosition(node);
        return super.visitAssignment(node, p);
    }

    @Override
    public TreePath visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        checkPosition(node);
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public TreePath visitBinary(BinaryTree node, Void p) {
        checkPosition(node);
        return super.visitBinary(node, p);
    }

    @Override
    public TreePath visitBlock(BlockTree node, Void p) {
        checkPosition(node);
        return super.visitBlock(node, p);
    }

    @Override
    public TreePath visitBreak(BreakTree node, Void p) {
        checkPosition(node);
        return super.visitBreak(node, p);
    }

    @Override
    public TreePath visitCase(CaseTree node, Void p) {
        checkPosition(node);
        return super.visitCase(node, p);
    }

    @Override
    public TreePath visitCatch(CatchTree node, Void p) {
        checkPosition(node);
        return super.visitCatch(node, p);
    }

    @Override
    public TreePath visitClass(ClassTree node, Void p) {
        checkPosition(node);
        checkDocComment();
        return super.visitClass(node, p);
    }

    @Override
    public TreePath visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        checkPosition(node);
        return super.visitConditionalExpression(node, p);
    }

    @Override
    public TreePath visitContinue(ContinueTree node, Void p) {
        checkPosition(node);
        return super.visitContinue(node, p);
    }

    @Override
    public TreePath visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        checkPosition(node);
        return super.visitDoWhileLoop(node, p);
    }

    @Override
    public TreePath visitErroneous(ErroneousTree node, Void p) {
        checkPosition(node);
        return super.visitErroneous(node, p);
    }

    @Override
    public TreePath visitExpressionStatement(ExpressionStatementTree node, Void p) {
        checkPosition(node);
        return super.visitExpressionStatement(node, p);
    }

    @Override
    public TreePath visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        checkPosition(node);
        return super.visitEnhancedForLoop(node, p);
    }

    @Override
    public TreePath visitForLoop(ForLoopTree node, Void p) {
        checkPosition(node);
        return super.visitForLoop(node, p);
    }

    @Override
    public TreePath visitIdentifier(IdentifierTree node, Void p) {
        checkPosition(node);
        return super.visitIdentifier(node, p);
    }

    @Override
    public TreePath visitIf(IfTree node, Void p) {
        checkPosition(node);
        return super.visitIf(node, p);
    }

    @Override
    public TreePath visitImport(ImportTree node, Void p) {
        checkPosition(node);
        return super.visitImport(node, p);
    }

    @Override
    public TreePath visitArrayAccess(ArrayAccessTree node, Void p) {
        checkPosition(node);
        return super.visitArrayAccess(node, p);
    }

    @Override
    public TreePath visitLabeledStatement(LabeledStatementTree node, Void p) {
        checkPosition(node);
        return super.visitLabeledStatement(node, p);
    }

    @Override
    public TreePath visitLiteral(LiteralTree node, Void p) {
        checkPosition(node);
        return super.visitLiteral(node, p);
    }

    @Override
    public TreePath visitMethod(MethodTree node, Void p) {
        checkPosition(node);
        checkDocComment();
        return super.visitMethod(node, p);
    }

    @Override
    public TreePath visitModifiers(ModifiersTree node, Void p) {
        checkPosition(node);
        return super.visitModifiers(node, p);
    }

    @Override
    public TreePath visitNewArray(NewArrayTree node, Void p) {
        checkPosition(node);
        return super.visitNewArray(node, p);
    }

    @Override
    public TreePath visitNewClass(NewClassTree node, Void p) {
        checkPosition(node);
        return super.visitNewClass(node, p);
    }

    @Override
    public TreePath visitLambdaExpression(LambdaExpressionTree node, Void p) {
        checkPosition(node);
        return super.visitLambdaExpression(node, p);
    }

    @Override
    public TreePath visitPackage(PackageTree node, Void p) {
        checkPosition(node);
        return super.visitPackage(node, p);
    }

    @Override
    public TreePath visitParenthesized(ParenthesizedTree node, Void p) {
        checkPosition(node);
        return super.visitParenthesized(node, p);
    }

    @Override
    public TreePath visitReturn(ReturnTree node, Void p) {
        checkPosition(node);
        return super.visitReturn(node, p);
    }

    @Override
    public TreePath visitMemberSelect(MemberSelectTree node, Void p) {
        checkPosition(node);
        return super.visitMemberSelect(node, p);
    }

    @Override
    public TreePath visitMemberReference(MemberReferenceTree node, Void p) {
        checkPosition(node);
        return super.visitMemberReference(node, p);
    }

    @Override
    public TreePath visitEmptyStatement(EmptyStatementTree node, Void p) {
        checkPosition(node);
        return super.visitEmptyStatement(node, p);
    }

    @Override
    public TreePath visitSwitch(SwitchTree node, Void p) {
        checkPosition(node);
        return super.visitSwitch(node, p);
    }

    @Override
    public TreePath visitSynchronized(SynchronizedTree node, Void p) {
        checkPosition(node);
        return super.visitSynchronized(node, p);
    }

    @Override
    public TreePath visitThrow(ThrowTree node, Void p) {
        checkPosition(node);
        return super.visitThrow(node, p);
    }

    @Override
    public TreePath visitCompilationUnit(CompilationUnitTree node, Void p) {
        checkPosition(node);
//        checkComments(node);
        return super.visitCompilationUnit(node, p);
    }

    @Override
    public TreePath visitTry(TryTree node, Void p) {
        checkPosition(node);
        return super.visitTry(node, p);
    }

    @Override
    public TreePath visitParameterizedType(ParameterizedTypeTree node, Void p) {
        checkPosition(node);
        return super.visitParameterizedType(node, p);
    }

    @Override
    public TreePath visitUnionType(UnionTypeTree node, Void p) {
        checkPosition(node);
        return super.visitUnionType(node, p);
    }

    @Override
    public TreePath visitIntersectionType(IntersectionTypeTree node, Void p) {
        checkPosition(node);
        return super.visitIntersectionType(node, p);
    }

    @Override
    public TreePath visitArrayType(ArrayTypeTree node, Void p) {
        checkPosition(node);
        return super.visitArrayType(node, p);
    }

    @Override
    public TreePath visitTypeCast(TypeCastTree node, Void p) {
        checkPosition(node);
        return super.visitTypeCast(node, p);
    }

    @Override
    public TreePath visitPrimitiveType(PrimitiveTypeTree node, Void p) {
        checkPosition(node);
        return super.visitPrimitiveType(node, p);
    }

    @Override
    public TreePath visitTypeParameter(TypeParameterTree node, Void p) {
        checkPosition(node);
        return super.visitTypeParameter(node, p);
    }

    @Override
    public TreePath visitInstanceOf(InstanceOfTree node, Void p) {
        checkPosition(node);
        return super.visitInstanceOf(node, p);
    }

    @Override
    public TreePath visitUnary(UnaryTree node, Void p) {
        checkPosition(node);
        return super.visitUnary(node, p);
    }

    @Override
    public TreePath visitWhileLoop(WhileLoopTree node, Void p) {
        checkPosition(node);
        return super.visitWhileLoop(node, p);
    }

//    @Override
//    public TreePath visitWithers(WithersTree node, Void p) {
//        checkPosition(node);
//        return super.visitWithers(node, p);
//    }
    @Override
    public TreePath visitModule(ModuleTree node, Void p) {
        checkPosition(node);
        return super.visitModule(node, p);
    }

    @Override
    public TreePath visitExports(ExportsTree node, Void p) {
        checkPosition(node);
        return super.visitExports(node, p);
    }

    @Override
    public TreePath visitRequires(RequiresTree node, Void p) {
        checkPosition(node);
        return super.visitRequires(node, p);
    }

    @Override
    public TreePath visitUses(UsesTree node, Void p) {
        checkPosition(node);
        return super.visitUses(node, p);
    }

    @Override
    public TreePath visitProvides(ProvidesTree node, Void p) {
        checkPosition(node);
        return super.visitProvides(node, p);
    }

    @Override
    public TreePath visitVariable(VariableTree node, Void p) {
        checkPosition(node);
        checkDocComment();
        return super.visitVariable(node, p);
    }

    private void checkDocComment() {
        DocCommentTree docCommentTree = docTrees.getDocCommentTree(getCurrentPath());
        if (docCommentTree != null) {
            long startPos = docTrees.getSourcePositions().getStartPosition(compilationUnit, getCurrentPath().getLeaf());
            long endPos = docTrees.getSourcePositions().getEndPosition(compilationUnit, getCurrentPath().getLeaf());

            if (startPos <= caretOffset && caretOffset <= endPos) {
                targetPath = getCurrentPath();
            }
        }
    }

//    /**
//     * Checks and processes line and block comments for the given compilation unit.
//     */
//    private void checkComments(CompilationUnitTree node) {
//        for (Comment comment : trees.getSourcePositions().getComments(node)) {
//            long startPos = comment.getSourcePos(0);
//            long endPos = startPos + comment.getText().length();
//
//            if (startPos <= caretOffset && caretOffset <= endPos) {
//                // Set target path to the comment
//                targetPath = getCurrentPath();
//            }
//        }
//    }
    public TreePath getTargetPath() {
        return targetPath;
    }
}

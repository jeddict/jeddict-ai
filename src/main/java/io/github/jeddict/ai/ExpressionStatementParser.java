/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import java.util.Arrays;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import org.netbeans.spi.editor.hints.Context;

public class ExpressionStatementParser {


    public static ExpressionStatementTree parseExpressionStatement(String content) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JavaCompiler not available. Ensure you're running this in a JDK environment.");
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, 
                Arrays.asList(new JavaSourceFromString("temp", content)));

        try {
            Iterable<? extends CompilationUnitTree> units = task.parse();
//            JavacTrees trees = JavacTrees.instance(task);
            for (CompilationUnitTree unit : units) {
                for (Tree t : unit.getTypeDecls()) {
                    if (t instanceof ClassTree) {
                        ClassTree classTree = (ClassTree) t;
                        for (Tree member : classTree.getMembers()) {
                            if (member instanceof MethodTree) {
                                MethodTree methodTree = (MethodTree) member;
                                for (StatementTree stmt : methodTree.getBody().getStatements()) {
                                    if (stmt instanceof ExpressionStatementTree) {
                                        ExpressionStatementTree exprStmt = (ExpressionStatementTree) stmt;
                                        // Check if the statement content matches and return
                                        if (exprStmt.toString().equals(content)) {
                                            return exprStmt;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Handle cases where parsing fails or content is not found
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(java.net.URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}

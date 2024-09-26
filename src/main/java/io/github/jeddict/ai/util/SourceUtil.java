/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.jeddict.ai.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.text.BadLocationException;
import org.json.JSONArray;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Gaurav Gupta
 */
public class SourceUtil {

    public static void fixImports(FileObject fileObject) {
        // TODO
    }

    public static void printSource(FileObject fileObject) {
        if (fileObject != null && "java".equals(fileObject.getExt())) {
            try (InputStream is = fileObject.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
        }
    }

    public static void addImports(WorkingCopy copy, JSONArray imports) {
        CompilationUnitTree compilationUnit = copy.getCompilationUnit();
        TreeMaker make = copy.getTreeMaker();
        CompilationUnitTree newcompilationUnit = compilationUnit;
        for (int i = 0; i < imports.length(); i++) {
            String importToAdd = imports.getString(i);
            if (importToAdd.startsWith("import ") && importToAdd.endsWith(";")) {
                importToAdd = importToAdd.substring(7, importToAdd.length() - 1).trim();  // Extract the class name
            }
            if (!isImportPresent(copy, compilationUnit, importToAdd)) {
                ImportTree newImport = make.Import(make.QualIdent(importToAdd), false);
                newcompilationUnit = make.addCompUnitImport(newcompilationUnit, newImport);
            }
        }
        copy.rewrite(compilationUnit, newcompilationUnit);
    }

    public static void addImports(WorkingCopy copy, List<String> imports) {
        CompilationUnitTree compilationUnit = copy.getCompilationUnit();
        TreeMaker make = copy.getTreeMaker();
        CompilationUnitTree newcompilationUnit = compilationUnit;
        for (int i = 0; i < imports.size(); i++) {
            String importToAdd = imports.get(i);
            if (importToAdd.startsWith("import ") && importToAdd.endsWith(";")) {
                importToAdd = importToAdd.substring(7, importToAdd.length() - 1).trim();  // Extract the class name
            }
            if (!isImportPresent(copy, compilationUnit, importToAdd)) {
                ImportTree newImport = make.Import(make.QualIdent(importToAdd), false);
                newcompilationUnit = make.addCompUnitImport(newcompilationUnit, newImport);
            }
        }
        copy.rewrite(compilationUnit, newcompilationUnit);
    }

    private static boolean isImportPresent(WorkingCopy copy, CompilationUnitTree compilationUnit, String importName) {
        for (ImportTree importTree : compilationUnit.getImports()) {
            if (importTree.getQualifiedIdentifier().toString().equals(importName)) {
                return true; // Import already exists
            }
        }
        return false;
    }

    public static void reformatClass(WorkingCopy copy, ClassTree classTree) throws Exception {
        // NetBeans does formatting on save or during 'fix imports', but you can also trigger formatting like this:
        copy.toPhase(JavaSource.Phase.UP_TO_DATE); // Move to the UP_TO_DATE phase

        // Create a rewriter for formatting
        copy.rewrite(classTree, classTree);  // This rewrites the class and lets NetBeans apply default formatting

        // Alternatively, save changes and let the IDE apply auto-format on save if configured
    }
    
    public static String geIndentaion(WorkingCopy copy, Tree leaf) {
        try {
            if (copy.getDocument() != null) {
                String sourceCode = copy.getDocument().getText(0, copy.getDocument().getEndPosition().getOffset());
                int startPos = (int) copy.getTrees().getSourcePositions().getStartPosition(copy.getCompilationUnit(), leaf);
                String[] lines = sourceCode.substring(0, startPos).split("\n"); // Zero-based index
                String lastLine = lines[lines.length - 1];
                return lastLine;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return "";
    }
}

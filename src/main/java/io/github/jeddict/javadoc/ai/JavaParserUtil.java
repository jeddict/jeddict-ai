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
package io.github.jeddict.javadoc.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.sun.source.tree.MethodTree;
import org.json.JSONArray;

/**
 *
 * @author Gaurav Gupta
 */
public class JavaParserUtil {
    
    public static void addImports(CompilationUnit cu, JSONArray imports) {
        for (int i = 0; i < imports.length(); i++) {
            String imp = imports.getString(i).trim();  // Trim any extra spaces

            if (!imp.isEmpty()) {
                String className;
                if (imp.startsWith("import ") && imp.endsWith(";")) {
                    className = imp.substring(7, imp.length() - 1).trim();  // Extract the class name
                } else {
                    className = imp;
                }

                boolean alreadyImported = cu.getImports().stream()
                        .anyMatch(existingImport -> existingImport.getNameAsString().equals(className));
                if (!alreadyImported) {
                    cu.addImport(className);
                }
            }
        }
    }

    public static void addMethods(CompilationUnit cu, JSONArray methods, JavaParser javaParser) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
            for (int i = 0; i < methods.length(); i++) {
                String methodCode = methods.getString(i);
                if (!methodCode.trim().isEmpty()) {
                    MethodDeclaration methodDeclaration = javaParser.parseMethodDeclaration(methodCode.trim()).getResult().orElse(null);
                    if (methodDeclaration != null) {
                        classDecl.addMember(methodDeclaration);
                    }
                }
            }
        });
    }
    
public static void updateMethods(CompilationUnit cu, MethodTree methodTree, JSONArray imports, String javaSnippet, JavaParser javaParser) { 
    String methodName = methodTree.getName().toString();
    
    cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
        // Find and remove the original method by its name
        classDecl.findFirst(MethodDeclaration.class, method -> method.getNameAsString().equals(methodName))
                .ifPresent(classDecl::remove);
        StringBuilder importsBuilder = new StringBuilder();
        for (int i = 0; i < imports.length(); i++) {
            String importStatement = imports.getString(i);
            if (importStatement.startsWith("import")) {
                importsBuilder.append(importStatement);
            } else {
                importsBuilder.append("import ").append(importStatement).append(";\n");
            }
        }
        String wrappedSnippet = importsBuilder.toString() // Appending the imports
                + "public class TempClass {\n" + javaSnippet + "\n}";
        javaParser.parse(wrappedSnippet).getResult().ifPresent(parsed -> {
            // Iterate through all types in the parsed snippet
            parsed.getTypes().forEach(type -> {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration tempClass = (ClassOrInterfaceDeclaration) type;

                    // Add each field (variable) found in the parsed snippet
                    tempClass.getFields().forEach(newField -> {
                        classDecl.addMember(newField);
                    });

                    // Add each method found in the parsed snippet
                    tempClass.getMethods().forEach(newMethod -> {
                        classDecl.addMember(newMethod);
                    });
                }
            });
        });
    });
}

}

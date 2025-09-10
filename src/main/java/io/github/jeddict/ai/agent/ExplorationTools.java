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
package io.github.jeddict.ai.agent;

import org.netbeans.api.java.source.*;
import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.lang.JeddictStreamHandler;
import io.github.jeddict.ai.util.FileUtil;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Set;

import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.modules.refactoring.api.RefactoringSession;
import org.openide.util.lookup.Lookups;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;

/**
 * Tools for code-level operations in NetBeans projects.
 *
 * <p>
 * This class offers various methods for AI assistants to explore and analyze Java source code
 * within a NetBeans project. It uses NetBeans JavaSource and Refactoring APIs to facilitate
 * operations such as listing classes and methods, searching for symbols, and finding usages.
 * These tools can be integrated into AI workflows to enhance automated code understanding,
 * navigation, and refactoring assistance.
 * </p>
 *
 * <p>Key functional capabilities include:</p>
 * <ul>
 *   <li>Listing all top-level classes declared in a Java file.</li>
 *   <li>Listing all method signatures declared in a Java file.</li>
 *   <li>Searching the entire project for a symbol by name (class, method, or field).</li>
 *   <li>Finding all usages of a specified class, method, or field within the codebase.</li>
 * </ul>
 *
 * <p><b>Example usage by an AI assistant:</b></p>
 * <pre>
 * ExplorationTools tools = new ExplorationTools(project, handler);
 * String classes = tools.listClassesInFile("src/main/java/com/example/MyClass.java");
 * System.out.println(classes); // Output: "Class: com.example.MyClass"
 * </pre>
 *
 * @author Gaurav Gupta
 */

public class ExplorationTools {

    private final Project project;
    private final JeddictStreamHandler handler;

    public ExplorationTools(Project project, JeddictStreamHandler handler) {
        this.project = project;
        this.handler = handler;
    }

    /**
     * List all top-level classes in a file.
     *
     * <p>
     * <b>Example:</b></p>
     * <pre>
     * listClassesInFile("src/main/java/com/example/MyClass.java");
     * // -> "Class: com.example.MyClass"
     * </pre>
     *
     * @param path relative path to the Java file
     * @return names of all top-level classes, or a message if none found
     */
    @Tool("List all classes declared in a given Java file by path")
    public String listClassesInFile(String path) {
        log("Listing classes in", path);
        return FileUtil.withJavaSource(project, path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                List<TypeElement> classes = ElementFilter.typesIn(cc.getTopLevelElements());
                for (TypeElement clazz : classes) {
                    result.append("Class: ").append(clazz.getQualifiedName()).append("\n");
                }
            }, true);
            return result.toString();
        }, false).toString().replace("File not found", "No classes found").replace("Not a Java source file", "No classes found");
    }

    /**
     * List all methods inside a class file.
     *
     * <p>
     * <b>Example:</b></p>
     * <pre>
     * listMethodsInFile("src/main/java/com/example/MyClass.java");
     * // -> "Method: public void sayHello()"
     * </pre>
     *
     * @param path relative path to the Java file
     * @return method signatures, or a message if none found
     */
    @Tool("List all methods of a class in a given Java file by path")
    public String listMethodsInFile(String path) {
        log("Listing methods in", path);
        return FileUtil.withJavaSource(project, path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (Element element : cc.getTopLevelElements()) {
                    List<? extends Element> enclosed = element.getEnclosedElements();
                    enclosed.stream()
                            .filter(e -> e.getKind().isExecutable())
                            .forEach(m -> result.append("Method: ").append(m.toString()).append("\n"));
                }
            }, true);
            return result.toString();
        }, false).toString().replace("File not found", "No methods found").replace("Not a Java source file", "No methods found");
    }

    /**
     * Search for a symbol (class, method, or field) in the whole project.
     *
     * <p>
     * <b>Example:</b></p>
     * <pre>
     * searchSymbol("UserService");
     * // -> "Found in: src/main/java/com/example/service/UserService.java"
     * </pre>
     */
    @Tool("Search for a symbol (class, method, or field) in the whole project")
    public String searchSymbol(String symbolName) {
        log("Searching symbol", symbolName);

        StringBuilder result = new StringBuilder();
        try {
            Sources sources = project.getLookup().lookup(Sources.class);
            if (sources == null) {
                return "No sources found in project.";
            }

            for (SourceGroup sg : sources.getSourceGroups(Sources.TYPE_GENERIC)) {
                JavaSource javaSource = JavaSource.forFileObject(sg.getRootFolder());
                if (javaSource == null) {
                    continue;
                }

                javaSource.runUserActionTask(cc -> {
                    cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    ClassIndex idx = cc.getClasspathInfo().getClassIndex();
                    Set<ElementHandle<TypeElement>> handles
                            = idx.getDeclaredTypes(symbolName, ClassIndex.NameKind.SIMPLE_NAME,
                                    Set.of(ClassIndex.SearchScope.SOURCE));

                    for (ElementHandle<TypeElement> h : handles) {
                        result.append("Found: ").append(h.getQualifiedName()).append("\n");
                    }
                }, true);
            }
        } catch (IOException e) {
            return "Symbol search failed: " + e.getMessage();
        }

        return result.length() == 0 ? "No matches found." : result.toString();
    }

    @Tool("Find all usages of a class, method, or field")
    public String findUsages(String path, String symbolName) {
        String jr = FileUtil.withJavaSource(project, path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            try {
                javaSource.runUserActionTask(cc -> {
                    cc.toPhase(Phase.ELEMENTS_RESOLVED);
                    for (TypeElement type : ElementFilter.typesIn(cc.getTopLevelElements())) {
                        for (Element member : type.getEnclosedElements()) {
                            if (member.getSimpleName().toString().equals(symbolName)
                                    || type.getSimpleName().toString().equals(symbolName)) {

                                ElementHandle<Element> handle = ElementHandle.create(member);
                                org.netbeans.modules.refactoring.api.WhereUsedQuery query
                                        = new org.netbeans.modules.refactoring.api.WhereUsedQuery(Lookups.singleton(handle));

                                RefactoringSession session = RefactoringSession.create("Find Usages");
                                query.prepare(session);
                                session.doRefactoring(true);

                                session.getRefactoringElements().forEach(elem
                                        -> result.append("Usage: ").append(elem.getDisplayText()).append("\n")
                                );
                            }
                        }
                    }
                }, true);
            } catch (IOException e) {
                return "Find usages failed: " + e.getMessage();
            }
            return result.length() == 0 ? "No usages found." : result.toString();
        }, false).toString();
        return jr.replace("File not found", "No usages found").replace("Not a Java source file", "No usages found");
    }
    
    private void log(String action, String message) {
        if (handler != null) {
            handler.onToolingResponse(action + " " + message + "\n");
        }
    }
}

/**
 * Copyright 2025-26 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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

import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.refactoring.api.RefactoringSession;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Tools for code-level operations in NetBeans Java projects only.
 *
 * <p>
 * This class offers various methods for AI assistants to explore and analyze
 * Java source code within a NetBeans project. It uses NetBeans JavaSource and
 * Refactoring APIs to facilitate operations such as listing classes and
 * methods, searching for symbols, and finding usages. These tools can be
 * integrated into AI workflows to enhance automated code understanding,
 * navigation, and refactoring assistance.
 * </p>
 *
 * <p>
 * Key functional capabilities include:</p>
 * <ul>
 * <li>Listing all top-level classes declared in a Java file.</li>
 * <li>Listing all method signatures declared in a Java file.</li>
 * <li>Searching the entire project for a symbol by name (class, method, or
 * field).</li>
 * <li>Finding all usages of a specified class, method, or field within the
 * codebase.</li>
 * </ul>
 *
 * <p>
 * <b>Example usage by an AI assistant:</b></p>
 * <pre>
 * ExplorationTools tools = new ExplorationTools(project, handler);
 * String classes = tools.listClassesInFile("src/main/java/com/example/MyClass.java");
 * System.out.println(classes); // Output: "Class: com.example.MyClass"
 * </pre>
 *
 * @author Gaurav Gupta
 */
public class ExplorationTools extends AbstractCodeTool {

    private final Lookup lookup;

    public ExplorationTools(final String basedir, Lookup lookup) {
        super(basedir);
        this.lookup = lookup;
    }

    private static boolean isJavaFile(String path) {
        return path != null && path.endsWith(".java");
    }

    /**
     * List all top-level classes in a Java source file.
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
    @Tool("JAVA ONLY: List all classes declared in a given Java file by path")
    public String listClassesInFile(String path) throws Exception {

        if (!isJavaFile(path)) {
            return "This tool supports Java source files only (.java).";
        }

        progress("Listing classes in " + path);

        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                List<TypeElement> classes
                        = ElementFilter.typesIn(cc.getTopLevelElements());
                for (TypeElement clazz : classes) {
                    result.append("Class: ")
                            .append(clazz.getQualifiedName())
                            .append("\n");
                }
            }, true);
            return result.toString();
        }, false).toString();
    }

    /**
     * List all methods and constructors in a Java class file.
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
    @Tool("JAVA ONLY: List all methods of a class in a given Java file by path")
    public String listMethodsInFile(String path) throws Exception {

        if (!isJavaFile(path)) {
            return "This tool supports Java source files only (.java).";
        }

        progress("Listing methods in " + path);

        return withJavaSource(path, javaSource -> {
            final StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (Element element : cc.getTopLevelElements()) {
                    element.getEnclosedElements().stream()
                            .filter(e -> e.getKind() == ElementKind.METHOD
                            || e.getKind() == ElementKind.CONSTRUCTOR)
                            .forEach(m
                                    -> result.append("Method: ")
                                    .append(m)
                                    .append("\n")
                            );
                }
            }, true);
            return result.toString();
        }, false).toString();
    }

    /**
     * Searches the project for a Java symbol (class, method, or field).
     *
     * <p>
     * <b>How the search works:</b></p>
     * <ol>
     * <li>Obtains all Java source groups from the NetBeans project.</li>
     * <li>Builds a {@link ClassIndex} from the project's Java classpath.</li>
     * <li>Retrieves all declared Java types (classes and interfaces) in source
     * scope.</li>
     * <li>For each type:
     * <ul>
     * <li>Resolves the type using {@link JavaSource}.</li>
     * <li>Checks whether the type's simple name matches the requested symbol
     * (class match).</li>
     * <li>Iterates over the type's members to check for matching methods,
     * constructors, or fields.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * <p>
     * The search is performed using the symbol's <b>simple name</b> only. Fully
     * qualified names are not required and are not matched directly.</p>
     *
     * <p>
     * <b>Limitations:</b></p>
     * <ul>
     * <li>Only Java source files are searched (no binaries or
     * dependencies).</li>
     * <li>Inner and anonymous classes are not indexed.</li>
     * <li>Overloaded methods are returned by name only, without signature
     * differentiation.</li>
     * </ul>
     * 
     * <p>
     * <b>Examples:</b></p>
     * <pre>
     * searchSymbol("UserService");   // Class: com.example.service.UserService
     * searchSymbol("findUser");      // Method: com.example.service.UserService.findUser
     * searchSymbol("userRepository");// Field: com.example.service.UserService.userRepository
     * </pre>
     *
     * @param symbolName simple name of the Java class, method, or field to
     * search for
     * @return matching symbols found in the Java source code
     */
    @Tool("JAVA ONLY: Search for a symbol (class, method, or field) in the whole project")
    public String searchSymbol(String symbolName) throws Exception {

        progress("Searching symbol " + symbolName);

        Sources sources = lookup.lookup(Sources.class);
        if (sources == null) {
            return "No Java sources found in project.";
        }

        SourceGroup[] groups
                = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);

        if (groups.length == 0) {
            progress("No sources found in project.");
            return "No sources found in project.";
        }

        ClasspathInfo cpInfo
                = ClasspathInfo.create(groups[0].getRootFolder());
        ClassIndex index = cpInfo.getClassIndex();

        StringBuilder result = new StringBuilder();

        Set<ElementHandle<TypeElement>> types
                = index.getDeclaredTypes(
                        "",
                        ClassIndex.NameKind.PREFIX,
                        Set.of(ClassIndex.SearchScope.SOURCE)
                );

        for (ElementHandle<TypeElement> h : types) {

            FileObject fo = SourceUtils.getFile(h, cpInfo);
            if (fo == null) {
                continue;
            }

            JavaSource js = JavaSource.forFileObject(fo);
            if (js == null) {
                continue;
            }

            js.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                TypeElement type = h.resolve(cc);
                if (type == null) {
                    return;
                }

                if (type.getSimpleName().contentEquals(symbolName)) {
                    result.append("Class: ")
                            .append(type.getQualifiedName())
                            .append("\n");
                }

                for (Element e : type.getEnclosedElements()) {
                    if (!e.getSimpleName().contentEquals(symbolName)) {
                        continue;
                    }

                    if (e.getKind() == ElementKind.METHOD
                            || e.getKind() == ElementKind.CONSTRUCTOR) {
                        result.append("Method: ")
                                .append(type.getQualifiedName())
                                .append(".")
                                .append(e.getSimpleName())
                                .append("\n");
                    } else if (e.getKind() == ElementKind.FIELD) {
                        result.append("Field: ")
                                .append(type.getQualifiedName())
                                .append(".")
                                .append(e.getSimpleName())
                                .append("\n");
                    }
                }
            }, true);
        }

        if (result.length() == 0) {
            progress("No matches found for symbol: " + symbolName);
            return "No matches found.";
        }
        return result.toString();
    }

    /**
     * Find all usages of a Java class, method, or field.
     *
     * @param path relative path to a .java file
     * @param symbolName Java symbol name
     * @return usage locations
     */
    @Tool("JAVA ONLY: Find all usages of a Java class, method, or field")
    public String findUsages(String path, String symbolName) throws Exception {

        if (!isJavaFile(path)) {
            return "This tool supports Java source files only (.java).";
        }

        return withJavaSource(path, javaSource -> {
            StringBuilder result = new StringBuilder();
            javaSource.runUserActionTask(cc -> {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                for (TypeElement type
                        : ElementFilter.typesIn(cc.getTopLevelElements())) {

                    for (Element member : type.getEnclosedElements()) {
                        if (member.getSimpleName().contentEquals(symbolName)
                                || type.getSimpleName().contentEquals(symbolName)) {

                            ElementHandle<Element> handle
                                    = ElementHandle.create(member);

                            var query
                                    = new org.netbeans.modules.refactoring.api.WhereUsedQuery(
                                            Lookups.singleton(handle));

                            RefactoringSession session
                                    = RefactoringSession.create("Find Usages");

                            query.prepare(session);
                            session.doRefactoring(true);

                            session.getRefactoringElements()
                                    .forEach(elem
                                            -> result.append("Usage: ")
                                            .append(elem.getDisplayText())
                                            .append("\n")
                                    );
                        }
                    }
                }
            }, true);
            if (result.length() == 0) {
                progress("No usages found for symbol: " + symbolName);
                return "No usages found.";
            }
            return result.toString();
        }, false).toString();
    }
}

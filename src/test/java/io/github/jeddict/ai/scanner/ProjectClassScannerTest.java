/**
 * Copyright 2026 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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
package io.github.jeddict.ai.scanner;

import com.sun.source.tree.CompilationUnitTree;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.BDDAssertions.then;

/**
 *
 * @author Gaurav Gupta
 */
class ProjectClassScannerTest {

    @Test
    void collects_field_and_return_types() throws Exception {
        String source = """
                public class Sample {
                    private String name;
                    public Integer getAge() { return 0; }
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Sample", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .contains("String", "Integer");
    }

    @Test
    void collects_method_parameter_types() throws Exception {
        String source = """
                public class Params {
                    public void setName(String name, Integer age) {}
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Params", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactlyInAnyOrder("String", "Integer");
    }

    @Test
    void collects_generics_arrays_and_throws_types() throws Exception {
        String source = """
                import java.util.List;
                import java.io.IOException;

                public class Complex {
                    private List<String> names;
                    public String[] getNames() throws IOException { return null; }
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Complex", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .contains("List", "String", "IOException");
    }

    @Test
    void collects_extends_and_implements_types() throws Exception {
        String source = """
                public class Child extends Base implements AutoCloseable, Runnable {
                    public void close() {}
                    public void run() {}
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Child", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactlyInAnyOrder("Base", "AutoCloseable", "Runnable");
    }

    @Test
    void collects_multiple_thrown_exceptions() throws Exception {
        String source = """
                import java.io.IOException;
                import java.sql.SQLException;

                public class ThrowsTest {
                    public void run() throws IOException, SQLException {}
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("ThrowsTest", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactlyInAnyOrder("IOException", "SQLException");
    }

    @Test
    void collects_wildcard_generic_bounds() throws Exception {
        String source = """
                import java.util.List;

                public class WildcardTest {
                    private List<? extends Number> numbers;
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("WildcardTest", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactlyInAnyOrder("List", "Number");
    }

    @Test
    void collects_nested_generic_types() throws Exception {
        String source = """
                import java.util.Map;
                import java.util.List;

                public class NestedGenerics {
                    private Map<String, List<Integer>> values;
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("NestedGenerics", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactlyInAnyOrder("Map", "String", "List", "Integer");
    }

    @Test
    void collects_array_component_type_once() throws Exception {
        String source = """
                public class Arrays {
                    private String[] names;
                    private String[][] matrix;
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Arrays", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactly("String");
    }

    @Test
    void collects_member_select_types() throws Exception {
        String source = """
                public class MemberSelect {
                    private java.time.LocalDate date;
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("MemberSelect", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .containsExactly("LocalDate");
    }

    @Test
    void ignores_primitives_and_void() throws Exception {
        String source = """
                public class PrimitiveTest {
                    private int count;
                    public void test(boolean flag) {}
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("PrimitiveTest", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .isEmpty();
    }

    @Test
    void handles_empty_class() throws Exception {
        String source = """
                public class Empty {
                }
                """;

        CompilationUnitTree compilation_unit =
                JavaTestUtils.parse("Empty", source);

        Set<String> referenced_classes =
                ProjectClassScanner.getReferencedClasses(compilation_unit);

        then(referenced_classes)
                .isEmpty();
    }
}

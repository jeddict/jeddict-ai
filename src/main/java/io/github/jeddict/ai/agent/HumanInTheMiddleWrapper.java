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

import dev.langchain4j.agent.tool.Tool;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import static io.github.jeddict.ai.agent.ToolPolicy.Policy.*;

/**
 * A factory for creating dynamic proxy wrappers around tool objects.
 * <p>
 * This factory uses Byte Buddy to create a subclass of a given tool object at
 * runtime. The proxy intercepts all method calls, allowing for pre-execution
 * checks based on a {@link ToolPolicy}. This ensures that potentially harmful
 * or state-changing operations can be reviewed before execution.
 * </p>
 * <p>
 * <b>Levels of Concern based on ToolPolicy:</b>
 * <ul>
 *     <li><b>{@code READ}</b>: Mostly no concern. These tools perform safe,
 *     read-only operations and are typically executed directly without
 *     additional approval.</li>
 *     <li><b>{@code INTERACTIVE}</b>: No concern. These tools inherently require
 *     user interaction (e.g., displaying a UI), and the interaction itself
 *     serves as an implicit approval. They are executed directly.</li>
 *     <li><b>{@code WRITE}</b>: Potential risk. These tools modify files or system
 *     state, posing a potential risk. They are intercepted, and a Human-in-the-Middle (HITM)
 *     function is invoked for explicit approval before execution.</li>
 *     <li><b>{@code UNKNOWN}</b>: Potential risk. This is the default policy if
 *     not explicitly provided, or for tools with an unspecified risk level. To
 *     err on the side of caution, these are treated as high-risk and are
 *     intercepted for approval via the HITM function.</li>
 * </ul>
 *
 * @see ToolPolicy
 */
public class HumanInTheMiddleWrapper {

    private final Function<String, Boolean> hitm;

    /**
     * Constructs a new HumanInTheMiddleWrapper.
     *
     * @param hitm A "Human in the Middle" (HITM) function that is invoked
     *             before executing a tool method that requires approval. The
     *             function receives a formatted string describing the tool call
     *             and must return {@code true} to allow execution or
     *             {@code false} to reject it.
     */
    public HumanInTheMiddleWrapper(Function<String, Boolean> hitm) {
        this.hitm = hitm;
    }

    /**
     * Wraps the given tool object with a policy-aware proxy.
     * <p>
     * The returned proxy intercepts all method calls. For methods annotated with
     * {@link Tool}, it applies the logic defined in {@link ToolPolicy}. All
     * method calls, whether intercepted or not, are delegated to the original
     * tool object to ensure its state and behavior are preserved.
     *
     * @param <T>          The type of the tool.
     * @param originalTool The original tool instance to wrap.
     * @return A proxy instance of the same type as the original tool.
     */
    public <T> T wrap(T originalTool) {
        try {
            Class<?> clazz = originalTool.getClass();
            DynamicType.Builder<?> builder = new ByteBuddy().subclass(clazz);

            for (Method method : clazz.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue; // Do not intercept methods from Object class (e.g., hashCode, equals)
                }

                // Intercept the method, delegate to our interceptor, and copy all annotations
                builder = builder.method(ElementMatchers.is(method))
                                 .intercept(MethodDelegation.to(new Interceptor(hitm, originalTool)))
                                 .annotateMethod(method.getDeclaredAnnotations());
            }


            return (T) builder.make()
                    .load(clazz.getClassLoader())
                    .getLoaded()
                    .getConstructor(clazz.getDeclaredConstructors()[0].getParameterTypes())
                    .newInstance(getDummyArgsFor(clazz.getDeclaredConstructors()[0]));
        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap tool", e);
        }
    }

    private Object[] getDummyArgsFor(Constructor<?> constructor) {
        Object[] dummyArgs = new Object[constructor.getParameterCount()];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isPrimitive()) {
                if (parameterTypes[i] == boolean.class) dummyArgs[i] = false;
                else dummyArgs[i] = 0;
            } else if (parameterTypes[i] == String.class) {
                dummyArgs[i] = "."; // Satisfy AbstractTool's non-null basedir check
            } else {
                dummyArgs[i] = null;
            }
        }
        return dummyArgs;
    }

    /**
     * The Byte Buddy interceptor that contains the core HITM logic.
     */
    public static class Interceptor {
        private final Function<String, Boolean> hitm;
        private final Object target;

        public Interceptor(Function<String, Boolean> hitm, Object target) {
            this.hitm = hitm;
            this.target = target;
        }

        @RuntimeType
        public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Exception {
            // Only apply policy logic if the method is a @Tool
            if (method.isAnnotationPresent(Tool.class)) {
                ToolPolicy policyAnn = method.getAnnotation(ToolPolicy.class);
                ToolPolicy.Policy policy = (policyAnn != null) ? policyAnn.value() : UNKNOWN;

                // For safe policies, execute directly
                if (policy == READONLY || policy == INTERACTIVE) {
                    return method.invoke(target, args);
                }

                // For risky policies, invoke the HITM function for approval
                if (hitm != null) {
                    String message = formatHitmMessage(method, args);
                                            if (!hitm.apply(message)) {
                            throw new ToolExecutionRejected("user cancelled action: " + method.getName());
                        }                }
            }
            
            // Always delegate execution to the original target to preserve state
            return method.invoke(target, args);
        }

        private String formatHitmMessage(Method method, Object[] args) {
            StringBuilder sb = new StringBuilder("Can I execute the tool below?\n");
            sb.append("   ").append(method.getName()).append("\n");

            // Try to find the method on the target class to get better parameter names
            Method targetMethod = method;
            try {
                targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                // Fallback to the original method if not found (shouldn't happen for valid overrides)
            }

            Parameter[] parameters = targetMethod.getParameters();
            if (parameters.length > 0) {
                for (int i = 0; i < parameters.length; i++) {
                    String paramName = parameters[i].getName(); // Should be real name if compiled with -parameters
                    String valueString = "null";
                    if (args != null && i < args.length) {
                        Object value = args[i];
                        if (value != null) {
                            if (value.getClass().isArray()) {
                                valueString = Arrays.deepToString((Object[]) value);
                            } else {
                                valueString = value.toString();
                            }
                        }
                    }
                    sb.append("   ").append(paramName).append(": ").append(valueString).append("\n");
                }
            }
            return sb.toString();
        }
    }
}

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the execution policy for a tool method, allowing for fine-grained
 * control over how and when tools are executed.
 * <p>
 * <b>Rationale:</b>
 * Not all tools are created equal. Some tools are safe, read-only operations
 * (e.g., listing files), while others perform sensitive or destructive actions
 * (e.g., writing to a file, executing a shell command). This annotation
 * provides a mechanism to classify tools so that a "human in the middle" (HITM)
 * or a safety supervisor can decide whether to allow an execution.
 * <p>
 * The policies are:
 * <ul>
 *     <li>{@link Policy#READ}: For safe, read-only operations that do not
 *     change system state. These are typically allowed to run without
 *     interruption.</li>
 *     <li>{@link Policy#WRITE}: For operations that modify files or system
 *     state. These are considered high-risk and should be intercepted for
 *     approval.</li>
 *     <li>{@link Policy#INTERACTIVE}: For tools that inherently require user
 *     interaction (e.g., opening an editor). These are allowed to run without
 *     pre-approval, as the interaction itself is the approval.</li>
 *     <li>{@link Policy#UNKNOWN}: The default policy for any tool where the
 *     risk level is not specified. To err on the side of caution, these are
 *     treated as high-risk and are intercepted for approval.</li>
 * </ul>
 * If this annotation is not present on a {@code @Tool} method, it is treated as
 * {@link Policy#UNKNOWN}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ToolPolicy {
    
    Policy value() default Policy.UNKNOWN;

    enum Policy {
        /** A safe, read-only operation. */
        READ,
        /** An operation that writes to the file system or changes state. */
        WRITE,
        /** An operation that is inherently interactive. */
        INTERACTIVE,
        /** An operation with an unspecified risk level (treated as high-risk). */
        UNKNOWN
    }
}
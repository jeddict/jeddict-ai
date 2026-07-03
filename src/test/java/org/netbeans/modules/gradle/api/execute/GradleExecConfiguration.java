/*
 * Copyright 2026 the original author or authors from the LLMToolify project
 * (https://github.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://apache.org
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.netbeans.modules.gradle.api.execute;

import org.netbeans.spi.project.ProjectConfiguration;

/**
 * Shadow test-classpath implementation of GradleExecConfiguration to bypass
 * package-private visibility limits during detached unit tests.
 */
public class GradleExecConfiguration implements ProjectConfiguration {

    // Public token matching the internal constant ID of the active configuration
    public static final String ACTIVE = "active";

    // Fallback public default constant token required by RunUtils
    public static final GradleExecConfiguration DEFAULT = new GradleExecConfiguration("default");

    private final String id;

    public GradleExecConfiguration(String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return id + " Configuration";
    }
}

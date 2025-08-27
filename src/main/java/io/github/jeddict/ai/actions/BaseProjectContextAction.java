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
package io.github.jeddict.ai.actions;

import org.netbeans.api.project.Project;

/**
 * A base class for context-aware actions that are specific to projects. This
 * class provides a project instance to its subclasses.
 */
public abstract class BaseProjectContextAction extends BaseContextAction {
    protected Project project;

    /**
     * Constructs a new BaseProjectContextAction.
     *
     * @param name the name of the action.
     * @param project the project.
     * @param enable true to enable the action, false to disable it.
     */
    public BaseProjectContextAction(
        final String name, final Project project, final boolean enable
    ) {
        super(name, enable);
        this.project = project;
    }

}

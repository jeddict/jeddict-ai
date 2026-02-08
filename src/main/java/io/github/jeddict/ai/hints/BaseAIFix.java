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
package io.github.jeddict.ai.hints;

import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.AgentUtil;
import java.util.List;
import java.util.logging.Logger;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.hints.JavaFix;

/**
 * Abstract base class for AI-powered code fixes that extends {@link JavaFix}.
 * Provides common functionality for AI-driven code corrections, including
 * rule management and brain initialization for AI reasoning.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Centralized logging through a dedicated {@link Logger} instance</li>
 *   <li>Access to application preferences via {@link PreferencesManager}</li>
 *   <li>Management of global and project-specific rules for AI guidance</li>
 *   <li>Initialization of the AI reasoning engine ({@link JeddictBrain})</li>
 * </ul>
 *
 * <p>Concrete implementations should provide specific fix logic while leveraging
 * the common infrastructure provided by this base class.
 *
 */
public abstract class BaseAIFix extends JavaFix {

    protected final Logger LOG = Logger.getLogger(this.getClass().getCanonicalName());

    protected final PreferencesManager pm = PreferencesManager.getInstance();

    protected final Action action;

    public BaseAIFix(final TreePathHandle treePathHandle, final Action action) {
        super(treePathHandle);
        this.action = action;
    }

    /**
     * Creates and initializes a new instance of {@link JeddictBrain} with default settings.
     *
     * <p>This method constructs a {@code JeddictBrain} object using the current model name
     * from the provided {@link #pm} (presumably a model or property manager), disables
     * any special initialization flags (set to {@code false}), and initializes it
     * with an empty list of configurations or parameters.
     *
     * <p>The returned {@code JeddictBrain} is ready for further customization or use
     * in natural language processing, knowledge base operations, or other AI-related tasks
     * depending on the implementation details of the {@code JeddictBrain} class.
     *
     * @return a new {@link JeddictBrain} instance configured with the default settings:
     *         model name from {@link #pm}, no special initialization, and no additional parameters.
     *
     * @see JeddictBrain#JeddictBrain(String, boolean, List)
     * @see #pm
     */
    protected JeddictBrain newJeddictBrain() {
        return new JeddictBrain(pm.getModelName(), false);
    }

    protected String globalRules() {
        return AgentUtil.normalizeRules(pm.getGlobalRules());
    }

    protected String projectRules(final Project project) {
        return AgentUtil.normalizeRules(pm.getProjectRules(project));
    }

}

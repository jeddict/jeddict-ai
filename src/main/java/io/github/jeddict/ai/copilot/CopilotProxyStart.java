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
package io.github.jeddict.ai.copilot;

import io.github.jeddict.ai.settings.GenAIProvider;
import io.github.jeddict.ai.settings.PreferencesManager;
import org.openide.util.Lookup;
import org.openide.windows.OnShowing;

/**
 *
 * @author arsi arsi(at)arsi.sk
 */
/**
 * This class is responsible for starting the Copilot Proxy service when the
 * application is showing. It implements the {@link Runnable} interface, and in
 * its {@code run()} method, it checks if the current AI provider is set to
 * {@code COPILOT_PROXY}. If so, it retrieves an instance of
 * {@link RunCopilotProxy} and starts the proxy.
 * <p>
 * The class is annotated with {@code @OnShowing}, indicating that it is
 * intended to be triggered when a particular UI component or application state
 * is shown.
 * </p>
 *
 * @see Runnable
 * @see RunCopilotProxy
 * @see GenAIProvider#COPILOT_PROXY
 * @see PreferencesManager
 * @see OnShowing
 */
@OnShowing
public class CopilotProxyStart implements Runnable {

    @Override
    public void run() {
        if (PreferencesManager.getInstance().getProvider() == GenAIProvider.COPILOT_PROXY) {
            RunCopilotProxy proxy = Lookup.getDefault().lookup(RunCopilotProxy.class);
            proxy.startProxy();
        }
    }

}

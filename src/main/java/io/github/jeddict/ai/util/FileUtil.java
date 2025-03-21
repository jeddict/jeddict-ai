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
package io.github.jeddict.ai.util;

import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;

/**
 *
 * @author Shiwani Gupta
 */
public class FileUtil {

    public static void saveOpenEditor() throws Exception {
        // Get the active TopComponent (the active editor window)
        TopComponent activatedComponent = TopComponent.getRegistry().getActivated();

        // Check if a document is open
        if (activatedComponent != null) {
            // Lookup the DataObject of the active editor
            DataObject dataObject = activatedComponent.getLookup().lookup(DataObject.class);

            if (dataObject != null) {
                // Get the SaveCookie from the DataObject
                SaveCookie saveCookie = dataObject.getLookup().lookup(SaveCookie.class);

                // If there are unsaved changes, save the file
                if (saveCookie != null) {
                    saveCookie.save();
                }
            }
        }
    }

}

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

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TypeUtilities;
import org.netbeans.swing.plaf.LFCustoms;

/**
 *
 * @author Gaurav Gupta
 */
public class Utilities {

    public static String getHTMLColor(int r, int g, int b) {
        Color c = LFCustoms.shiftColor(new Color(r, g, b));
        return "<font color=#" //NOI18N
                + LFCustoms.getHexString(c.getRed())
                + LFCustoms.getHexString(c.getGreen())
                + LFCustoms.getHexString(c.getBlue())
                + ">"; //NOI18N
    }

    public static CharSequence getTypeName(CompilationInfo info, TypeMirror type, boolean fqn) {
        return getTypeName(info, type, fqn, false);
    }

    public static CharSequence getTypeName(CompilationInfo info, TypeMirror type, boolean fqn, boolean varArg) {
        Set<TypeUtilities.TypeNameOptions> options = EnumSet.noneOf(TypeUtilities.TypeNameOptions.class);
        if (fqn) {
            options.add(TypeUtilities.TypeNameOptions.PRINT_FQN);
        }
        if (varArg) {
            options.add(TypeUtilities.TypeNameOptions.PRINT_AS_VARARG);
        }
        return info.getTypeUtilities().getTypeName(type, options.toArray(new TypeUtilities.TypeNameOptions[0]));
    }

    public static String errorHTMLBlock(Throwable x) {
        return String.format("""
```html
<table border="1" bgcolor="#FFFFCC" cellpadding="5" cellspacing="0" width="100%%">
  <tr>
    <td>
        <b>An error occurred communicating with the model:</b><br>
        <pre><font color="#666666">%s</font></pre>
    </td>
  </tr>
</table>
```
        """, ExceptionUtils.getStackTrace(x));
    }
}

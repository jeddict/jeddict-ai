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
package io.github.jeddict.ai.completion;

import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.util.SourceUtil;
import io.github.jeddict.ai.util.Utilities;
import static io.github.jeddict.ai.util.Utilities.getHTMLColor;
import java.net.URL;
import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeMirror;
import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.modules.editor.java.JavaCompletionItem;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.openide.xml.XMLUtil;

/**
 *
 * @author Gaurav Gupta
 */
public class JeddictItem extends JavaCompletionItem {

    private static final String LOCAL_VARIABLE = "io/github/jeddict/ai/logo.png"; //NOI18N
    private static final String PARAMETER_COLOR = getHTMLColor(64, 64, 188);
    private static ImageIcon icon;

    private String varName;
    private String description;
    private List<String> imports;
    private boolean newVarName;
    private boolean smartType;
    private String typeName;
    private String leftText;
    private String rightText;
    private int assignToVarOffset;
    private CharSequence assignToVarText;
    int caretToEndLength;

    public JeddictItem(CompilationInfo info, TypeMirror type, String varName, String description, List<String> imports, int substitutionOffset, int caretToEndLength, boolean newVarName, boolean smartType, int assignToVarOffset) {
        super(substitutionOffset);
        this.varName = varName;
        this.imports = imports;
        this.description = description;
        this.caretToEndLength = caretToEndLength;
        this.newVarName = newVarName;
        this.smartType = true;
        this.typeName = type != null ? Utilities.getTypeName(info, type, false).toString() : null;
        this.assignToVarOffset = assignToVarOffset;
        this.assignToVarText = assignToVarOffset < 0 ? null : createAssignToVarText(info, type, varName);
    }

    public JeddictItem(CompilationInfo info, TypeMirror type, String varName, String description, List<String> imports, int substitutionOffset, boolean newVarName, boolean smartType, int assignToVarOffset) {
        super(substitutionOffset);
        this.varName = varName;
        this.description = description;
        this.imports = imports;
        this.caretToEndLength = -1;
        this.newVarName = newVarName;
        this.smartType = true;
        this.typeName = type != null ? Utilities.getTypeName(info, type, false).toString() : null;
        this.assignToVarOffset = assignToVarOffset;
        this.assignToVarText = assignToVarOffset < 0 ? null : createAssignToVarText(info, type, varName);
    }

    @Override
    public void defaultAction(JTextComponent component) {
        super.defaultAction(component);
        try {
            int startPos = substitutionOffset;
            int lengthToReplace = varName.length();

            if (caretToEndLength > 0) {
                component.getDocument().remove(startPos + lengthToReplace, caretToEndLength);
            }

            Reformat reformat = Reformat.get(component.getDocument());
            reformat.lock();
            try {
                reformat.reformat(startPos, startPos + lengthToReplace);
            } finally {
                reformat.unlock();
            }

            CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>() {
                @Override
                public void run(WorkingCopy workingCopy) throws Exception {
                    workingCopy.toPhase(JavaSource.Phase.RESOLVED);
                    SourceUtil.addImports(workingCopy, imports);
                }

                @Override
                public void cancel() {
                }
            };
            JavaSource javaSource = JavaSource.forDocument(component.getDocument());
            if (javaSource != null) {
                javaSource.runModificationTask(task).commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSortPriority() {
        return smartType ? 200 - SMART_TYPE : 200;
    }

    @Override
    public CharSequence getSortText() {
        return varName;
    }

    @Override
    public CharSequence getInsertPrefix() {
        return varName;
    }

    @Override
    protected String getLeftHtmlText() {

        if (leftText == null) {
            leftText = PARAMETER_COLOR + BOLD + varName.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    + BOLD_END + COLOR_END;
        }
        return leftText;
    }

    @Override
    protected String getRightHtmlText() {
        if (rightText == null) {
            rightText = escape(typeName);
        }
        return rightText;
    }

    @Override
    protected ImageIcon getIcon() {
        return AssistantChat.icon;
    }

    @Override
    public int getAssignToVarOffset() {
        return assignToVarOffset;
    }

    @Override
    public CharSequence getAssignToVarText() {
        return assignToVarText;
    }

    @Override
    public String toString() {
        return (typeName != null ? typeName + " " : "") + varName; //NOI18N
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return varName == null ? null : new CompletionTask() {
            private final CompletionDocumentation cd = new CompletionDocumentation() {
                @Override
                public String getText() {

                    String escapedString = varName
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;");

                    String keywordsPattern = "\\b("
                            + "public|private|protected|static|final|abstract|synchronized|native|strictfp|transient|volatile|"
                            + "boolean|byte|char|short|int|long|float|double|void|"
                            + "if|else|switch|case|default|break|continue|return|for|while|do|"
                            + "try|catch|finally|throw|throws|assert|"
                            + "class|interface|extends|implements|new|this|super|"
                            + "import|package|enum|instanceof"
                            + ")\\b|[{}]";
                    escapedString = escapedString.replaceAll(keywordsPattern, "<b>$0</b>");
                    escapedString = escapedString.replaceAll("System\\.out\\.println", "<i>System.out.println</i>");

                    if (description != null && !description.isEmpty()) {
                        return "<html><body><pre>" + escapedString + "\n\n\n" + description.replaceAll("<\br>", "\n") + "</pre></body></html>";
                    } else {
                        return "<html><body><pre>" + escapedString + "</pre></body></html>";
                    }
                }

                @Override
                public URL getURL() {
                    return null;
                }

                @Override
                public CompletionDocumentation resolveLink(String link) {
                    return null;
                }

                @Override
                public javax.swing.Action getGotoSourceAction() {
                    return null;
                }
            };

            @Override
            public void query(CompletionResultSet resultSet) {
                resultSet.setDocumentation(cd);
                resultSet.finish();
            }

            @Override
            public void refresh(CompletionResultSet resultSet) {
                resultSet.setDocumentation(cd);
                resultSet.finish();
            }

            @Override
            public void cancel() {
            }
        };
    }

    private static CharSequence createAssignToVarText(CompilationInfo info, TypeMirror type, String name) {
        name = adjustName(name);
        type = SourceUtils.resolveCapturedType(info, type);
        StringBuilder sb = new StringBuilder();
        sb.append("${TYPE type=\""); //NOI18N
        sb.append(Utilities.getTypeName(info, type, true));
        sb.append("\" default=\""); //NOI18N
        sb.append(Utilities.getTypeName(info, type, false));
        sb.append("\" editable=false}"); //NOI18N
        sb.append(" ${NAME newVarName=\""); //NOI18N
        sb.append(name);
        sb.append("\" default=\""); //NOI18N
        sb.append(name);
        sb.append("\"} = "); //NOI18N
        return sb;
    }

    private static String escape(String s) {
        if (s != null) {
            try {
                return XMLUtil.toElementContent(s);
            } catch (Exception ex) {
            }
        }
        return s;
    }

    private static String adjustName(String name) {
        if (name == null) {
            return null;
        }
        String shortName = null;
        if (name.startsWith("get") && name.length() > 3) { //NOI18N
            shortName = name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2) { //NOI18N
            shortName = name.substring(2);
        }
        if (shortName != null) {
            return firstToLower(shortName);
        }
        if (SourceVersion.isKeyword(name)) {
            return "a" + Character.toUpperCase(name.charAt(0)) + name.substring(1); //NOI18N
        } else {
            return name;
        }
    }

    private static String firstToLower(String name) {
        if (name.length() == 0) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean toLower = true;
        char last = Character.toLowerCase(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            if (toLower && Character.isUpperCase(name.charAt(i))) {
                result.append(Character.toLowerCase(last));
            } else {
                result.append(last);
                toLower = false;
            }
            last = name.charAt(i);
        }
        result.append(last);
        if (SourceVersion.isKeyword(result)) {
            return "a" + name; //NOI18N
        } else {
            return result.toString();
        }
    }

}

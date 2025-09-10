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

import io.github.jeddict.ai.agent.FileAction;
import io.github.jeddict.ai.agent.FileActionParser;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.response.Block;
import io.github.jeddict.ai.response.Response;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import static io.github.jeddict.ai.util.MimeUtil.MIME_MARKDOWN;
import static io.github.jeddict.ai.util.MimeUtil.MIME_MERMAID;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PUML;
import static io.github.jeddict.ai.util.SourceUtil.findClassInSourcePath;
import static io.github.jeddict.ai.util.SourceUtil.findFileInProjects;
import static io.github.jeddict.ai.util.SourceUtil.openFileInEditor;
import static io.github.jeddict.ai.util.SourceUtil.openFileInEditorAtLine;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.View;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.FontColorNames;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Shiwani Gupta
 */
public class EditorUtil {

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();
    private static final Logger LOG = Logger.getLogger(EditorUtil.class.getName());

    public static String updateEditors(BiConsumer<String, Set<FileObject>> queryUpdate, Project project, AssistantChat topComponent, Response response, Set<FileObject> threadContext) {
        StringBuilder code = new StringBuilder();

        topComponent.clear();

        if (response.getQuery() != null && !response.getQuery().isEmpty()) {
            topComponent.createUserQueryPane(queryUpdate, response.getQuery(), response.getMessageContext());
        }

        JComponent firstPane = null;

        Block actionBlock = null;
        for (Block block : response.getBlocks()) {
            LOG.finest(() -> ("block:\n" + block));
            if ("action".equals(block.getType())) {
                actionBlock = block;
                continue;
            }
            LOG.finest("printing\n" + actionBlock + "\nand\n" + block);
            JComponent pane = printBlock(code, actionBlock, block, project, topComponent);
            actionBlock = null;
            if (firstPane == null) {
                firstPane = pane;
            }
        }

        if (firstPane != null) {
            firstPane.scrollRectToVisible(firstPane.getBounds());
        }
        topComponent.revalidate();
        topComponent.repaint();
        List<FileObject> context = new ArrayList<>();
        if (threadContext != null && !threadContext.isEmpty()) {
            context.addAll(threadContext);
        }
        if (response.getMessageContext() != null && !response.getMessageContext().isEmpty()) {
            context.addAll(response.getMessageContext());
        }
        topComponent.getParseCodeEditor(context);
        topComponent.attachMenusToEditors();
        return code.toString();
    }

    public static JComponent printBlock(
        final StringBuilder code,
        final Block actionBlock,
        final Block contentBlock,
        final Project project,
        final AssistantChat assistantChat
    ) {
        LOG.finest(
            () -> "\n" + code + "\n"
                + actionBlock + "\n"
                + contentBlock + "\n"
                + project + "\n"
                + assistantChat
        );
        JComponent pane;
        if (contentBlock != null && (contentBlock.getType().equals("text") || contentBlock.getType().equals("web"))) {
            LOG.finest(() -> "Creating a text/web pane");
            String html;
            if (contentBlock.getType().equals("text")) {
                html = renderer.render(parser.parse(contentBlock.getContent()));
                html = wrapClassNamesWithAnchor(html);
            } else {
                html = contentBlock.getContent();
            }
            JEditorPane htmlPane = assistantChat.createHtmlPane(html);
            pane = htmlPane;
            htmlPane.addHyperlinkListener(e -> {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    String fileName = e.getDescription();
                    if (fileName.endsWith(".java")) {
                        String javaClass = fileName.substring(0, fileName.length() - 5);
                        FileObject path = findClassInSourcePath(javaClass, true);
                        if (path != null) {
                            openFileInEditor(path);
                        }
                    }
                    if (fileName.startsWith("#")) {
                        int lineNumber = -1;
                        String javaClass = fileName.substring(1);
                        if (javaClass.contains("@")) {
                            String[] javaClassLoc = javaClass.split("@");
                            javaClass = javaClassLoc[0];
                            lineNumber = Integer.parseInt(javaClassLoc[1]);
                        }
                        FileObject path = findFileInProjects(javaClass);
                        if (path == null) {
                            path = findClassInSourcePath(javaClass, true);
                        }
                        if (path != null) {
                            if (lineNumber < 0) {
                                openFileInEditor(path);
                            } else {
                                openFileInEditorAtLine(path, lineNumber);
                            }
                        }
                    }
                }
            });
        } else if (
            actionBlock != null &&
            contentBlock != null &&
            project != null
        ) {
            LOG.finest(() -> "Creating an action pane");

            //
            // This is text for which we have an action and the chat is attached
            // to a project. In the case the project is null the block can be
            // handled as a normal block.
            //

            FileAction action = FileActionParser.parse(actionBlock.getContent(), contentBlock.getContent());
            code.append('\n').append(contentBlock.getContent()).append('\n');
            pane = assistantChat.createActionPane(action);
        } else {
            LOG.finest(() -> "Creating specialized pane");
            code.append('\n').append(contentBlock.getContent()).append('\n');
            String mimeType = getMimeType(contentBlock.getType());
            if (MIME_PUML.equals(mimeType)) {
                LOG.finest(() -> "Creating an SVG pane");
                pane = assistantChat.createSVGPane(contentBlock);
            } else if (MIME_MARKDOWN.equals(mimeType)) {
                LOG.finest(() -> "Creating a Markdown pane");
                pane = assistantChat.createMarkdownPane(contentBlock);
            } else if (MIME_MERMAID.equals(mimeType)) {
                LOG.finest(() -> "Creating an Mermaid pane");
                pane = assistantChat.createMermaidPane(contentBlock);
            } else {
                LOG.finest(() -> "Creating an generic code pane");
                pane = assistantChat.createCodePane(mimeType, contentBlock);
            }
        }
        return pane;
    }

    public static String addLineBreaksToMarkdown(String markdown, int maxLineLength) {
        String[] lines = markdown.split("\n");
        StringBuilder formattedMarkdown = new StringBuilder();

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                formattedMarkdown.append(breakLongLine(line, maxLineLength));
            }
            formattedMarkdown.append('\n');
        }

        return formattedMarkdown.toString();
    }

    private static String breakLongLine(String line, int maxLineLength) {
        StringBuilder formattedLine = new StringBuilder();
        int lastBreakIndex = 0;

        for (int i = 0; i < line.length(); i++) {
            if (i - lastBreakIndex >= maxLineLength && (line.charAt(i) == '.' || line.charAt(i) == ',')) {
                formattedLine.append(line, lastBreakIndex, i + 1).append("<br>");
                lastBreakIndex = i + 1;
            }
        }

        if (lastBreakIndex < line.length()) {
            formattedLine.append(line.substring(lastBreakIndex));
        }

        return formattedLine.toString();
    }

    private static final Map<String, String> OPENAI_NETBEANS_EDITOR_MAP = new HashMap<>();
    private static final Map<String, String> REVERSE_OPENAI_NETBEANS_EDITOR_MAP = new HashMap<>();

    static {
        // OpenAI Code Block and NetBeans Editor MIME type mappings
        OPENAI_NETBEANS_EDITOR_MAP.put("java", JAVA_MIME);
        OPENAI_NETBEANS_EDITOR_MAP.put("puml", "text/x-puml");
        OPENAI_NETBEANS_EDITOR_MAP.put("plantuml", "text/x-puml");
        OPENAI_NETBEANS_EDITOR_MAP.put("mermaid", "text/x-mermaid");
        OPENAI_NETBEANS_EDITOR_MAP.put("md", "text/x-markdown");
        OPENAI_NETBEANS_EDITOR_MAP.put("markdown", "text/x-markdown");
        OPENAI_NETBEANS_EDITOR_MAP.put("xml", "text/xml");
        OPENAI_NETBEANS_EDITOR_MAP.put("python", "text/x-python");
        OPENAI_NETBEANS_EDITOR_MAP.put("javascript", "text/javascript");
        OPENAI_NETBEANS_EDITOR_MAP.put("html", "text/html");
        OPENAI_NETBEANS_EDITOR_MAP.put("css", "text/css");
        OPENAI_NETBEANS_EDITOR_MAP.put("sql", "text/x-sql");
        OPENAI_NETBEANS_EDITOR_MAP.put("csharp", "text/x-csharp");
        OPENAI_NETBEANS_EDITOR_MAP.put("cpp", "text/x-c++src");
        OPENAI_NETBEANS_EDITOR_MAP.put("bash", "text/x-shellscript");
        OPENAI_NETBEANS_EDITOR_MAP.put("ruby", "text/x-ruby");
        OPENAI_NETBEANS_EDITOR_MAP.put("go", "text/x-go");
        OPENAI_NETBEANS_EDITOR_MAP.put("kotlin", "text/x-kotlin");
        OPENAI_NETBEANS_EDITOR_MAP.put("php", "text/x-php");
        OPENAI_NETBEANS_EDITOR_MAP.put("r", "text/x-r");
        OPENAI_NETBEANS_EDITOR_MAP.put("swift", "text/x-swift");
        OPENAI_NETBEANS_EDITOR_MAP.put("typescript", "text/typescript");
        OPENAI_NETBEANS_EDITOR_MAP.put("scala", "text/x-scala");
        OPENAI_NETBEANS_EDITOR_MAP.put("dart", "text/x-dart"); // Consider "application/dart"
        OPENAI_NETBEANS_EDITOR_MAP.put("perl", "text/x-perl");
        OPENAI_NETBEANS_EDITOR_MAP.put("yaml", "text/x-yaml");
        OPENAI_NETBEANS_EDITOR_MAP.put("json", "text/x-json");
        OPENAI_NETBEANS_EDITOR_MAP.put("asm", "text/x-asm");
        OPENAI_NETBEANS_EDITOR_MAP.put("haskell", "text/x-haskell");
        OPENAI_NETBEANS_EDITOR_MAP.put("latex", "text/x-tex");
        OPENAI_NETBEANS_EDITOR_MAP.put("groovy", "text/x-groovy");
        OPENAI_NETBEANS_EDITOR_MAP.put("powershell", "text/x-powershell");
        OPENAI_NETBEANS_EDITOR_MAP.put("vb", "text/x-vb");
        OPENAI_NETBEANS_EDITOR_MAP.put("scheme", "text/x-scheme");
        OPENAI_NETBEANS_EDITOR_MAP.put("rust", "text/x-rust");
        OPENAI_NETBEANS_EDITOR_MAP.put("objectivec", "text/x-objectivec");
        OPENAI_NETBEANS_EDITOR_MAP.put("elixir", "text/x-elixir");
        OPENAI_NETBEANS_EDITOR_MAP.put("lua", "text/x-lua");
        OPENAI_NETBEANS_EDITOR_MAP.put("cobol", "text/x-cobol");
        OPENAI_NETBEANS_EDITOR_MAP.put("fsharp", "text/x-fsharp");
        OPENAI_NETBEANS_EDITOR_MAP.put("crystal", "text/x-crystal");
        OPENAI_NETBEANS_EDITOR_MAP.put("actionscript", "text/x-actionscript");
        OPENAI_NETBEANS_EDITOR_MAP.put("nim", "text/x-nim");
        OPENAI_NETBEANS_EDITOR_MAP.put("vhdl", "text/x-vhdl");
        OPENAI_NETBEANS_EDITOR_MAP.put("sas", "text/x-sas");
        OPENAI_NETBEANS_EDITOR_MAP.put("cakephp", "text/x-cakephp");
        OPENAI_NETBEANS_EDITOR_MAP.put("laravel", "text/x-laravel");
        OPENAI_NETBEANS_EDITOR_MAP.put("cmake", "text/x-cmake");
        OPENAI_NETBEANS_EDITOR_MAP.put("embeddedc", "text/x-embeddedc");
        OPENAI_NETBEANS_EDITOR_MAP.put("sass", "text/x-sass"); // Separate from SCSS
        OPENAI_NETBEANS_EDITOR_MAP.put("scss", "text/x-scss");
        OPENAI_NETBEANS_EDITOR_MAP.put("less", "text/x-less");
        OPENAI_NETBEANS_EDITOR_MAP.put("pug", "text/x-pug");
        OPENAI_NETBEANS_EDITOR_MAP.put("coffeescript", "text/x-coffeescript");
        OPENAI_NETBEANS_EDITOR_MAP.put("vue", "text/x-vue");
        OPENAI_NETBEANS_EDITOR_MAP.put("jsx", "text/jsx");
        OPENAI_NETBEANS_EDITOR_MAP.put("c", "text/x-c"); // C language
        OPENAI_NETBEANS_EDITOR_MAP.put("clojure", "text/x-clojure"); // Clojure
        OPENAI_NETBEANS_EDITOR_MAP.put("forth", "text/x-forth"); // Forth
        OPENAI_NETBEANS_EDITOR_MAP.put("smalltalk", "text/x-smalltalk"); // Smalltalk
        OPENAI_NETBEANS_EDITOR_MAP.put("sml", "text/x-sml"); // Standard ML
        OPENAI_NETBEANS_EDITOR_MAP.put("ada", "text/x-ada"); // Ada
        OPENAI_NETBEANS_EDITOR_MAP.put("scratch", "text/x-scratch"); // Scratch

// Missing types added
        OPENAI_NETBEANS_EDITOR_MAP.put("properties", "text/x-properties"); // Properties files
        OPENAI_NETBEANS_EDITOR_MAP.put("dockerfile", "text/x-dockerfile"); // Dockerfiles
        OPENAI_NETBEANS_EDITOR_MAP.put("csv", "text/csv"); // CSV files
        OPENAI_NETBEANS_EDITOR_MAP.put("graphql", "application/graphql"); // GraphQL files
        OPENAI_NETBEANS_EDITOR_MAP.put("json5", "text/x-json5"); // JSON5 files
        OPENAI_NETBEANS_EDITOR_MAP.put("yml", "text/x-yaml"); // YAML files (alternative extension)
        OPENAI_NETBEANS_EDITOR_MAP.put("ini", "text/x-ini"); // INI files
        OPENAI_NETBEANS_EDITOR_MAP.put("html5", "text/html"); // HTML5 files

// OpenAI Code Block and NetBeans Editor MIME type mappings for Jakarta EE
        OPENAI_NETBEANS_EDITOR_MAP.put("jakarta", JAVA_MIME); // General Jakarta EE Java files
        OPENAI_NETBEANS_EDITOR_MAP.put("jsp", "text/x-jsp"); // JavaServer Pages
        OPENAI_NETBEANS_EDITOR_MAP.put("faces", "text/x-jsf"); // JavaServer Faces
        OPENAI_NETBEANS_EDITOR_MAP.put("webxml", "text/xml"); // web.xml deployment descriptor
        OPENAI_NETBEANS_EDITOR_MAP.put("persistence", JAVA_MIME); // JPA persistence.xml files
        OPENAI_NETBEANS_EDITOR_MAP.put("beans", JAVA_MIME); // CDI beans.xml files
        OPENAI_NETBEANS_EDITOR_MAP.put("context", JAVA_MIME); // CDI context.xml files
        OPENAI_NETBEANS_EDITOR_MAP.put("config", "text/x-properties"); // Configuration properties files
        OPENAI_NETBEANS_EDITOR_MAP.put("js", "text/javascript"); // JavaScript files for web applications

        for (Map.Entry<String, String> entry : OPENAI_NETBEANS_EDITOR_MAP.entrySet()) {
            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put(entry.getValue(), entry.getKey());

            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put(JAVA_MIME, "java");
            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put("text/xml", "xml");
            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put("text/javascript", "js");
            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put("text/x-yaml", "yaml");
            REVERSE_OPENAI_NETBEANS_EDITOR_MAP.put("text/html", "html");
        }

    }

    // Method to get the NetBeans MIME type for a given ChatGPT code block type
    public static String getMimeType(String chatGptType) {
        if (chatGptType == null) {
            return MIME_PLAIN_TEXT;
        }
        return OPENAI_NETBEANS_EDITOR_MAP.getOrDefault(chatGptType, MIME_PLAIN_TEXT); // Default to binary if not found
    }

    public static String getExtension(String mimeType) {
        if (mimeType == null) {
            return "java";
        }
        return REVERSE_OPENAI_NETBEANS_EDITOR_MAP.getOrDefault(mimeType, null); // Returns null if not found
    }

    public static boolean isSuitableForWebAppDirectory(String mimeType) {
        // Define the allowed MIME types for src/main/webapp
        Set<String> allowedMimeTypes = new HashSet<>(Arrays.asList(
                "text/html", // HTML files
                "text/x-jsp", // JSP files
                "text/css", // CSS files
                "text/x-scss", // SCSS files
                "text/x-less", // LESS files
                "text/javascript", // JavaScript files
                "text/x-vue", // Vue.js files
                "text/x-pug", // Pug template engine files
                "text/xml", // XML files (for web-related configs like web.xml)
                "text/x-ts", // TypeScript files
                "text/x-jsx" // JSX files (React.js)
        ));

        // Check if the MIME type is allowed for web applications
        return allowedMimeTypes.contains(mimeType);
    }

    public static String getHTMLContent(int wrapWidth, String bodyContent) {
        Font newFont = getFontFromMimeType("text/html");
        java.awt.Color textColor = getTextColorFromMimeType("text/html"); // Get text color
        java.awt.Color backgroundColor = getBackgroundColorFromMimeType("text/html"); // Get background color
        boolean isDark = ColorUtil.isDarkColor(backgroundColor);
        String newContent = """
    <html>
      <head>
        <style>
            html, body {
              margin: 0;
              padding: 10px;
              width: NB_WRAP_WIDTHpx;
              font-family: 'NB_FONT_NAME';
              font-size: NB_FONT_SIZEpx;
              line-height: 1.5;
              color: NB_FONT_COLOR;
              background-color: NB_BACKGROUND_COLOR;
              word-wrap: break-word;
              overflow-wrap: break-word;
              white-space: normal;
            }

            h1, h2, h3, h4, h5, h6 {
              margin: 0 0 10px 0;
              font-weight: bold;
              font-size: inherit;
            }

            p {
              margin: 0 0 10px 0;
            }

            a {
              color: #007bff;
              text-decoration: none;
            }
            code a {
              color: #007bff;
            }
            a:hover {
              text-decoration: underline;
            }

            strong {
              font-weight: bold;
            }

            code {
              font-size: NB_FONT_SIZEpx;
              font-family: monospace;
              padding: 2px 4px;
              border-radius: 3px;
              color: #ff6600;
            }

            pre {
              font-size: NB_FONT_SIZEpx;
              padding: 10px;
              overflow-x: auto;
              border-radius: 3px;
              font-family: monospace;
              margin: 10px 0;
            }

            pre code {
              padding: 0;
              background: none;
            }
            ul {
                list-style-type: none;
                padding: 0;
            }
            li {
                margin-bottom: 10px;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
            }
            th, td {
                border: 1px solid NB_HEADER_BACKGROUND_COLOR;
            }
            th {
                background-color: NB_HEADER_BACKGROUND_COLOR;
            }
            .highlight {
                color: #007bff;
            }
        </style>
      </head>
      <body>
        """ + bodyContent + """
      </body>
    </html>
    """;
        newContent = newContent.replace("NB_FONT_SIZE", String.valueOf(newFont.getSize()));
        newContent = newContent.replace("NB_FONT_NAME", newFont.getName());
        newContent = newContent.replace("NB_WRAP_WIDTH", String.valueOf(wrapWidth));

        if (textColor != null) {
            newContent = newContent.replace("NB_FONT_COLOR", "#" + Integer.toHexString(textColor.getRGB()).substring(2).toUpperCase());
        }
        if (backgroundColor != null) {
            newContent = newContent.replace("NB_BACKGROUND_COLOR", "#" + Integer.toHexString(backgroundColor.getRGB()).substring(2).toUpperCase());
            if (isDark) {
                newContent = newContent.replace("NB_HEADER_BACKGROUND_COLOR", "#" + Integer.toHexString(backgroundColor.brighter().getRGB()).substring(2).toUpperCase());
            } else {
                newContent = newContent.replace("NB_HEADER_BACKGROUND_COLOR", "#" + Integer.toHexString(backgroundColor.darker().getRGB()).substring(2).toUpperCase());
            }
        }
        return newContent;
    }

    public static Font getFontFromMimeType(String mimeType) {
        FontColorSettings fcs = MimeLookup.getLookup(MimePath.parse(mimeType))
                .lookup(FontColorSettings.class);

        if (fcs != null) {
            AttributeSet defaultSet = fcs.getFontColors(FontColorNames.DEFAULT_COLORING);
            if (defaultSet != null) {
                String fontFamily = (String) defaultSet.getAttribute(javax.swing.text.StyleConstants.FontFamily);
                Integer fontSize = (Integer) defaultSet.getAttribute(javax.swing.text.StyleConstants.FontSize);
                if (fontFamily != null && fontSize != null) {
                    return new Font(fontFamily, Font.PLAIN, fontSize);
                }
            }
        }
        return null;
    }

    public static java.awt.Color getTextColorFromMimeType(String mimeType) {
        FontColorSettings fcs = MimeLookup.getLookup(MimePath.parse(mimeType))
                .lookup(FontColorSettings.class);

        if (fcs != null) {
            AttributeSet defaultSet = fcs.getFontColors(FontColorNames.DEFAULT_COLORING);
            if (defaultSet != null) {
                return (java.awt.Color) defaultSet.getAttribute(javax.swing.text.StyleConstants.Foreground);
            }
        }
        return null;
    }

    public static java.awt.Color getBackgroundColorFromMimeType(String mimeType) {
        FontColorSettings fcs = MimeLookup.getLookup(MimePath.parse(mimeType))
                .lookup(FontColorSettings.class);

        if (fcs != null) {
            AttributeSet defaultSet = fcs.getFontColors(FontColorNames.DEFAULT_COLORING);
            if (defaultSet != null) {
                return (java.awt.Color) defaultSet.getAttribute(javax.swing.text.StyleConstants.Background);
            }
        }
        return null;
    }

    /**
     * Wraps Java class names inside <code></code> with <a></a> tags.
     */
    public static String wrapClassNamesWithAnchor(String input) {
        input = input.replaceAll("(\\b\\w+\\.java\\b)", "<a href=\"$1\">$1</a>");
        Pattern pattern = Pattern.compile("<code>([^<]+)</code>");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!token.matches("[\\w\\.]+")) {
                matcher.appendReplacement(sb, matcher.group());
                continue;
            }
            String lastSegment = token.contains(".") ? token.substring(token.lastIndexOf('.') + 1) : token;
            if (lastSegment.length() > 0 && Character.isUpperCase(lastSegment.charAt(0))) {
                String replacement = "<a href=\"#" + token + "\">" + token + "</a>";
                replacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, matcher.group());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Get text for a specified line.
     *
     * @param document document
     * @param line line number
     * @return line text
     */
    public static String getLineText(BaseDocument document, int line) {
        if (document == null || line < 0) {
            return ""; // NOI18N
        }
        int startOffset = LineDocumentUtils.getLineStartFromIndex(document, line);
        if (startOffset == -1) {
            return ""; // NOI18N
        }
        try {
            int endOffset = LineDocumentUtils.getLineEnd(document, startOffset);
            if (endOffset == -1) {
                return ""; // NOI18N
            }
            return document.getText(startOffset, endOffset - startOffset);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return ""; // NOI18N
    }

    public static String getLineText(Document document, View view) {
        if (document == null || view == null) {
            return ""; // NOI18N
        }
        int startOffset = view.getStartOffset();
        int endOffset = view.getEndOffset();
        try {
            return document.getText(startOffset, endOffset - startOffset);
        } catch (BadLocationException ex) {
            LOG.log(Level.WARNING, ex.getMessage());
        }
        return ""; // NOI18N
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.jeddict.ai.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Shiwani Gupta
 */
public class MimeUtil {
    
    public static final String JAVA_MIME = "text/x-java";
    public static final String MIME_HTML = "text/html";
    public static final String MIME_CSS = "text/css";
    public static final String MIME_SCSS = "text/scss";
    public static final String MIME_SASS = "text/sass";
    public static final String MIME_LESS = "text/less";
    public static final String MIME_XHTML = "text/xhtml";
    public static final String MIME_JS = "application/javascript";
    public static final String MIME_JS_ALT = "text/x-javascript";
    public static final String MIME_JADE = "text/jade";
    public static final String MIME_MAVEN_POM = "text/x-maven-pom+xml";
    public static final String MIME_GRADLE = "text/x-gradle+x-groovy";
    public static final String MIME_NETBEANS_LAYER = "text/x-netbeans-layer+xml";
    public static final String MIME_BEANS_JAKARTA = "text/x-beans-jakarta+xml";
    public static final String MIME_BEANS = "text/x-beans+xml";
    public static final String MIME_JSP = "text/x-jsp";
    public static final String MIME_JSON = "text/x-json";
    public static final String MIME_YAML = "text/x-yaml";
    public static final String MIME_TOML = "text/x-toml";
    public static final String MIME_PROPERTIES = "text/x-properties";
    public static final String MIME_GROOVY = "text/x-groovy";
    public static final String MIME_DOCKERFILE = "text/x-dockerfile";
    public static final String MIME_SQL = "text/x-sql";
    public static final String MIME_XML_DTD = "application/xml-dtd";
    public static final String MIME_XML = "text/xml";
    public static final String MIME_TESTNG = "text/x-testng+xml";
    public static final String MIME_FXML = "text/x-fxml+xml";
    public static final String MIME_RUST = "text/x-rust";
    public static final String MIME_TAG = "text/x-tag";
    public static final String MIME_PHP5 = "text/x-php5";
    public static final String MIME_TWIG = "text/x-twig";
    public static final String MIME_TPL = "x-tpl";
    public static final String MIME_LATTE = "text/x-latte";
    public static final String MIME_KO_DATA_BIND = "text/ko-data-bind";
    public static final String MIME_TYPESCRIPT = "application/x-typescript";
    public static final String MIME_ANT_XML = "text/x-ant+xml";
    public static final String MIME_PLAIN_TEXT = "text/plain";
    public static final String MIME_REPL = "text/x-repl";
    public static final String MIME_SHELL = "text/sh";

    public static final Map<String, String> MIME_TYPE_DESCRIPTIONS = new HashMap<>();

    static {
        MIME_TYPE_DESCRIPTIONS.put(MIME_HTML, "HTML elements and attributes");
        MIME_TYPE_DESCRIPTIONS.put(MIME_CSS, "CSS styles");
        MIME_TYPE_DESCRIPTIONS.put(MIME_SCSS, "SCSS styles (Sassy CSS extension)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_SASS, "SASS styles (Syntactically Awesome Style Sheets)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_LESS, "LESS styles (Leaner CSS extension)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_XHTML, "JSF page (Java Server Faces)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_JS, "JavaScript code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_JS_ALT, "JavaScript code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_JADE, "JavaScript Jade template code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_MAVEN_POM, "Maven pom.xml file");
        MIME_TYPE_DESCRIPTIONS.put(MIME_GRADLE, "Gradle build script (Groovy DSL)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_NETBEANS_LAYER, "NetBeans module configuration XML (Layer XML)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_BEANS_JAKARTA, "Jakarta EE Beans XML");
        MIME_TYPE_DESCRIPTIONS.put(MIME_BEANS, "JavaBeans XML (general bean configuration)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_JSP, "JavaServer Pages (JSP) code for server-side Java web applications");
        MIME_TYPE_DESCRIPTIONS.put(MIME_JSON, "JSON");
        MIME_TYPE_DESCRIPTIONS.put(MIME_YAML, "YAML");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TOML, "Tom's Obvious, Minimal Language (TOML) for configuration files");
        MIME_TYPE_DESCRIPTIONS.put(MIME_PROPERTIES, "Properties file (key-value pairs for configuration)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_GROOVY, "Groovy scripting language");
        MIME_TYPE_DESCRIPTIONS.put(MIME_DOCKERFILE, "Dockerfile for defining container images");
        MIME_TYPE_DESCRIPTIONS.put(MIME_SQL, "Structured Query Language (SQL) queries");
        MIME_TYPE_DESCRIPTIONS.put(MIME_XML_DTD, "XML Document Type Definition (DTD) for defining XML structure");
        MIME_TYPE_DESCRIPTIONS.put(MIME_XML, "XML data (Extensible Markup Language)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TESTNG, "TestNG configuration XML (for TestNG testing framework)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_FXML, "FXML (JavaFX interface markup)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_RUST, "Rust code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TAG, "Custom tag file (Java-based custom JSP tags)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_PHP5, "PHP5 code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TWIG, "Twig template engine for PHP");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TPL, "PHP TPL template");
        MIME_TYPE_DESCRIPTIONS.put(MIME_LATTE, "Latte template engine for PHP");
        MIME_TYPE_DESCRIPTIONS.put(MIME_KO_DATA_BIND, "KnockoutJS data binding");
        MIME_TYPE_DESCRIPTIONS.put(MIME_TYPESCRIPT, "TypeScript code");
        MIME_TYPE_DESCRIPTIONS.put(MIME_ANT_XML, "Apache Ant build script XML");
        MIME_TYPE_DESCRIPTIONS.put(MIME_PLAIN_TEXT, "Plain text (no special formatting or markup)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_REPL, "REPL code (Read-Eval-Print Loop, interactive programming environment)");
        MIME_TYPE_DESCRIPTIONS.put(MIME_SHELL, "Shell script (commands for Unix/Linux shell environments)");
    }
}

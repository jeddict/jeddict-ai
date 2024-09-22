/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gaurav Gupta
 */
public class Snippet {

    List<String> imports = new ArrayList<>();

    String snippet;

    public Snippet(String snippet, List<String> imports) {
        this.snippet = snippet;
        this.imports = imports;
    }

    public List<String> getImports() {
        return imports;
    }

    public String getSnippet() {
        return snippet;
    }

}

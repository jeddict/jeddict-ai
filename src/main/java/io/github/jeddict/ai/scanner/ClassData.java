/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai.scanner;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Gaurav Gupta
 */
public class ClassData {

    private final String _package;
    private final String className;
    private final String classSignature;
    private Set<String> subtree;

    public ClassData(String _package, String className, String classSignature) {
        this._package = _package;
        this.className = className;
        this.classSignature = classSignature;
    }

    public boolean addSubTree(String e) {
        if (subtree == null) {
            subtree = new HashSet<>();
        }
        return subtree.add(e);
    }

    public String getPackage() {
        return _package;
    }

    public String getClassName() {
        return className;
    }

    public String getClassSignature() {
        return classSignature;
    }

    public Set<String> getSubtree() {
        return subtree;
    }

    @Override
    public String toString() {
        return classSignature;
    }

}

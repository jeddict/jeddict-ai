/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import org.netbeans.api.java.lexer.JavaTokenId;

/**
 *
 * @author Gaurav Gupta
 */
public class JavaToken {

    boolean javaContext;
    JavaTokenId id;
    int offset;

    public JavaToken(boolean javaContext, JavaTokenId id, int offset) {
        this.javaContext = javaContext;
        this.id = id;
        this.offset = offset;
    }

    public JavaToken(boolean javaContext) {
        this.javaContext = javaContext;
    }

    public boolean isJavaContext() {
        return javaContext;
    }

    public JavaTokenId getId() {
        return id;
    }

    public int getOffset() {
        return offset;
    }

}

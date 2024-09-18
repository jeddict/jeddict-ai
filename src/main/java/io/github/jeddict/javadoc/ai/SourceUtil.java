/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.javadoc.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.netbeans.modules.java.editor.imports.JavaFixAllImports;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Gaurav Gupta
 */
public class SourceUtil {
    public static void fixImports(FileObject fileObject) {
        try {
            // Ensure the fileObject is a Java file
            if (fileObject != null && "java".equals(fileObject.getExt())) {
                printSource(fileObject);
                // Get the default instance of JavaFixAllImports
                JavaFixAllImports fixAllImports = JavaFixAllImports.getDefault();
                
                // Call fixAllImports with the fileObject and a target (usually null)
                fixAllImports.fixAllImports(fileObject, null);
                printSource(fileObject);
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }
    
    
    public static void printSource(FileObject fileObject) {
        if (fileObject != null && "java".equals(fileObject.getExt())) {
            try (InputStream is = fileObject.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
        }
    }
}
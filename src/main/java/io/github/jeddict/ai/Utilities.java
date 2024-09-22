/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jeddict.ai;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
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
}

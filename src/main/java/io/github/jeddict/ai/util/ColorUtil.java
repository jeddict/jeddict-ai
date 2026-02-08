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

/**
 *
 * @author Gaurav Gupta
 */
public class ColorUtil {    // Lighten the color by a given percentage
    public static Color lighten(Color original, float percentage) {
        float[] hsb = Color.RGBtoHSB(original.getRed(), original.getGreen(), original.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + percentage));  // Increase brightness
    }

    // Darken the color by a given percentage
    public static Color darken(Color original, float percentage) {
        float[] hsb = Color.RGBtoHSB(original.getRed(), original.getGreen(), original.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.max(0f, hsb[2] - percentage));  // Decrease brightness
    }

    public static boolean isDarkColor(Color color) {
        double luminance = 0.2126 * color.getRed() / 255
                + 0.7152 * color.getGreen() / 255
                + 0.0722 * color.getBlue() / 255;
        return luminance < 0.5; // Threshold: lower = darker
    }

    public static String web(final Color color) {
        return String.format(
            "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()
        );
    }
}

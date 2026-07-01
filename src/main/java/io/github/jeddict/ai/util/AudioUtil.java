/**
 * Copyright 2025-26 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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

import io.github.jeddict.ai.settings.PreferencesManager;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.openide.util.Exceptions;

/**
 * Utility class to play audio notification when communication with LLM ends.
 *
 * @author Gaurav Gupta
 */
public class AudioUtil {

    private static final Logger LOG = Logger.getLogger(AudioUtil.class.getName());

    public static void playNotificationSound() {
        playNotificationSound(PreferencesManager.getInstance().getPlaySoundFile());
    }

    public static void playNotificationSound(final String soundFile) {
        new Thread(() -> {
            try {
                String resourcePath = "/sounds/" + soundFile;
                try (InputStream is = AudioUtil.class.getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        throw new IllegalArgumentException("Sound file not found: " + resourcePath);
                    }
                    try (InputStream bufferedIn = new BufferedInputStream(is);
                        AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn)) {
                        AudioFormat format = audioIn.getFormat();
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                            line.open(format);
                            line.start();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = audioIn.read(buffer)) != -1) {
                                line.write(buffer, 0, bytesRead);
                            }
                            line.drain();
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.severe("failed to play wav sound: " + soundFile + ", falling back to system beep");
                try {
                    Toolkit.getDefaultToolkit().beep();
                } catch (Throwable t2) {
                    LOG.severe("failed to play system beep");
                }
                Exceptions.printStackTrace(t);
            }
        }, "JeddictAISoundNotifier").start();
    }
}

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
package io.github.jeddict.ai.settings;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javax.swing.JPanel;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

public class AIAssistantPanelFX extends JPanel {

    final private Logger log = Logger.getLogger(getClass().getName());

    private JFXPanel jfxPanel;
    private AIAssistantPanelController fxController;
    private final AIAssistantSettingsControllerFX optionsController;

    private final CountDownLatch latch = new CountDownLatch(1);

    public AIAssistantPanelFX(final AIAssistantSettingsControllerFX optionsController) {
        this.optionsController = optionsController;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        Platform.setImplicitExit(false);
        Platform.runLater(this::createScene);
    }

    private void createScene() {
        log.info("createScene");
        try {
            ResourceBundle bundle = NbBundle.getBundle("io.github.jeddict.ai.settings.Bundle");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AIAssistantSettings.fxml"), bundle);
            Parent root = loader.load();
            fxController = loader.getController();
            log.info("fxController: " + fxController);

            fxController.setOptionsController(optionsController);

            Scene scene = new Scene(root);
            // Theme synchronization could be added here if there's a common CSS
            jfxPanel.setScene(scene);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        latch.countDown();
    }

    void load() {
        checkFxController();
        Platform.runLater(() -> {
            fxController.load();
        });
    }

    void store() {
        checkFxController();
        Platform.runLater(() -> {
            fxController.store();
        });
    }

    boolean valid() {
        checkFxController();
        return fxController.valid();
    }

    private void checkFxController() {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch(InterruptedException x) {
            log.info("ups, not enough time...");
        }
        if (fxController == null) {
            final String msg = "fxController is null, this is a bug!";
            log.severe(msg);
            throw new IllegalStateException(msg);
        }
    }
}

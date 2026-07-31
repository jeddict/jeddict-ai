/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.GPT4AllModelFetcher;
import io.github.jeddict.ai.models.GroqModelFetcher;
import io.github.jeddict.ai.models.LMStudioModelFetcher;
import io.github.jeddict.ai.models.OllamaModelFetcher;
import io.github.jeddict.ai.models.registry.GenAIModel;
import io.github.jeddict.ai.models.registry.GenAIModelRegistry;
import io.github.jeddict.ai.models.registry.GenAIProvider;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.RequestProcessor;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

/**
 *
 */
public class ModelUpdaterDialog {

    public static final String TITLE = "Select Models to Add";

    private final Logger LOG = Logger.getLogger(getClass().getName());

    private Window window;

    private final GenAIProvider selectedProvider;
    private final ObservableList<GenAIModel> currentModels;
    private final String providerEndpoint;

    private static final String DEFAULT_COPILOT_PROVIDER_LOCATION = "http://localhost:4141/v1";

    public ModelUpdaterDialog(
        final Window window,
        final GenAIProvider selectedProvider,
        final String providerEndpoint,
        final ObservableList<GenAIModel> currentModels
    ) {
        this.window = window;
        this.selectedProvider = selectedProvider;
        this.providerEndpoint = providerEndpoint;
        this.currentModels = currentModels;
    }

    public void updateModels() {
        final PreferencesManager pm = PreferencesManager.getInstance();

        ProgressHandle ph = ProgressHandle.createHandle("Fetching available AI models...");
        ph.start();
        RequestProcessor.getDefault().post(() -> {
            LinkedHashMap<String, GenAIModel> availableModels = getModelListTable(selectedProvider);
            ph.finish();
            SwingUtilities.invokeLater(() -> {
                // Creare il dialog
                Window parent = SwingUtilities.getWindowAncestor(window);
                JDialog modelSelectionDialog = new JDialog(parent, TITLE, Dialog.ModalityType.APPLICATION_MODAL);
                modelSelectionDialog.setLayout(new BorderLayout());
                modelSelectionDialog.setSize(800, 600);
                modelSelectionDialog.setLocationRelativeTo(window);

                // Creare il modello della tabella
                String[] columnNames = {"Select", "Name", "Description", "Input Price/M", "Output Price/M"};
                DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        if (columnIndex == 0) {
                            return Boolean.class;
                        }
                        if (columnIndex == 3 || columnIndex == 4) {
                            return Double.class;
                        }
                        return String.class;
                    }

                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return column == 0; // Solo la colonna di selezione è editabile
                    }
                };

                // Popolare la tabella con i modelli disponibili
                for (GenAIModel model : availableModels.values()) {
                    // Verificare se il modello è già presente nella combobox
                    Boolean alreadyInComboBox = on(currentModels).loop((m) -> {
                        if (m.fullName().equals(model.fullName())) {
                            _break_(true);
                        }
                    });

                    // Aggiungere alla tabella con selezione predefinita se non è già presente
                    tableModel.addRow(new Object[]{
                        alreadyInComboBox, // Selezionare solo i modelli presenti
                        model.fullName(),
                        model.description(),
                        model.inputPrice() * 1000000,
                        model.outputPrice() * 1000000
                    });
                }

                // Creazione della JTable con tooltip personalizzato
                JTable modelsTable = new JTable(tableModel) {
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int row = rowAtPoint(p);
                        int col = columnAtPoint(p);

                        if (row >= 0 && col >= 0) {
                            if (col == 2) { // Descrizione
                                Object value = getValueAt(row, col);
                                return (value != null) ? value.toString() : null;
                            }
                        }
                        return super.getToolTipText(e);
                    }

                    @Override
                    public Point getToolTipLocation(MouseEvent e) {
                        return new Point(20, -20);
                    }
                };

                // AGGIUNTA DELL'ORDINAMENTO PER LE COLONNE
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
                modelsTable.setRowSorter(sorter);

                // Comparatore per le colonne numeriche (prezzi)
                sorter.setComparator(3, Comparator.comparingDouble(value -> (Double) value));
                sorter.setComparator(4, Comparator.comparingDouble(value -> (Double) value));

                modelsTable.setPreferredScrollableViewportSize(new Dimension(750, 400));
                modelsTable.setFillsViewportHeight(true);
                modelsTable.setAutoCreateRowSorter(true);

                // Impostare la larghezza delle colonne
                modelsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
                modelsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
                modelsTable.getColumnModel().getColumn(2).setPreferredWidth(350);
                modelsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
                modelsTable.getColumnModel().getColumn(4).setPreferredWidth(80);

                JScrollPane scrollPane = new JScrollPane(modelsTable);

                // Pannello per i pulsanti
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton selectAllButton = new JButton("Select All");
                JButton deselectAllButton = new JButton("Deselect All");
                JButton addSelectedButton = new JButton("Add Selected Models");
                JButton cancelButton = new JButton("Cancel");

                buttonPanel.add(selectAllButton);
                buttonPanel.add(deselectAllButton);
                buttonPanel.add(addSelectedButton);
                buttonPanel.add(cancelButton);

                // Aggiungere i componenti al dialog
                modelSelectionDialog.add(scrollPane, BorderLayout.CENTER);
                modelSelectionDialog.add(buttonPanel, BorderLayout.SOUTH);

                // Gestire gli eventi dei pulsanti
                selectAllButton.addActionListener(ev -> {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        tableModel.setValueAt(true, i, 0);
                    }
                });

                deselectAllButton.addActionListener(ev -> {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        tableModel.setValueAt(false, i, 0);
                    }
                });

                addSelectedButton.addActionListener(ev -> {
                    List<GenAIModel> selectedModels = new ArrayList<>();
                    Map<String, List<GenAIModel>> modelsByProvider = new HashMap<>();

                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
                        if (selected != null && selected) {
                            String modelName = (String) tableModel.getValueAt(i, 1);
                            GenAIModel model = availableModels.get(modelName);
                            if (model != null) {
                                selectedModels.add(model);

                                // Raggruppare per provider
                                String providerName = model.provider().name();
                                modelsByProvider.computeIfAbsent(providerName, k -> new ArrayList<>()).add(model);
                            }
                        }
                    }

                    if (selectedModels.isEmpty()) {
                        JOptionPane.showMessageDialog(modelSelectionDialog,
                            "Please select at least one model to add.",
                            "No Selection", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Save models for each providers
                    for (Map.Entry<String, List<GenAIModel>> entry : modelsByProvider.entrySet()) {
                        List<GenAIModel> existingModels = pm.getGenAIModelList(entry.getKey());
                        if (existingModels == null) {
                            existingModels = new ArrayList<>();
                        }

                        // Add not already present models
                        for (GenAIModel newModel : entry.getValue()) {
                            boolean alreadyExists = existingModels.stream()
                                .anyMatch(model -> model.fullName().equalsIgnoreCase(newModel.fullName()));

                            if (!alreadyExists) {
                                existingModels.add(newModel);
                                currentModels.add(newModel);
                            }
                        }

                        pm.setGenAIModelList(existingModels, entry.getKey());
                    }

                    JOptionPane.showMessageDialog(modelSelectionDialog,
                        "Added " + selectedModels.size() + " models successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                    modelSelectionDialog.dispose();
                });

                cancelButton.addActionListener(ev -> modelSelectionDialog.dispose());

                modelSelectionDialog.setVisible(true);
            }); // close SwingUtilities.invokeLater
        }); // close RequestProcessor.post
    }

    private LinkedHashMap<String, GenAIModel> getModelListTable(GenAIProvider selectedProvider) {
        // TODO Sostituire con la ripresa dal file json
        final LinkedHashMap<String, GenAIModel> models = new LinkedHashMap<>();
        try {
            if (selectedProvider == GenAIProvider.OLLAMA
                    && !providerEndpoint.isEmpty()) {
                OllamaModelFetcher fetcher = new OllamaModelFetcher();
                List<String> strModels = fetcher.fetchModelNames(providerEndpoint);
                strModels.forEach((model) -> {
                    models.put(model, new GenAIModel(selectedProvider, model, model, 0, 0));
                });
                // Aggiunge tutti i MODELS esistenti alla lista
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
            } else if (selectedProvider == GenAIProvider.LM_STUDIO
                    && !providerEndpoint.isEmpty()) {
                LMStudioModelFetcher fetcher = new LMStudioModelFetcher();
                List<String> strModels = fetcher.fetchModelNames(providerEndpoint);
                strModels.forEach((model) -> {
                    models.put(model, new GenAIModel(selectedProvider, model, model, 0, 0));
                });
                // Aggiunge tutti i MODELS esistenti alla lista
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
            } else if (selectedProvider == GenAIProvider.GPT4ALL
                    && !providerEndpoint.isEmpty()) {
                GPT4AllModelFetcher fetcher = new GPT4AllModelFetcher();
                models.putAll(fetcher.fetchGenAIModels(providerEndpoint));
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
            } else if (selectedProvider == GenAIProvider.COPILOT_PROXY) {
                GPT4AllModelFetcher fetcher = new GPT4AllModelFetcher();
                models.putAll(fetcher.fetchGenAIModels(DEFAULT_COPILOT_PROVIDER_LOCATION));
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
            } else if (selectedProvider == GenAIProvider.GROQ
                    && !providerEndpoint.isEmpty()) {
                GroqModelFetcher fetcher = new GroqModelFetcher();
                List<String> strModels = fetcher.fetchModels(providerEndpoint, null/* do we really need the key?*/);
                strModels.forEach((model) -> {
                    models.put(model, new GenAIModel(selectedProvider, model, model, 0, 0));
                });
                // Aggiunge tutti i MODELS esistenti alla lista
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
            } else if (selectedProvider == GenAIProvider.CUSTOM_OPEN_AI) {
                GenAIModelRegistry fetcher = new GenAIModelRegistry();
                LinkedHashMap<String, GenAIModel> apiModels = fetcher.fetchGenAIModels(providerEndpoint);
                // Prima aggiungi i modelli predefiniti
                models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(), selectedProvider.name()));

                // Poi aggiungi i modelli dall'API (che sovrascrivono i duplicati)
                models.putAll(apiModels);
            }
        } catch (IOException x) {
            LOG.info("failed to retrieve modeles " + x);
        }

        if (models.isEmpty()) {
            models.putAll(getModelsByProvider(GenAIModelRegistry.getModels(),selectedProvider.name()));
        }

        // Ordina la mappa alfabeticamente per chiave
        /*
        Map<String, GenAIModel> sortedModels = new LinkedHashMap<>();
        models.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> sortedModels.put(entry.getKey(), entry.getValue()));*/

        return models;
    }

    private LinkedHashMap<String, GenAIModel> getModelsByProvider(Map<String, GenAIModel> models, String provider) {
        LinkedHashMap<String, GenAIModel> filteredModels = new LinkedHashMap<>();
        for (Map.Entry<String, GenAIModel> entry : models.entrySet()) {
            GenAIModel model = entry.getValue();
            if (model.provider().name().equals(provider)) {
                filteredModels.put(entry.getKey(), model);
            }
        }
        return filteredModels;
    }
}

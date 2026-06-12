package io.github.jeddict.ai.settings;

import io.github.jeddict.ai.models.registry.GenAIProvider;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

public class ModelManagerModel {
    private final ListProperty<GenAIProvider> providers = new SimpleListProperty<>(FXCollections.observableArrayList(GenAIProvider.values()));
    private final ObjectProperty<GenAIProvider> provider = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final DoubleProperty inputPrice = new SimpleDoubleProperty(0.0);
    private final DoubleProperty outputPrice = new SimpleDoubleProperty(0.0);

    public ListProperty<GenAIProvider> providersProperty() { return providers; }
    public ObjectProperty<GenAIProvider> providerProperty() { return provider; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty inputPriceProperty() { return inputPrice; }
    public DoubleProperty outputPriceProperty() { return outputPrice; }

    public void reset() {
        name.set(""); description.set("");
        inputPrice.set(0.0); outputPrice.set(0.0);
    }
}

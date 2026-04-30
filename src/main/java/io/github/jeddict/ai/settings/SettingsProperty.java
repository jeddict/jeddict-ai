/**
 * Copyright 2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;

/**
 * A property representing a map where individual keys are stored as JavaFX properties.
 */
public class SettingsProperty extends SimpleMapProperty<String, Property<?>> {

    public SettingsProperty() {
        super(FXCollections.observableHashMap());
    }

    public StringProperty string(String key) {
        return string(key, "");
    }

    public StringProperty string(String key, String defaultValue) {
        return (StringProperty) computeIfAbsent(key, k -> init(new SimpleStringProperty(defaultValue)));
    }

    public BooleanProperty bool(String key) {
        return bool(key, false);
    }

    public BooleanProperty bool(String key, boolean defaultValue) {
        return (BooleanProperty) computeIfAbsent(key, k -> init(new SimpleBooleanProperty(defaultValue)));
    }

    public IntegerProperty integer(String key) {
        return integer(key, 0);
    }

    public IntegerProperty integer(String key, int defaultValue) {
        return (IntegerProperty) computeIfAbsent(key, k -> init(new SimpleIntegerProperty(defaultValue)));
    }

    public DoubleProperty decimal(String key) {
        return decimal(key, 0.0);
    }

    public DoubleProperty decimal(String key, double defaultValue) {
        return (DoubleProperty) computeIfAbsent(key, k -> init(new SimpleDoubleProperty(defaultValue)));
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectProperty<T> object(String key) {
        return object(key, null);
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectProperty<T> object(String key, T defaultValue) {
        return (ObjectProperty<T>) computeIfAbsent(key, k -> init(new SimpleObjectProperty<>(defaultValue)));
    }

    /**
     * Set a value in the map. If a property already exists for the key, its value
     * is updated. If not, a new Property of the appropriate type is created.
     */
    public void set(final String key, final Object value) {
        Property p = super.get(key);
        if (p != null) {
            p.setValue(value);
        } else {
            Property newProperty;
            if (value instanceof String string) {
                newProperty = new SimpleStringProperty(string);
            } else if (value instanceof Boolean aBoolean) {
                newProperty = new SimpleBooleanProperty(aBoolean);
            } else if (value instanceof Integer integer) {
                newProperty = new SimpleIntegerProperty(integer);
            } else if (value instanceof Long aLong) {
                newProperty = new SimpleLongProperty(aLong);
            } else if (value instanceof Double aDouble) {
                newProperty = new SimpleDoubleProperty(aDouble);
            } else if (value instanceof Float aFloat) {
                newProperty = new SimpleFloatProperty(aFloat);
            } else {
                newProperty = new SimpleObjectProperty<>(value);
            }
            put(key, init(newProperty));
        }
    }

    /**
     * Get the value of a property in the map.
     */
    public Object getValue(String key) {
        Property<?> p = super.get(key);
        return p != null ? p.getValue() : null;
    }

    @Override
    public Property<?> put(String key, Property<?> value) {
        Property<?> existing = super.put(key, value);
        if (value != null) {
            init(value);
        }
        return existing;
    }

    private <T extends Property<?>> T init(T p) {
        final ChangeListener<Object> listener = (obs, old, newValue) -> {
            // fireValueChangedEvent() informs SimpleMapProperty listeners
            // that the value (the map) has changed internally.
            fireValueChangedEvent();
        };
        p.addListener(listener);
        return p;
    }
}

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

import static org.assertj.core.api.BDDAssertions.then;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ste.commons.javafx.property.IntegerProperty;

public class SettingsPropertyTest {

    private SettingsProperty mapProperty;

    @BeforeEach
    void setUp() {
        mapProperty = new SettingsProperty();
    }

    @Test
    void string_property_updates_map() {
        StringProperty property = mapProperty.string("myKey", "defaultVal");
        then(property.get()).isEqualTo("defaultVal");
        then(mapProperty.getValue("myKey")).isEqualTo("defaultVal");

        property.set("newVal");
        then(mapProperty.getValue("myKey")).isEqualTo("newVal");
    }

    @Test
    void map_updates_string_property() {
        StringProperty property = mapProperty.string("myKey", "defaultVal");
        mapProperty.set("myKey", "updatedByMap");
        then(property.get()).isEqualTo("updatedByMap");
    }

    @Test
    void multiple_string_properties_are_independent() {
        StringProperty p1 = mapProperty.string("key1", "v1");
        StringProperty p2 = mapProperty.string("key2", "v2");

        p1.set("newV1");
        then(mapProperty.getValue("key1")).isEqualTo("newV1");
        then(mapProperty.getValue("key2")).isEqualTo("v2");
        then(p2.get()).isEqualTo("v2");

        mapProperty.set("key2", "newV2");
        then(p2.get()).isEqualTo("newV2");
        then(p1.get()).isEqualTo("newV1");
    }

    @Test
    void boolean_property_updates_map() {
        BooleanProperty property = mapProperty.bool("myKey", true);
        then(property.get()).isTrue();
        then(mapProperty.getValue("myKey")).isEqualTo(true);

        property.set(false);
        then(mapProperty.getValue("myKey")).isEqualTo(false);
    }

    @Test
    void map_updates_boolean_property() {
        BooleanProperty property = mapProperty.bool("myKey", true);
        mapProperty.set("myKey", false);
        then(property.get()).isFalse();
    }

    @Test
    void multiple_boolean_properties_are_independent() {
        BooleanProperty p1 = mapProperty.bool("key1", true);
        BooleanProperty p2 = mapProperty.bool("key2", false);

        p1.set(false);
        then(mapProperty.getValue("key1")).isEqualTo(false);
        then(p2.get()).isFalse();

        mapProperty.set("key2", true);
        then(p2.get()).isTrue();
        then(p1.get()).isFalse();
    }

    @Test
    void integer_property_updates_map() {
        IntegerProperty property = mapProperty.integer("myKey", 10);
        then(property.get()).isEqualTo(10);
        then(mapProperty.getValue("myKey")).isEqualTo(10);

        property.set(20);
        then(mapProperty.getValue("myKey")).isEqualTo(20);
    }

    @Test
    void map_updates_integer_property() {
        IntegerProperty property = mapProperty.integer("myKey", 10);
        mapProperty.set("myKey", 30);
        then(property.get()).isEqualTo(30);
    }

    @Test
    void object_property_updates_map() {
        ObjectProperty<Object> property = mapProperty.object("myKey", "val");
        then(property.get()).isEqualTo("val");
        then(mapProperty.getValue("myKey")).isEqualTo("val");

        property.set("newVal");
        then(mapProperty.getValue("myKey")).isEqualTo("newVal");
    }

    @Test
    void map_updates_object_property() {
        ObjectProperty<Object> property = mapProperty.object("myKey", "val");
        mapProperty.set("myKey", "updatedByMap");
        then(property.get()).isEqualTo("updatedByMap");
    }

    @Test
    void set_creates_correct_property_type() {
        mapProperty.set("stringKey", "stringVal");
        then(mapProperty.get("stringKey")).isExactlyInstanceOf(SimpleStringProperty.class);

        mapProperty.set("booleanKey", true);
        then(mapProperty.get("booleanKey")).isExactlyInstanceOf(SimpleBooleanProperty.class);

        mapProperty.set("integerKey", 123);
        then(mapProperty.get("integerKey")).isExactlyInstanceOf(IntegerProperty.class);

        mapProperty.set("doubleKey", 1.23);
        then(mapProperty.get("doubleKey")).isExactlyInstanceOf(SimpleDoubleProperty.class);

        mapProperty.set("objectKey", new Object());
        then(mapProperty.get("objectKey")).isExactlyInstanceOf(SimpleObjectProperty.class);
    }

    @Test
    void map_emits_events_when_linked_property_updated() {
        StringProperty property = mapProperty.string("myKey", "initial");
        final boolean[] notified = {false};
        mapProperty.addListener((javafx.beans.InvalidationListener) obs -> {
            notified[0] = true;
        });

        property.set("newValue");
        then(notified[0]).isTrue();
    }

    @Test
    void multiple_calls_for_same_key_return_same_instance() {
        StringProperty p1 = mapProperty.string("myKey");
        StringProperty p2 = mapProperty.string("myKey");

        then(p1).isSameAs(p2);

        p1.set("newValue");
        then(p2.get()).isEqualTo("newValue");
    }

    @Test
    void list_property_supports_type_inference_for_string() {
        ObservableList<String> values = mapProperty.list("listKey");

        values.add("alpha");
        values.add("beta");

        then(values).containsExactly("alpha", "beta");
    }

    @Test
    void list_property_supports_type_inference_for_integer() {
        ListProperty<Integer> values = mapProperty.list("intListKey");

        values.add(10);
        values.add(20);

        then(values).containsExactly(10, 20);
    }

    @Test
    void list_multiple_calls_for_same_key_return_same_instance() {
        ObservableList<String> first = mapProperty.list("sameListKey");
        ObservableList<String> second = mapProperty.list("sameListKey");

        then(first).isSameAs(second);
    }
}

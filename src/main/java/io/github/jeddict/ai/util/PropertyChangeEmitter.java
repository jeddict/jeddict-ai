package io.github.jeddict.ai.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A helper interface that provides default implementations for managing
 * {@link PropertyChangeListener}s using a {@link PropertyChangeSupport} instance.
 * This interface allows classes to easily become property change emitters without
 * needing to declare and manage their own {@link PropertyChangeSupport} field.
 * <p>
 * The {@link PropertyChangeSupport} instance for each implementing object is
 * stored in a static {@link WeakHashMap} to prevent memory leaks.
 * </p>
 */
public interface PropertyChangeEmitter {

    /**
     * A static {@link WeakHashMap} that stores {@link PropertyChangeSupport} instances
     * for each object implementing this interface.
     * The keys are the {@link PropertyChangeEmitter} instances, and the values are
     * their corresponding {@link PropertyChangeSupport} objects.
     * Using a {@link WeakHashMap} ensures that entries are automatically removed
     * when the {@link PropertyChangeEmitter} instance is no longer strongly referenced,
     * preventing memory leaks.
     */
    static final Map<PropertyChangeEmitter, PropertyChangeSupport> SUPPORT_MAP = new WeakHashMap<>();

    /**
     * Returns the {@link PropertyChangeSupport} instance associated with this object.
     * If no {@link PropertyChangeSupport} instance exists for this object, a new one
     * is created and stored in the {@link #SUPPORT_MAP}.
     *
     * @return The {@link PropertyChangeSupport} instance for this object.
     */
    default PropertyChangeSupport getSupport() {
        return SUPPORT_MAP.computeIfAbsent(this, PropertyChangeSupport::new);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener The {@link PropertyChangeListener} to be added.
     */
    default void addPropertyChangeListener(PropertyChangeListener listener) {
        getSupport().addPropertyChangeListener(listener);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list for a specific property.
     *
     * @param propertyName The name of the property to listen on.
     * @param listener The {@link PropertyChangeListener} to be added.
     */
    default void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getSupport().addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     * This method should be used to remove {@link PropertyChangeListener}s that were registered
     * for all properties.
     *
     * @param listener The {@link PropertyChangeListener} to be removed.
     */
    default void removePropertyChangeListener(PropertyChangeListener listener) {
        getSupport().removePropertyChangeListener(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} from the listener list for a specific property.
     *
     * @param propertyName The name of the property that was listened on.
     * @param listener The {@link PropertyChangeListener} to be removed.
     */
    default void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getSupport().removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Fires a property change event to all registered listeners.
     * No event is fired if old and new values are equal and non-null.
     *
     * @param propertyName The programmatic name of the property that was changed.
     * @param oldValue The old value of the property.
     * @param newValue The new value of the property.
     */
    default void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getSupport().firePropertyChange(propertyName, oldValue, newValue);
    }
}
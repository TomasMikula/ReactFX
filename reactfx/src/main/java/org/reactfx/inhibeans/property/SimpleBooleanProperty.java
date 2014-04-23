package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleBooleanProperty}.
 */
public class SimpleBooleanProperty
extends javafx.beans.property.SimpleBooleanProperty
implements Property<Boolean> {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public Guard block() {
        if(blocked) {
            return Guard.EMPTY_GUARD;
        } else {
            blocked = true;
            return this::release;
        }
    }

    private void release() {
        blocked = false;
        if(fireOnRelease) {
            fireOnRelease = false;
            super.fireValueChangedEvent();
        }
    }

    @Override
    protected void fireValueChangedEvent() {
        if(blocked)
            fireOnRelease = true;
        else
            super.fireValueChangedEvent();
    }


    /********************************
     *** Superclass constructors. ***
     ********************************/

    public SimpleBooleanProperty() {
        super();
    }

    public SimpleBooleanProperty(boolean initialValue) {
        super(initialValue);
    }

    public SimpleBooleanProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleBooleanProperty(Object bean, String name, boolean initialValue) {
        super(bean, name, initialValue);
    }
}

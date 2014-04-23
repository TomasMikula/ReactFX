package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleFloatProperty}.
 */
public class SimpleFloatProperty
extends javafx.beans.property.SimpleFloatProperty
implements Property<Number> {

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

    public SimpleFloatProperty() {
        super();
    }

    public SimpleFloatProperty(float initialValue) {
        super(initialValue);
    }

    public SimpleFloatProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleFloatProperty(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    }
}

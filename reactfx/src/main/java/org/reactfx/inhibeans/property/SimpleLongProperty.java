package org.reactfx.inhibeans.property;

import org.reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleLongProperty}.
 */
public class SimpleLongProperty
extends javafx.beans.property.SimpleLongProperty
implements Property<Number> {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public Hold block() {
        if(blocked) {
            return Hold.EMPTY_HOLD;
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

    public SimpleLongProperty() {
        super();
    }

    public SimpleLongProperty(long initialValue) {
        super(initialValue);
    }

    public SimpleLongProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleLongProperty(Object bean, String name, long initialValue) {
        super(bean, name, initialValue);
    }
}

package org.reactfx.inhibeans.property;

import org.reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyFloatWrapper}.
 */
public class ReadOnlyFloatWrapper
extends javafx.beans.property.ReadOnlyFloatWrapper
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

    public ReadOnlyFloatWrapper() {
        super();
    }

    public ReadOnlyFloatWrapper(float initialValue) {
        super(initialValue);
    }

    public ReadOnlyFloatWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyFloatWrapper(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    };
}

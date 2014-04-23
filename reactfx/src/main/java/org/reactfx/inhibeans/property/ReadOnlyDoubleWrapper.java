package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyDoubleWrapper}.
 */
public class ReadOnlyDoubleWrapper
extends javafx.beans.property.ReadOnlyDoubleWrapper
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

    public ReadOnlyDoubleWrapper() {
        super();
    }

    public ReadOnlyDoubleWrapper(double initialValue) {
        super(initialValue);
    }

    public ReadOnlyDoubleWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyDoubleWrapper(Object bean, String name,
            double initialValue) {
        super(bean, name, initialValue);
    };
}

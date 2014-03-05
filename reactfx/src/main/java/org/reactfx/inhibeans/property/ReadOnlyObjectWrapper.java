package org.reactfx.inhibeans.property;

import org.reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyObjectWrapper}.
 */
public class ReadOnlyObjectWrapper<T>
extends javafx.beans.property.ReadOnlyObjectWrapper<T>
implements Property<T> {

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

    public ReadOnlyObjectWrapper() {
        super();
    }

    public ReadOnlyObjectWrapper(T initialValue) {
        super(initialValue);
    }

    public ReadOnlyObjectWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyObjectWrapper(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    };
}

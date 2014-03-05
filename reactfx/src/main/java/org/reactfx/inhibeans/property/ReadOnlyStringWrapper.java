package org.reactfx.inhibeans.property;

import org.reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyStringWrapper}.
 */
public class ReadOnlyStringWrapper
extends javafx.beans.property.ReadOnlyStringWrapper
implements Property<String> {

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

    public ReadOnlyStringWrapper() {
        super();
    }

    public ReadOnlyStringWrapper(String initialValue) {
        super(initialValue);
    }

    public ReadOnlyStringWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyStringWrapper(Object bean, String name,
            String initialValue) {
        super(bean, name, initialValue);
    };
}

package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyStringWrapper}.
 */
public class ReadOnlyStringWrapper
extends javafx.beans.property.ReadOnlyStringWrapper
implements Property<String> {

    private int blocked = 0;
    private boolean fireOnRelease = false;

    @Override
    public Guard block() {
        ++blocked;
        return ((Guard) this::release).closeableOnce();
    }

    private void release() {
        assert blocked > 0;
        if(--blocked == 0 && fireOnRelease) {
            fireOnRelease = false;
            super.fireValueChangedEvent();
        }
    }

    @Override
    protected void fireValueChangedEvent() {
        if(blocked > 0) {
            fireOnRelease = true;
        } else {
            super.fireValueChangedEvent();
        }
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

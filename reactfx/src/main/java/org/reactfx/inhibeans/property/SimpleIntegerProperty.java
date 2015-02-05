package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleIntegerProperty}.
 */
@Deprecated
public class SimpleIntegerProperty
extends javafx.beans.property.SimpleIntegerProperty
implements Property<Number> {

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

    public SimpleIntegerProperty() {
        super();
    }

    public SimpleIntegerProperty(int initialValue) {
        super(initialValue);
    }

    public SimpleIntegerProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleIntegerProperty(Object bean, String name, int initialValue) {
        super(bean, name, initialValue);
    }
}

package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleDoubleProperty}.
 */
public class SimpleDoubleProperty
extends javafx.beans.property.SimpleDoubleProperty
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

    public SimpleDoubleProperty() {
        super();
    }

    public SimpleDoubleProperty(double initialValue) {
        super(initialValue);
    }

    public SimpleDoubleProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleDoubleProperty(Object bean, String name, double initialValue) {
        super(bean, name, initialValue);
    }
}

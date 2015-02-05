package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleObjectProperty}.
 */
@Deprecated
public class SimpleObjectProperty<T>
extends javafx.beans.property.SimpleObjectProperty<T>
implements Property<T> {

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

    public SimpleObjectProperty() {
        super();
    }

    public SimpleObjectProperty(T initialValue) {
        super(initialValue);
    }

    public SimpleObjectProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleObjectProperty(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }
}
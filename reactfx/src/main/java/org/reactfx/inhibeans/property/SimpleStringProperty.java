package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleStringProperty}.
 */
public class SimpleStringProperty
extends javafx.beans.property.SimpleStringProperty
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

    public SimpleStringProperty() {
        super();
    }

    public SimpleStringProperty(String initialValue) {
        super(initialValue);
    }

    public SimpleStringProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleStringProperty(Object bean, String name, String initialValue) {
        super(bean, name, initialValue);
    }
}

package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleBooleanProperty}.
 */
@Deprecated
public class SimpleBooleanProperty
extends javafx.beans.property.SimpleBooleanProperty
implements Property<Boolean> {

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

    public SimpleBooleanProperty() {
        super();
    }

    public SimpleBooleanProperty(boolean initialValue) {
        super(initialValue);
    }

    public SimpleBooleanProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleBooleanProperty(Object bean, String name, boolean initialValue) {
        super(bean, name, initialValue);
    }
}

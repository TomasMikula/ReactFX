package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyObjectWrapper}.
 */
@Deprecated
public class ReadOnlyObjectWrapper<T>
extends javafx.beans.property.ReadOnlyObjectWrapper<T>
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

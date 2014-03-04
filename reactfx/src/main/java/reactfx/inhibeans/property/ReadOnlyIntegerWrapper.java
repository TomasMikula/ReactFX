package reactfx.inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyIntegerWrapper}.
 */
public class ReadOnlyIntegerWrapper
extends javafx.beans.property.ReadOnlyIntegerWrapper
implements Property<Number> {

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

    public ReadOnlyIntegerWrapper() {
        super();
    }

    public ReadOnlyIntegerWrapper(int initialValue) {
        super(initialValue);
    }

    public ReadOnlyIntegerWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyIntegerWrapper(Object bean, String name, int initialValue) {
        super(bean, name, initialValue);
    };
}

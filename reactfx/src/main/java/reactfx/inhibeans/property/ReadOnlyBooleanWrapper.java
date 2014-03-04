package reactfx.inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyBooleanWrapper}.
 */
public class ReadOnlyBooleanWrapper
extends javafx.beans.property.ReadOnlyBooleanWrapper
implements Property<Boolean> {

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

    public ReadOnlyBooleanWrapper() {
        super();
    }

    public ReadOnlyBooleanWrapper(boolean initialValue) {
        super(initialValue);
    }

    public ReadOnlyBooleanWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyBooleanWrapper(Object bean, String name,
            boolean initialValue) {
        super(bean, name, initialValue);
    };
}

package inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.IntegerPropertyBase}.
 */
public abstract class IntegerPropertyBase
extends javafx.beans.property.IntegerPropertyBase
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

    public IntegerPropertyBase() {
        super();
    }

    public IntegerPropertyBase(int initialValue) {
        super(initialValue);
    }
}

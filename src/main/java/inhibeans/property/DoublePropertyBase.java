package inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.DoublePropertyBase}.
 */
public abstract class DoublePropertyBase
extends javafx.beans.property.DoublePropertyBase
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

    public DoublePropertyBase() {
        super();
    }

    public DoublePropertyBase(double initialValue) {
        super(initialValue);
    }
}

package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.DoublePropertyBase}.
 */
public abstract class FloatPropertyBase
extends javafx.beans.property.FloatPropertyBase
implements InhibitoryProperty<Number> {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public Block block() {
        if(blocked) {
            return Block.EMPTY_BLOCK;
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

    public FloatPropertyBase() {
        super();
    }

    public FloatPropertyBase(float initialValue) {
        super(initialValue);
    }
}

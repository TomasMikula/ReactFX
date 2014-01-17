package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.StringPropertyBase}.
 */
public abstract class StringPropertyBase
extends javafx.beans.property.StringPropertyBase
implements InhibitoryProperty<String> {

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

    public StringPropertyBase() {
        super();
    }

    public StringPropertyBase(String initialValue) {
        super(initialValue);
    }
}

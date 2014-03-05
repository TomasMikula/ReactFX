package org.reactfx.inhibeans.property;

import org.reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.BooleanPropertyBase}.
 */
public abstract class BooleanPropertyBase
extends javafx.beans.property.BooleanPropertyBase
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

    public BooleanPropertyBase() {
        super();
    }

    public BooleanPropertyBase(boolean initialValue) {
        super(initialValue);
    }
}

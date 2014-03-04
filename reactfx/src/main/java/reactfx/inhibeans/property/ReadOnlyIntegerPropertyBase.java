package reactfx.inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyIntegerPropertyBase}.
 */
public abstract class ReadOnlyIntegerPropertyBase
extends javafx.beans.property.ReadOnlyIntegerPropertyBase
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
}

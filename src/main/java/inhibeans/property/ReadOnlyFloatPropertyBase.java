package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyFloatPropertyBase}.
 */
public abstract class ReadOnlyFloatPropertyBase
extends javafx.beans.property.ReadOnlyFloatPropertyBase
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
}

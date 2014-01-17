package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyLongWrapper}.
 */
public class ReadOnlyLongWrapper
extends javafx.beans.property.ReadOnlyLongWrapper
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

    public ReadOnlyLongWrapper() {
        super();
    }

    public ReadOnlyLongWrapper(long initialValue) {
        super(initialValue);
    }

    public ReadOnlyLongWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyLongWrapper(Object bean, String name, long initialValue) {
        super(bean, name, initialValue);
    };
}

package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyIntegerWrapper}.
 */
public class ReadOnlyIntegerWrapper
extends javafx.beans.property.ReadOnlyIntegerWrapper
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

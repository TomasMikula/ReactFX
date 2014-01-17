package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyDoubleWrapper}.
 */
public class ReadOnlyDoubleWrapper
extends javafx.beans.property.ReadOnlyDoubleWrapper
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

    public ReadOnlyDoubleWrapper() {
        super();
    }

    public ReadOnlyDoubleWrapper(double initialValue) {
        super(initialValue);
    }

    public ReadOnlyDoubleWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyDoubleWrapper(Object bean, String name,
            double initialValue) {
        super(bean, name, initialValue);
    };
}

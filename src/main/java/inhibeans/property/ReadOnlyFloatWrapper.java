package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyFloatWrapper}.
 */
public class ReadOnlyFloatWrapper
extends javafx.beans.property.ReadOnlyFloatWrapper
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

    public ReadOnlyFloatWrapper() {
        super();
    }

    public ReadOnlyFloatWrapper(float initialValue) {
        super(initialValue);
    }

    public ReadOnlyFloatWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyFloatWrapper(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    };
}

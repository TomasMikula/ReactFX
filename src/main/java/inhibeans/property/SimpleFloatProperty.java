package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleFloatProperty}.
 */
public class SimpleFloatProperty
extends javafx.beans.property.SimpleFloatProperty
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

    public SimpleFloatProperty() {
        super();
    }

    public SimpleFloatProperty(float initialValue) {
        super(initialValue);
    }

    public SimpleFloatProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleFloatProperty(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    }
}

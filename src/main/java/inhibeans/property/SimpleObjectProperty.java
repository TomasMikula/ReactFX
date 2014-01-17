package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleObjectProperty}.
 */
public class SimpleObjectProperty<T>
extends javafx.beans.property.SimpleObjectProperty<T>
implements InhibitoryProperty<T> {

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

    public SimpleObjectProperty() {
        super();
    }

    public SimpleObjectProperty(T initialValue) {
        super(initialValue);
    }

    public SimpleObjectProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleObjectProperty(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }
}
package inhibeans.property;

import inhibeans.Block;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleStringProperty}.
 */
public class SimpleStringProperty
extends javafx.beans.property.SimpleStringProperty
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

    public SimpleStringProperty() {
        super();
    }

    public SimpleStringProperty(String initialValue) {
        super(initialValue);
    }

    public SimpleStringProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleStringProperty(Object bean, String name, String initialValue) {
        super(bean, name, initialValue);
    }
}

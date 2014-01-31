package inhibeans.property;

import reactfx.Hold;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleDoubleProperty}.
 */
public class SimpleDoubleProperty
extends javafx.beans.property.SimpleDoubleProperty
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


    /********************************
     *** Superclass constructors. ***
     ********************************/

    public SimpleDoubleProperty() {
        super();
    }

    public SimpleDoubleProperty(double initialValue) {
        super(initialValue);
    }

    public SimpleDoubleProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleDoubleProperty(Object bean, String name, double initialValue) {
        super(bean, name, initialValue);
    }
}

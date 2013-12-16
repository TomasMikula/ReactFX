package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.DoublePropertyBase}.
 */
public abstract class DoublePropertyBase
extends javafx.beans.property.DoublePropertyBase
implements InhibitoryProperty<Number> {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public AutoCloseable block() {
        blocked = true;
        return this;
    }

    @Override
    public void release() {
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

    public DoublePropertyBase() {
        super();
    }

    public DoublePropertyBase(double initialValue) {
        super(initialValue);
    }
}

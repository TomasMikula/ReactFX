package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.IntegerPropertyBase}.
 */
public abstract class IntegerPropertyBase
extends javafx.beans.property.IntegerPropertyBase
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

    public IntegerPropertyBase() {
        super();
    }

    public IntegerPropertyBase(int initialValue) {
        super(initialValue);
    }
}

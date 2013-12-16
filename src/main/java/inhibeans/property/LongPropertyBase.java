package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.LongPropertyBase}.
 */
public abstract class LongPropertyBase
extends javafx.beans.property.LongPropertyBase
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

    public LongPropertyBase() {
        super();
    }

    public LongPropertyBase(long initialValue) {
        super(initialValue);
    }
}

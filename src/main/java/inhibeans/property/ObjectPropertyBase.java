package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ObjectPropertyBase}.
 */
public abstract class ObjectPropertyBase<T>
extends javafx.beans.property.ObjectPropertyBase<T>
implements InhibitoryProperty<T> {

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

    public ObjectPropertyBase() {
        super();
    }

    public ObjectPropertyBase(T initialValue) {
        super(initialValue);
    }
}

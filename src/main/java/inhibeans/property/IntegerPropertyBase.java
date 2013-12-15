package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.IntegerPropertyBase}.
 */
public abstract class IntegerPropertyBase extends javafx.beans.property.IntegerPropertyBase {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    public void block() {
        blocked = true;
    }

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

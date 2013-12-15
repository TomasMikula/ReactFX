package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.StringPropertyBase}.
 */
public abstract class StringPropertyBase extends javafx.beans.property.StringPropertyBase {

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

    public StringPropertyBase() {
        super();
    }

    public StringPropertyBase(String initialValue) {
        super(initialValue);
    }
}

package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.BooleanPropertyBase}.
 */
public abstract class BooleanPropertyBase extends javafx.beans.property.BooleanPropertyBase implements InhibitoryProperty<Boolean> {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public void block() {
        blocked = true;
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

    public BooleanPropertyBase() {
        super();
    }

    public BooleanPropertyBase(boolean initialValue) {
        super(initialValue);
    }
}

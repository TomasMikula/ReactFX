package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyBooleanWrapper}.
 */
public class ReadOnlyBooleanWrapper
extends javafx.beans.property.ReadOnlyBooleanWrapper
implements InhibitoryProperty<Boolean> {

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

    public ReadOnlyBooleanWrapper() {
        super();
    }

    public ReadOnlyBooleanWrapper(boolean initialValue) {
        super(initialValue);
    }

    public ReadOnlyBooleanWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyBooleanWrapper(Object bean, String name,
            boolean initialValue) {
        super(bean, name, initialValue);
    };
}

package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyStringWrapper}.
 */
public class ReadOnlyStringWrapper
extends javafx.beans.property.ReadOnlyStringWrapper
implements InhibitoryProperty<String> {

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

    public ReadOnlyStringWrapper() {
        super();
    }

    public ReadOnlyStringWrapper(String initialValue) {
        super(initialValue);
    }

    public ReadOnlyStringWrapper(Object bean, String name) {
        super(bean, name);
    }

    public ReadOnlyStringWrapper(Object bean, String name,
            String initialValue) {
        super(bean, name, initialValue);
    };
}

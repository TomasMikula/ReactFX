package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleObjectProperty}.
 */
public class SimpleObjectProperty<T>
extends javafx.beans.property.SimpleObjectProperty<T>
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

    public SimpleObjectProperty() {
        super();
    }

    public SimpleObjectProperty(T initialValue) {
        super(initialValue);
    }

    public SimpleObjectProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleObjectProperty(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }
}
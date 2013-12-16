package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleStringProperty}.
 */
public class SimpleStringProperty
extends javafx.beans.property.SimpleStringProperty
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

    public SimpleStringProperty() {
        super();
    }

    public SimpleStringProperty(String initialValue) {
        super(initialValue);
    }

    public SimpleStringProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleStringProperty(Object bean, String name, String initialValue) {
        super(bean, name, initialValue);
    }
}

package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleIntegerProperty}.
 */
public class SimpleIntegerProperty extends javafx.beans.property.SimpleIntegerProperty {

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

    public SimpleIntegerProperty() {
        super();
    }

    public SimpleIntegerProperty(int initialValue) {
        super(initialValue);
    }

    public SimpleIntegerProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleIntegerProperty(Object bean, String name, int initialValue) {
        super(bean, name, initialValue);
    }
}

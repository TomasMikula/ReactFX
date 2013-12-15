package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.SimpleBooleanProperty}.
 */
public class SimpleBooleanProperty extends javafx.beans.property.SimpleBooleanProperty implements InhibitoryProperty<Boolean> {

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

    public SimpleBooleanProperty() {
        super();
    }

    public SimpleBooleanProperty(boolean initialValue) {
        super(initialValue);
    }

    public SimpleBooleanProperty(Object bean, String name) {
        super(bean, name);
    }

    public SimpleBooleanProperty(Object bean, String name, boolean initialValue) {
        super(bean, name, initialValue);
    }
}

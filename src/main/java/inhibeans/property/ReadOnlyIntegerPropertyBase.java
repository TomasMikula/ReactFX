package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyIntegerPropertyBase}.
 */
public abstract class ReadOnlyIntegerPropertyBase extends javafx.beans.property.ReadOnlyIntegerPropertyBase {

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
}

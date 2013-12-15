package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyLongPropertyBase}.
 */
public abstract class ReadOnlyLongPropertyBase extends javafx.beans.property.ReadOnlyLongPropertyBase {

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

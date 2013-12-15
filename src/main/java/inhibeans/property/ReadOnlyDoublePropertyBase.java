package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyDoublePropertyBase}.
 */
public abstract class ReadOnlyDoublePropertyBase extends javafx.beans.property.ReadOnlyDoublePropertyBase {

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

package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyFloatPropertyBase}.
 */
public abstract class ReadOnlyFloatPropertyBase extends javafx.beans.property.ReadOnlyFloatPropertyBase {

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

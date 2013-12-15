package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyStringPropertyBase}.
 */
public abstract class ReadOnlyStringPropertyBase extends javafx.beans.property.ReadOnlyStringPropertyBase {

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

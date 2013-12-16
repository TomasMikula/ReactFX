package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyStringPropertyBase}.
 */
public abstract class ReadOnlyStringPropertyBase
extends javafx.beans.property.ReadOnlyStringPropertyBase
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
}

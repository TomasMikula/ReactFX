package inhibeans.property;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyObjectPropertyBase}.
 */
public abstract class ReadOnlyObjectPropertyBase<T>
extends javafx.beans.property.ReadOnlyObjectPropertyBase<T>
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
}

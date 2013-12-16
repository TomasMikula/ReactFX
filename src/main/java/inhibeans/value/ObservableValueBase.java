package inhibeans.value;

public abstract class ObservableValueBase<T>
extends javafx.beans.value.ObservableValueBase<T>
implements InhibitoryObservableValue<T> {

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
}

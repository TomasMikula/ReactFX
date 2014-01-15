package inhibeans;

import reactfx.ObservableBase;

public class Impulse extends ObservableBase implements Observable {

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
            notifyListeners();
        }
    }

    public void trigger() {
        if(blocked)
            fireOnRelease = true;
        else
            notifyListeners();
    }
}

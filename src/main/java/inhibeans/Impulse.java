package inhibeans;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;

public class Impulse implements Observable {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    private List<InvalidationListener> listeners = null;

    @Override
    public void addListener(InvalidationListener listener) {
        if(listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        if(listeners != null) {
            listeners.remove(listener);
        }
    }

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

    private void notifyListeners() {
        if(listeners != null) {
            for(Object l: listeners.toArray()) {
                ((InvalidationListener) l).invalidated(this);
            }
        }
    }
}

package reactfx;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

public abstract class ObservableBase implements Observable {
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

    protected void notifyListeners() {
        if(listeners != null) {
            for(Object l: listeners.toArray()) {
                ((InvalidationListener) l).invalidated(this);
            }
        }
    }
}
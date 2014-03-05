package org.reactfx.inhibeans.property;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;

public class CountingListener implements InvalidationListener {

    private int count = 0;
    private final ObservableValue<?> observable;

    public CountingListener(ObservableValue<?> observable) {
        this.observable = observable;
        observable.addListener(this);
    }

    @Override
    public void invalidated(Observable o) {
        ++count;
        observable.getValue(); // force recomputation
    }

    public int get() {
        return count;
    }

    public int getAndReset() {
        int res = count;
        count = 0;
        return res;
    }
}

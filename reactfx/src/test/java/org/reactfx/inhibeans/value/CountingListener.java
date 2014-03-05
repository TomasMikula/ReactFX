package org.reactfx.inhibeans.value;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

public class CountingListener implements InvalidationListener {

    private int count = 0;

    public CountingListener(Observable observable) {
        observable.addListener(this);
    }

    @Override
    public void invalidated(Observable o) {
        ++count;
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

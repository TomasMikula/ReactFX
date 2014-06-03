package org.reactfx;

import java.util.function.Consumer;

class EventCounter implements Consumer<Object> {
    private int count = 0;

    @Override
    public void accept(Object o) {
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

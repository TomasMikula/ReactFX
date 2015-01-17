package org.reactfx.value;

class Counter {
    private int count = 0;
    public void inc() {
        count += 1;
    }
    public int get() {
        return count;
    }
    public void reset() {
        count = 0;
    }

    public int getAndReset() {
        int res = count;
        count = 0;
        return res;
    }
}
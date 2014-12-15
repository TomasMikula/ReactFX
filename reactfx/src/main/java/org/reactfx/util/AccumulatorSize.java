package org.reactfx.util;

public enum AccumulatorSize {
    ZERO, ONE, MANY;

    public static AccumulatorSize fromInt(int n) {
        if(n < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + n);
        } else switch(n) {
            case 0: return ZERO;
            case 1: return ONE;
            default: return MANY;
        }
    }
}
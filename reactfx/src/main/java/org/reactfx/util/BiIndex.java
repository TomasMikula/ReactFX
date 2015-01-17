package org.reactfx.util;

import java.util.function.BiFunction;

public final class BiIndex {
    public final int major;
    public final int minor;

    public BiIndex(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public <T> T map(BiFunction<Integer, Integer, T> f) {
        return f.apply(major, minor);
    }

    public BiIndex adjustMajor(int adjustment) {
        return new BiIndex(major + adjustment, minor);
    }

    public BiIndex adjustMinor(int adjustment) {
        return new BiIndex(major, minor + adjustment);
    }

    @Override
    public String toString() {
        return "[" + major + ", " + minor + "]";
    }
}
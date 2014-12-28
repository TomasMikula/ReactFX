package org.reactfx;


public interface ObservableHelpers<O, T> {
    void addObserver(O observer);
    void removeObserver(O observer);
    Subscription observe(O observer);
    void notifyObservers(T event);

    default int defaultHashCode() { return System.identityHashCode(this); }
    default boolean defaultEquals(Object o) { return this == o; }
    default String defaultToString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode());
    }
}
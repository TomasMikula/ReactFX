package org.reactfx;

public interface ErrorHandler {
    default void onError(Throwable error) {
        error.printStackTrace();
    }
}

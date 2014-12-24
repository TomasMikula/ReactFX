package org.reactfx.spi;

public interface ErrorHandler {
    void handle(Throwable error);
}
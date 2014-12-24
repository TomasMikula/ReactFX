package org.reactfx;

import java.util.ServiceLoader;

import org.reactfx.spi.ErrorHandler;

class ErrorHandlerService {

    private static ErrorHandlerService isntance;

    public static synchronized ErrorHandlerService getInstance() {
        if(isntance == null) {
            isntance = new ErrorHandlerService();
        }
        return isntance;
    }

    private final ServiceLoader<ErrorHandler> loader;

    private ErrorHandlerService() {
        loader = ServiceLoader.load(ErrorHandler.class);
    }

    public void handleError(Throwable error) {
        for(ErrorHandler h: loader) {
            h.handle(error);
            return;
        }

        // if no service provider found, print the stack trace
        error.printStackTrace();
    }
}

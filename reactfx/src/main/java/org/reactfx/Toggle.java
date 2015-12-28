package org.reactfx;

import java.util.function.Consumer;

import javafx.beans.value.ObservableValue;

import org.reactfx.value.ProxyVal;
import org.reactfx.value.Val;

/**
 * Observable boolean that changes value when suspended.
 * Which boolean value is the value of the base state and which is the value
 * of the suspended state depends on the implementation. */
public interface Toggle extends Val<Boolean>, Suspendable {

    /**
     * Creates a {@linkplain Toggle} view of an observable boolean and a
     * {@linkplain Suspendable} whose suspension causes the boolean value
     * to switch.
     * @param obs boolean value that indicates suspension of {@code suspender}.
     * @param suspender Assumed to switch the value of {@code obs} when
     * suspended and switch back when resumed, unless there are other suspenders
     * keeping it in the value corresponding to the suspended state.
     */
    static Toggle from(ObservableValue<Boolean> obs, Suspendable suspender) {
        return new ToggleFromVal(Val.wrap(obs), suspender);
    }
}

class ToggleFromVal extends ProxyVal<Boolean, Boolean> implements Toggle {
    private Suspendable suspender;

    public ToggleFromVal(Val<Boolean> obs, Suspendable suspender) {
        super(obs);
        this.suspender = suspender;
    }

    @Override
    public Boolean getValue() {
        return getUnderlyingObservable().getValue();
    }

    @Override
    public Guard suspend() {
        return suspender.suspend();
    }

    @Override
    protected Consumer<? super Boolean> adaptObserver(Consumer<? super Boolean> observer) {
        return observer;
    }

}
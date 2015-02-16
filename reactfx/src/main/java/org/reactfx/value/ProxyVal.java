package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.ProxyObservable;

public abstract class ProxyVal<T, U>
extends ProxyObservable<Consumer<? super T>, Consumer<? super U>, Val<U>>
implements Val<T> {

    protected ProxyVal(Val<U> underlying) {
        super(underlying);
    }
}

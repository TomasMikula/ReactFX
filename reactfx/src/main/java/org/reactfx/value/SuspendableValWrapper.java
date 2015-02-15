package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.SuspendableBase;
import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.NotificationAccumulator;

class SuspendableValWrapper<T>
extends SuspendableBase<Consumer<? super T>, T, T>
implements SuspendableVal<T> {
    private final Val<T> delegate;

    protected SuspendableValWrapper(Val<T> obs) {
        super(
                obs.invalidations(),
                NotificationAccumulator.retainOldestValNotifications());
        this.delegate = obs;
    }

    @Override
    public T getValue() {
        return delegate.getValue();
    }

    @Override
    protected AccumulatorSize sizeOf(T accum) {
        return AccumulatorSize.ONE;
    }

    @Override
    protected T headOf(T accum) {
        return accum;
    }

    @Override
    protected T tailOf(T accum) {
        throw new UnsupportedOperationException("Cannot take a tail of a single value");
    }

}
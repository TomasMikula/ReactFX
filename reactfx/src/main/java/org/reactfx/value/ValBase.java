package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.ObservableBase;
import org.reactfx.Subscription;
import org.reactfx.util.NotificationAccumulator;

public abstract class ValBase<T>
extends ObservableBase<Consumer<? super T>, T>
implements Val<T> {
    private boolean valid = false; // irrelevant when not isObservingInputs()
    private T value = null;

    protected ValBase() {
        super(NotificationAccumulator.retainOldestValNotifications());
    }

    @Override
    public final T getValue() {
        if(!valid || !isObservingInputs()) {
            value = computeValue();
            valid = true;
        }
        return value;
    }

    @Override
    protected final Subscription observeInputs() {
        valid = false;
        return connect();
    }

    protected final void invalidate() {
        if(valid) {
            valid = false;
            notifyObservers(value);
        }
    }

    @Override
    protected final void newObserver(Consumer<? super T> oldValueObserver) {
        // make sure the current value is valid, so that the observer
        // does not miss any invalidations or changes
        getValue();
    }

    /**
     * Implementation of this method should start observing inputs. If the value
     * of this {@linkplain Val} may change as a result of input change, the
     * corresponding input observer should call {@link #invalidate()} to notify
     * observers of this {@linkplain Val}. By the time of calling {@linkplain
     * #invalidate()}, the input observer must have already updated any internal
     * state of this {@linkplain Val}, so that a subsequent call to {@link
     * #computeValue()} returns the current value of this {@linkplain Val}.
     * @return Subscription that can be used to stop observing inputs.
     */
    protected abstract Subscription connect();
    protected abstract T computeValue();
}

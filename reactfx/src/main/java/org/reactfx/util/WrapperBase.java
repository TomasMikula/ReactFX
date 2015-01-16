package org.reactfx.util;

import java.util.Objects;

/**
 * Base class for value-based wrappers, that is wrappers that implement
 * {@link #equals(Object)} and {@link #hashCode()} solely by comparing/hashing
 * the wrapped values.
 * @param <T> type of the wrapped value.
 */
public abstract class WrapperBase<T> {
    private final T delegate;

    /**
     * @param delegate wrapped value.
     */
    protected WrapperBase(T delegate) {
        if(delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        this.delegate = delegate;
    }

    public final T getWrappedValue() {
        return delegate;
    }

    @Override
    public final boolean equals(Object that) {
        if(that instanceof WrapperBase) {
            return Objects.equals(
                    ((WrapperBase<?>) that).delegate,
                    this.delegate);
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }
}
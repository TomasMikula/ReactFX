package org.reactfx.inhibeans;

import java.util.function.Supplier;

import org.reactfx.Guard;
import org.reactfx.Guardian;
import org.reactfx.Suspendable;

/**
 * @deprecated Superseded by {@link Suspendable}.
 */
@Deprecated
public interface Observable extends javafx.beans.Observable, Guardian {

    /**
     * Prevents invalidation and change events from being emitted,
     * until the returned guard is released.
     *
     * @return a {@code Guard} instance that can be released to resume
     * the delivery of invalidation and change events. If this observable
     * has been invalidated one or more times before the guard is released,
     * a single notification is passed to invalidation and change listeners
     * of this observable.
     * The returned {@code Guard} is {@code AutoCloseable}, which makes it
     * convenient to use in try-with-resources.
     */
    Guard block();

    /**
     * Equivalent to {@link #block()}.
     */
    @Override
    default Guard guard() {
        return block();
    }

    /**
     * Runs the given computation, making sure the invalidation and change
     * events are blocked. When done, previous blocked state is restored.
     *
     * <p>Equivalent to
     * <pre>
     * try(Guard g = block()) {
     *     r.run();
     * }
     * </pre>
     */
    default void blockWhile(Runnable r) {
        try(Guard g = block()) {
            r.run();
        }
    }

    /**
     * Runs the given computation, making sure the invalidation and change
     * events are blocked. When done, previous blocked state is restored.
     *
     * <pre>
     * T t = this.blockWhile(f);
     * </pre>
     *
     * is equivalent to
     *
     * <pre>
     * T t;
     * try(Guard g = block()) {
     *     t = f.get();
     * }
     * </pre>
     */
    default <T> T blockWhile(Supplier<T> f) {
        try(Guard g = block()) {
            return f.get();
        }
    }
}

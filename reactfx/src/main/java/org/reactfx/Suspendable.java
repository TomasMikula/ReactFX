package org.reactfx;

import java.util.function.Supplier;

/**
 * Interface for observable objects that can temporarily suspend notifications.
 * What notifications are delivered when notifications are resumed depends on
 * the concrete implementation. For example, notifications produced while
 * suspended may be queued, accumulated, or ignored completely.
 */
public interface Suspendable {

    /**
     * Suspends notification delivery for this observable object.
     * Notifications produced while suspended may be queued for later delivery,
     * accumulated into a single cumulative notification, or discarded
     * completely, depending on the concrete implementation.
     *
     * @return a {@code Guard} instance that can be released to resume
     * the delivery of notifications. Releasing the guard will trigger delivery
     * of queued or accumulated notifications, if any.
     * The returned {@code Guard} is {@code AutoCloseable}, which makes it
     * convenient to use in try-with-resources.
     */
    Guard suspend();

    /**
     * Runs the given computation with notifications suspended.
     *
     * <p>Equivalent to
     * <pre>
     * try(Guard g = suspend()) {
     *     r.run();
     * }
     * </pre>
     */
    default void suspendWhile(Runnable r) {
        try(Guard g = suspend()) { r.run(); }
    };

    /**
     * Runs the given computation with notifications suspended.
     *
     * The code
     *
     * <pre>
     * T t = this.suspendWhile(f);
     * </pre>
     *
     * is equivalent to
     *
     * <pre>
     * T t;
     * try(Guard g = suspend()) {
     *     t = f.get();
     * }
     * </pre>
     *
     * @return the result produced by the given supplier {@code f}.
     */
    default <U> U suspendWhile(Supplier<U> f) {
        try(Guard g = suspend()) { return f.get(); }
    }

}

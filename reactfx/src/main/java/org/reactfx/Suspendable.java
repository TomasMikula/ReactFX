package org.reactfx;

import java.util.function.Supplier;

/**
 * Observable object whose notifications can be suspended temporarily. What
 * notifications are delivered when notifications are resumed depends on the
 * concrete implementation. For example, notifications produced while suspended
 * may be queued, accumulated, or ignored.
 */
public interface Suspendable {

    Guard suspend();

    default void suspendWhile(Runnable r) {
        try(Guard g = suspend()) { r.run(); }
    };

    default <U> U suspendWhile(Supplier<U> f) {
        try(Guard g = suspend()) { return f.get(); }
    }

}

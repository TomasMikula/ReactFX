package org.reactfx;

import java.util.Arrays;
import java.util.function.Supplier;

import javafx.beans.value.ObservableValue;

/**
 * Interface for objects that can be temporarily <em>suspended</em>, where the
 * definition of "suspended" depends on the context. For example, when an
 * {@link Observable} is {@linkplain Suspendable}, it means that its
 * notification delivery can be suspended temporarily. In that case, what
 * notifications are delivered when notifications are resumed depends on the
 * concrete implementation. For example, notifications produced while suspended
 * may be queued, accumulated, or ignored completely.
 */
public interface Suspendable {

    /**
     * Suspends this suspendable object.
     *
     * <p>In case of suspendable {@link Observable},
     * suspends notification delivery for this observable object.
     * Notifications produced while suspended may be queued for later delivery,
     * accumulated into a single cumulative notification, or discarded
     * completely, depending on the concrete implementation.
     *
     * @return a {@linkplain Guard} instance that can be released to end
     * suspension. In case of suspended notifications, releasing the returned
     * {@linkplain Guard} will trigger delivery of queued or accumulated
     * notifications, if any.
     *
     * <p>The returned {@code Guard} is {@code AutoCloseable}, which makes it
     * convenient to use in try-with-resources.
     */
    Guard suspend();

    /**
     * Runs the given computation while suspended.
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
     * Runs the given computation while suspended.
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

    /**
     * Arranges to suspend this {@linkplain Suspendable} whenever
     * {@code condition} is {@code true}.
     *
     * @return A {@linkplain Subscription} that can be used to stop observing
     * {@code condition} and stop suspending this {@linkplain Suspendable} based
     * on {@code condition}. If at the time of unsubscribing the returned
     * {@linkplain Subscription} this {@linkplain Suspendable} was suspended due
     * to {@code condition} being {@code true}, it will be resumed.
     */
    default Subscription suspendWhen(ObservableValue<Boolean> condition) {
        return new Subscription() {

            private Guard suspensionGuard = null;

            private final Subscription sub = EventStreams.valuesOf(condition)
                    .subscribe(this::suspendSource)
                    .and(this::resumeSource);

            @Override
            public void unsubscribe() {
                sub.unsubscribe();
            }

            private void suspendSource(boolean suspend) {
                if(suspend) {
                    suspendSource();
                } else {
                    resumeSource();
                }
            }

            private void suspendSource() {
                if(suspensionGuard == null) {
                    suspensionGuard = Suspendable.this.suspend();
                }
            }

            private void resumeSource() {
                if(suspensionGuard != null) {
                    Guard toClose = suspensionGuard;
                    suspensionGuard = null;
                    toClose.close();
                }
            }

        };
    }


    /**
     * Returns a {@linkplain Suspendable} that combines all the given
     * {@linkplain Suspendable}s into one. When that combined
     * {@linkplain Suspendable} is suspended, all participating
     * {@linkplain Suspendable}s are suspended, in the given order. When
     * resumed, all participating {@linkplain Suspendable}s are resumed, in
     * reverse order.
     *
     */
    static Suspendable combine(Suspendable... suspendables) {
        switch(suspendables.length) {
            case 0: throw new IllegalArgumentException("Must invoke with at least 1 argument");
            case 1: return suspendables[0];
            case 2: return new BiSuspendable(suspendables[0], suspendables[1]);
            default: return new MultiSuspendable(suspendables);
        }
    }
}

class BiSuspendable implements Suspendable {
    private final Suspendable s1;
    private final Suspendable s2;

    public BiSuspendable(Suspendable s1, Suspendable s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override
    public Guard suspend() {
        return new BiGuard(s1.suspend(), s2.suspend());
    }
}

class MultiSuspendable implements Suspendable {
    private final Suspendable[] suspendables;

    public MultiSuspendable(Suspendable... suspendables) {
        this.suspendables = suspendables;
    }

    @Override
    public Guard suspend() {
        Guard[] guards = Arrays.stream(suspendables)
                .map(g -> g.suspend()).toArray(n -> new Guard[n]);
        return new MultiGuard(guards);
    }
}

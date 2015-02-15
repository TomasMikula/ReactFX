package org.reactfx;

/**
 * An {@linkplain Observable} that does not maintain its own collection of
 * observers, but rather adapts and registers every given observer with the
 * underlying {@linkplain Observable}
 * @param <O> observer type accepted by this {@linkplain Observable}
 * @param <P> observer type accepted by the underlying {@linkplain Observable}
 * @param <U> type of the underlying observable to which observers are delegated
 */
public abstract class ProxyObservable<O, P, U extends Observable<P>>
implements Observable<O> {
    private final U underlying;

    protected ProxyObservable(U underlying) {
        this.underlying = underlying;
    }

    /**
     * Adapts the given observer to observer of the underlying
     * {@linkplain Observable}.
     *
     * <p><strong>Important:</strong> It is required that
     * the transformation applied to two observers that are _equal_ yields two
     * adapted observers that are _equal_. In other words, if `o1.equals(o2)`,
     * then it must be the case that
     * `adaptObserver(o1).equals(adaptObserver(o2))`.
     *
     * @param observer observer to be adapted for the underlying
     * {@linkplain Observable}
     * @return observer adapted for the underlying {@linkplain Observable}
     */
    protected abstract P adaptObserver(O observer);

    protected final U getUnderlyingObservable() {
        return underlying;
    }

    @Override
    public final void addObserver(O observer) {
        P adapted = adaptObserver(observer);

        assert adapted.equals(adaptObserver(observer))
        : "Two adaptations of the same observer resulted in non-equal"
        + " adapted observers";

        underlying.addObserver(adapted);
    }

    @Override
    public final void removeObserver(O observer) {
        underlying.removeObserver(adaptObserver(observer));
    }

    // Overridden to avoid second transformation on removeObserver
    @Override
    public final Subscription observe(O observer) {
        P adapted = adaptObserver(observer);
        underlying.addObserver(adapted);
        return () -> underlying.removeObserver(adapted);
    }
}

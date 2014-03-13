package org.reactfx;

public interface Hold extends AutoCloseable {

    static Hold EMPTY_HOLD = () -> {};

    /**
     * Releases this hold. Does not throw.
     */
    @Override
    void close();

    /**
     * Returns a hold that is a composition of multiple holds.
     * Its {@code close()} method closes the holds in reverse order.
     * @param holds holds that should be released (in reverse order)
     * when the returned hold is released.
     * @return
     */
    static Hold multi(Hold... holds) {
        return () -> {
            for(int i = holds.length - 1; i >= 0; --i) {
                holds[i].close();
            }
        };
    }
}

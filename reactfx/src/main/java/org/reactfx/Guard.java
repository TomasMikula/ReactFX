package org.reactfx;

@FunctionalInterface
public interface Guard extends Hold {

    static Guard EMPTY_GUARD = () -> {};

    /**
     * Releases this guard. Does not throw.
     */
    @Override
    void close();


    /**
     * Returns a guard that is a composition of multiple guards.
     * Its {@code close()} method closes the guards in reverse order.
     * @param guards guards that should be released (in reverse order)
     * when the returned guards is released.
     * @return
     */
    static Guard multi(Guard... guards) {
        return () -> {
            for(int i = guards.length - 1; i >= 0; --i) {
                guards[i].close();
            }
        };
    }
}

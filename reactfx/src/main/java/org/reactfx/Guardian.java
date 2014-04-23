package org.reactfx;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Interface for objects usable to guard execution (in some sense).
 */
public interface Guardian {
    Guard guard();

    default void guardWhile(Runnable r) {
        try(Guard g = guard()) {
            r.run();
        }
    }

    default <T> T guardWhile(Supplier<T> f) {
        try(Guard g = guard()) {
            return f.get();
        }
    }

    static final Guardian EMPTY = () -> Guard.EMPTY_GUARD;

    /**
     * Returns a guardian that combines all of the given guardians into one.
     * Guards will be obtained in the specified order and released in reversed
     * order.
     */
    static Guardian combine(Guardian... guardians) {
        switch(guardians.length) {
            case 0: return EMPTY;
            case 1: return guardians[0];
            case 2: return new BiGuardian(guardians[0], guardians[1]);
            default: return new MultiGuardian(guardians);
        }
    }
}

class BiGuardian implements Guardian {
    private final Guardian g1;
    private final Guardian g2;

    public BiGuardian(Guardian g1, Guardian g2) {
        this.g1 = g1;
        this.g2 = g2;
    }

    @Override
    public Guard guard() {
        return new BiGuard(g1.guard(), g2.guard());
    }
}

class MultiGuardian implements Guardian {
    private final Guardian[] guardians;

    public MultiGuardian(Guardian... guardians) {
        this.guardians = guardians;
    }

    @Override
    public Guard guard() {
        Guard[] guards = Arrays.stream(guardians).map(g -> g.guard()).toArray(n -> new Guard[n]);
        return new MultiGuard(guards);
    }
}
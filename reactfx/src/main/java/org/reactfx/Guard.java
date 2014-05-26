package org.reactfx;


@FunctionalInterface
public interface Guard extends Hold {

    static Guard EMPTY_GUARD = () -> {};

    /**
     * Releases this guard. Does not throw.
     */
    @Override
    void close();

    default Guard closeableOnce() {
        return new CloseableOnceGuard(this);
    }


    /**
     * Returns a guard that is a composition of multiple guards.
     * Its {@code close()} method closes the guards in reverse order.
     * @param guards guards that should be released (in reverse order)
     * when the returned guards is released.
     * @return
     */
    static Guard multi(Guard... guards) {
        switch(guards.length) {
            case 0: return EMPTY_GUARD;
            case 1: return guards[0];
            case 2: return new BiGuard(guards[0], guards[1]);
            default: return new MultiGuard(guards);
        }
    }
}

class CloseableOnceGuard implements Guard {
    private Guard delegate;

    public CloseableOnceGuard(Guard delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        if(delegate != null) {
            delegate.close();
            delegate = null;
        }
    }
}

class BiGuard implements Guard {
    private final Guard g1;
    private final Guard g2;

    public BiGuard(Guard g1, Guard g2) {
        this.g1 = g1;
        this.g2 = g2;
    }

    @Override
    public void close() {
        // close in reverse order
        g2.close();
        g1.close();
    }
}

class MultiGuard implements Guard {
    private final Guard[] guards;

    public MultiGuard(Guard... guards) {
        this.guards = guards;
    }

    @Override
    public void close() {
        // close in reverse order
        for(int i = guards.length - 1; i >= 0; --i) {
            guards[i].close();
        }
    }
}
package reactfx;

import java.util.NoSuchElementException;

public abstract class FusionResult<T> {

    private static final FusionResult<?> ANNIHILATED = new FusionResult<Void>() {
        @Override public boolean isFused() { return false; }
        @Override public boolean isAnnihilated() { return true; }
        @Override public boolean isFailed() { return false; }
        @Override public Void get() { throw new NoSuchElementException(); }
    };

    private static final FusionResult<?> FAILED = new FusionResult<Void>() {
        @Override public boolean isFused() { return false; }
        @Override public boolean isAnnihilated() { return false; }
        @Override public boolean isFailed() { return true; }
        @Override public Void get() { throw new NoSuchElementException(); }
    };

    public static <T> FusionResult<T> fused(T result) {
        return new FusionResult<T>() {
            @Override public boolean isFused() { return true; }
            @Override public boolean isAnnihilated() { return false; }
            @Override public boolean isFailed() { return false; }
            @Override public T get() { return result; }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> FusionResult<T> annihilated() {
        return (FusionResult<T>) ANNIHILATED;
    }

    @SuppressWarnings("unchecked")
    public static <T> FusionResult<T> failed() {
        return (FusionResult<T>) FAILED;
    }

    // Private constructor to prevent subclassing.
    private FusionResult() {}

    public abstract boolean isFused();
    public abstract boolean isAnnihilated();
    public abstract boolean isFailed();
    public abstract T get();
}
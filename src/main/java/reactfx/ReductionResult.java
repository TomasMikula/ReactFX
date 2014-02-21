package reactfx;

import java.util.NoSuchElementException;

public abstract class ReductionResult<T> {

    private static final ReductionResult<?> ANNIHILATED = new ReductionResult<Void>() {
        @Override public boolean isReduced() { return false; }
        @Override public boolean isAnnihilated() { return true; }
        @Override public boolean isFailed() { return false; }
        @Override public Void get() { throw new NoSuchElementException(); }
    };

    private static final ReductionResult<?> FAILED = new ReductionResult<Void>() {
        @Override public boolean isReduced() { return false; }
        @Override public boolean isAnnihilated() { return false; }
        @Override public boolean isFailed() { return true; }
        @Override public Void get() { throw new NoSuchElementException(); }
    };

    public static <T> ReductionResult<T> reduced(T result) {
        return new ReductionResult<T>() {
            @Override public boolean isReduced() { return true; }
            @Override public boolean isAnnihilated() { return false; }
            @Override public boolean isFailed() { return false; }
            @Override public T get() { return result; }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> ReductionResult<T> annihilated() {
        return (ReductionResult<T>) ANNIHILATED;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReductionResult<T> failed() {
        return (ReductionResult<T>) FAILED;
    }

    // Private constructor to prevent subclassing.
    private ReductionResult() {}

    public abstract boolean isReduced();
    public abstract boolean isAnnihilated();
    public abstract boolean isFailed();
    public abstract T get();
}
package inhibeans;

import java.util.function.Supplier;

public interface Observable extends javafx.beans.Observable {

    /**
     * Prevents invalidation and change events from being emitted,
     * until the returned block is released.
     *
     * @return a {@code Block} instance that can be released to resume
     * the delivery of invalidation and change events. If this observable
     * has been invalidated one or more times before the block is released,
     * a single notification is passed to invalidation and change listeners
     * of this observable.
     * The returned {@code Block} is {@code AutoCloseable}, which makes it
     * convenient to use it in try-with-resources.
     */
    Hold block();

    /**
     * Runs the given computation, making sure the invalidation and change
     * events are blocked. When done, previous blocked state is restored.
     *
     * <p>Equivalent to
     * <pre>
     * try(Hold h = block()) {
     *     r.run();
     * }
     * </pre>
     */
    default void blockWhile(Runnable r) {
        try(Hold b = block()) {
            r.run();
        }
    }

    /**
     * Runs the given computation, making sure the invalidation and change
     * events are blocked. When done, previous blocked state is restored.
     *
     * <pre>
     * T t = this.blockWhile(f);
     * </pre>
     *
     * is equivalent to
     *
     * <pre>
     * T t;
     * try(Hold h = block()) {
     *     t = f.get();
     * }
     * </pre>
     */
    default <T> T blockWhile(Supplier<T> f) {
        try(Hold b = block()) {
            return f.get();
        }
    }
}

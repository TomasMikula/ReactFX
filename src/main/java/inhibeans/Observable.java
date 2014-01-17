package inhibeans;

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
    Block block();

    /**
     * Equivalent to
     * <pre>
     * try(Block b = block()) {
     *     r.run();
     * }
     * </pre>
     */
    default void blockWhile(Runnable r) {
        try(Block b = block()) {
            r.run();
        }
    }
}

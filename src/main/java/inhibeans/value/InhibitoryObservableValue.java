package inhibeans.value;

import javafx.beans.value.ObservableValue;

public interface InhibitoryObservableValue<T>
extends ObservableValue<T>, AutoCloseable {

    /**
     * Prevents invalidation and change events from being emitted.
     *
     * @return {@code this}. This makes it convenient to use the return
     * value of {@code block()} in try-with-resources.
     */
    AutoCloseable block();

    /**
     * Resumes the delivery of invalidation and change events.
     * If this observable value has been invalidated multiple
     * times since the call to {@link #block()}, invalidation
     * and change listeners will only be executed once.
     */
    void release();

    /**
     * Equivalent to {@link #release()}.
     */
    @Override
    default void close() {
        release();
    }

    /**
     * Equivalent to
     * <pre>
     * {@code
     * this.block();
     * r.run();
     * this.release();
     * }</pre>
     */
    default void blockWhile(Runnable r) {
        block();
        r.run();
        release();
    }
}

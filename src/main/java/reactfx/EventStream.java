package reactfx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Stream of values (events).
 *
 * It is an analog of rxJava's {@code Observable}, but "Observable"
 * already has a different meaning in JavaFX.
 *
 * @param <T> type of values this source emits.
 */
public interface EventStream<T> {

    /**
     * Get notified every time this stream emits a value.
     * @param consumer function to call on the emitted value.
     * @return subscription that can be used to stop observing
     * this event stream.
     */
    Subscription subscribe(Consumer<T> consumer);

    default EventStream<T> filter(Predicate<T> predicate) {
        return EventStreams.filter(this, predicate);
    }

    default <U> EventStream<U> map(Function<T, U> f) {
        return EventStreams.map(this, f);
    }

    default InterceptableEventStream<T> interceptable() {
        return EventStreams.interceptable(this);
    }
}

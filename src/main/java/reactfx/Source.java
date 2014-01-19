package reactfx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Observable stream of values.
 *
 * It is an analog of rxJava's {@code Observable}, but "Observable"
 * already has a different meaning in JavaFX. "Stream" is also already
 * taken in JDK8, so we go with "Source".
 *
 * @param <T> type of values this source emits.
 */
public interface Source<T> {
    Subscription subscribe(Consumer<T> consumer);

    default Source<T> filter(Predicate<T> predicate) {
        return Sources.filter(this, predicate);
    }

    default <U> Source<U> map(Function<T, U> f) {
        return Sources.map(this, f);
    }
}

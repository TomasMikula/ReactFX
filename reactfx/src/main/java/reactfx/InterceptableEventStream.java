package reactfx;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public interface InterceptableEventStream<T> extends EventStream<T> {

    Hold mute();
    Hold pause();
    Hold retainLatest();
    Hold reduce(BinaryOperator<T> reduction);
    Hold tryReduce(BiFunction<T, T, ReductionResult<T>> reduction);

    /**
     * @deprecated Use {@link #reduce(BinaryOperator)} instead.
     */
    @Deprecated
    default Hold fuse(BinaryOperator<T> fusor) {
        return reduce(fusor);
    }

    /**
     * @deprecated Use {@link #tryReduce(BiFunction)} instead.
     */
    @Deprecated
    default Hold fuse(BiFunction<T, T, ReductionResult<T>> fusor) {
        return tryReduce(fusor);
    }

    default void muteWhile(Runnable r) {
        try(Hold h = mute()) { r.run(); }
    };

    default <U> U muteWhile(Supplier<U> f) {
        try(Hold h = mute()) { return f.get(); }
    }

    default void pauseWhile(Runnable r) {
        try(Hold h = pause()) { r.run(); }
    }

    default <U> U pauseWhile(Supplier<U> f) {
        try(Hold h = pause()) { return f.get(); }
    }

    default void retainLatestWhile(Runnable r) {
        try(Hold h = retainLatest()) { r.run(); }
    }

    default <U> U retainLatestWhile(Supplier<U> f) {
        try(Hold h = retainLatest()) { return f.get(); }
    }

    default void reduceWhile(BinaryOperator<T> reduction, Runnable r) {
        try(Hold h = reduce(reduction)) { r.run(); }
    }

    default <U> U reduceWhile(BinaryOperator<T> reduction, Supplier<U> f) {
        try(Hold h = reduce(reduction)) { return f.get(); }
    }

    default void tryReduceWhile(BiFunction<T, T, ReductionResult<T>> reduction, Runnable r) {
        try(Hold h = tryReduce(reduction)) { r.run(); }
    }

    default <U> U tryReduceWhile(BiFunction<T, T, ReductionResult<T>> reduction, Supplier<U> f) {
        try(Hold h = tryReduce(reduction)) { return f.get(); }
    }

    /**
     * @deprecated use {@link #reduceWhile(BinaryOperator, Runnable)} instead.
     */
    @Deprecated
    default void fuseWhile(BinaryOperator<T> fusor, Runnable r) {
        reduceWhile(fusor, r);
    }

    /**
     * @deprecated use {@link #reduceWhile(BinaryOperator, Supplier)} instead.
     */
    @Deprecated
    default <U> U fuseWhile(BinaryOperator<T> fusor, Supplier<U> f) {
        return reduceWhile(fusor, f);
    }

    /**
     * @deprecated use {@link #tryReduceWhile(BiFunction, Runnable)} instead.
     */
    @Deprecated
    default void fuseWhile(BiFunction<T, T, ReductionResult<T>> fusor, Runnable r) {
        tryReduceWhile(fusor, r);
    }

    /**
     * @deprecated use {@link #tryReduceWhile(BiFunction, Supplier)} instead.
     */
    @Deprecated
    default <U> U fuseWhile(BiFunction<T, T, ReductionResult<T>> fusor, Supplier<U> f) {
        return tryReduceWhile(fusor, f);
    }
}

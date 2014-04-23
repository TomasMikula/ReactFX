package org.reactfx;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public interface InterceptableEventStream<T> extends EventStream<T> {

    Guard mute();
    Guard pause();
    Guard retainLatest();
    Guard reduce(BinaryOperator<T> reduction);
    Guard tryReduce(BiFunction<T, T, ReductionResult<T>> reduction);

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
        try(Guard g = mute()) { r.run(); }
    };

    default <U> U muteWhile(Supplier<U> f) {
        try(Guard g = mute()) { return f.get(); }
    }

    default void pauseWhile(Runnable r) {
        try(Guard g = pause()) { r.run(); }
    }

    default <U> U pauseWhile(Supplier<U> f) {
        try(Guard g = pause()) { return f.get(); }
    }

    default void retainLatestWhile(Runnable r) {
        try(Guard g = retainLatest()) { r.run(); }
    }

    default <U> U retainLatestWhile(Supplier<U> f) {
        try(Guard g = retainLatest()) { return f.get(); }
    }

    default void reduceWhile(BinaryOperator<T> reduction, Runnable r) {
        try(Guard g = reduce(reduction)) { r.run(); }
    }

    default <U> U reduceWhile(BinaryOperator<T> reduction, Supplier<U> f) {
        try(Guard g = reduce(reduction)) { return f.get(); }
    }

    default void tryReduceWhile(BiFunction<T, T, ReductionResult<T>> reduction, Runnable r) {
        try(Guard g = tryReduce(reduction)) { r.run(); }
    }

    default <U> U tryReduceWhile(BiFunction<T, T, ReductionResult<T>> reduction, Supplier<U> f) {
        try(Guard g = tryReduce(reduction)) { return f.get(); }
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

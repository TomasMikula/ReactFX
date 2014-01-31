package reactfx;

import inhibeans.Hold;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public interface InterceptableEventStream<T> extends EventStream<T> {

    Hold mute();
    Hold pause();
    Hold retainLatest();
    Hold fuse(BinaryOperator<T> fusor);
    Hold fuse(BiFunction<T, T, FusionResult<T>> fusor);

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

    default void fuseWhile(BinaryOperator<T> fusor, Runnable r) {
        try(Hold h = fuse(fusor)) { r.run(); }
    }

    default <U> U fuseWhile(BinaryOperator<T> fusor, Supplier<U> f) {
        try(Hold h = fuse(fusor)) { return f.get(); }
    }

    default void fuseWhile(BiFunction<T, T, FusionResult<T>> fusor, Runnable r) {
        try(Hold h = fuse(fusor)) { r.run(); }
    }

    default <U> U fuseWhile(BiFunction<T, T, FusionResult<T>> fusor, Supplier<U> f) {
        try(Hold h = fuse(fusor)) { return f.get(); }
    }
}

package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.reactfx.util.TriConsumer;
import org.reactfx.util.TriFunction;
import org.reactfx.util.TriPredicate;
import org.reactfx.util.Tuple3;

public interface TriEventStream<A, B, C> extends EventStream<Tuple3<A, B, C>> {

    Subscription subscribe(TriConsumer<? super A, ? super B, ? super C> subscriber);

    @Override
    default Subscription subscribe(Consumer<? super Tuple3<A, B, C>> subscriber) {
        return subscribe((a, b, c) -> subscriber.accept(t(a, b, c)));
    }

    default TriEventStream<A, B, C> hook(
            TriConsumer<? super A, ? super B, ? super C> sideEffect) {
        return new SideEffectTriStream<>(this, sideEffect);
    }

    default TriEventStream<A, B, C> filter(
            TriPredicate<? super A, ? super B, ? super C> predicate) {
        return new FilterTriStream<>(this, predicate);
    }

    default <U> EventStream<U> map(
            TriFunction<? super A, ? super B, ? super C, ? extends U> f) {
        return new MappedTriStream<>(this, f);
    }

    default <U> CompletionStageStream<U> mapToCompletionStage(
            TriFunction<? super A, ? super B, ? super C, CompletionStage<U>> f) {
        return new MappedToCompletionStageTriStream<>(this, f);
    }

    default <U> TaskStream<U> mapToTask(
            TriFunction<? super A, ? super B, ? super C, Task<U>> f) {
        return new MappedToTaskTriStream<>(this, f);
    }

    default <U> EventStream<U> filterMap(
            TriPredicate<? super A, ? super B, ? super C> predicate,
            TriFunction<? super A, ? super B, ? super C, ? extends U> f) {
        return new FilterMapTriStream<>(this, predicate, f);
    }

    default <U> EventStream<U> flatMap(BiFunction<? super A, ? super B, ? extends EventStream<U>> f) {
        return flatMap(t -> f.apply(t._1, t._2));
    }

    @Override
    default TriEventStream<A, B, C> emitOn(EventStream<?> impulse) {
        return new EmitOnTriStream<>(this, impulse);
    }

    @Override
    default TriEventStream<A, B, C> repeatOn(EventStream<?> impulse) {
        return new RepeatOnTriStream<>(this, impulse);
    }

    @Override
    default InterceptableTriEventStream<A, B, C> interceptable() {
        if(this instanceof InterceptableTriEventStream) {
            return (InterceptableTriEventStream<A, B, C>) this;
        } else {
            return new InterceptableTriEventStreamImpl<A, B, C>(this);
        }
    }

    @Override
    default TriEventStream<A, B, C> threadBridge(
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        return new TriThreadBridge<A, B, C>(this, sourceThreadExecutor, targetThreadExecutor);
    }

    @Override
    default TriEventStream<A, B, C> threadBridgeFromFx(Executor targetThreadExecutor) {
        return threadBridge(Platform::runLater, targetThreadExecutor);
    }

    @Override
    default TriEventStream<A, B, C> threadBridgeToFx(Executor sourceThreadExecutor) {
        return threadBridge(sourceThreadExecutor, Platform::runLater);
    }

    @Override
    default TriEventStream<A, B, C> guardedBy(Guardian... guardians) {
        return new GuardedTriStream<>(this, guardians);
    }
}

package org.reactfx;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

public interface BiEventStream<A, B> extends EventStream<Tuple2<A, B>> {

    Subscription subscribe(BiSubscriber<? super A, ? super B> subscriber);

    default Subscription subscribe(
            BiConsumer<? super A, ? super B> onEvent,
            Consumer<? super Throwable> onError) {
        return subscribe(BiSubscriber.create(onEvent, onError));
    }

    @Override
    default Subscription subscribe(
            Subscriber<? super Tuple2<A, B>> subscriber) {
        return subscribe(BiSubscriber.fromSubscriber(subscriber));
    }

    // Override to avoid constructing a tuple that is ignored anyway
    @Override
    default Subscription monitor(Consumer<? super Throwable> onError) {
        return subscribe(BiSubscriber.fromErrorHandler(onError));
    }

    default Subscription feedTo2(BiEventSink<? super A, ? super B> sink) {
        return subscribe((a, b) -> sink.push(a, b));
    }

    default BiEventStream<A, B> hook(
            BiConsumer<? super A, ? super B> sideEffect) {
        return new SideEffectBiStream<>(this, sideEffect);
    }

    default BiEventStream<A, B> filter(
            BiPredicate<? super A, ? super B> predicate) {
        return new FilterBiStream<>(this, predicate);
    }

    @Override
    default BiEventStream<A, B> distinct() {
        return new DistinctBiStream<>(this);
    }

    default <U> EventStream<U> map(
            BiFunction<? super A, ? super B, ? extends U> f) {
        return new MappedBiStream<>(this, f);
    }

    default <C, D> BiEventStream<C, D> mapToBi(BiFunction<? super A, ? super B, Tuple2<C, D>> f) {
        return new MappedBiToBiStream<>(this, f);
    }

    default <C, D, E> TriEventStream<C, D, E> mapToTri(BiFunction<? super A, ? super B, Tuple3<C, D, E>> f) {
        return new MappedBiToTriStream<>(this, f);
    }

    default <U> CompletionStageStream<U> mapToCompletionStage(
            BiFunction<? super A, ? super B, CompletionStage<U>> f) {
        return new MappedToCompletionStageBiStream<>(this, f);
    }

    default <U> TaskStream<U> mapToTask(
            BiFunction<? super A, ? super B, Task<U>> f) {
        return new MappedToTaskBiStream<>(this, f);
    }

    default <U> EventStream<U> filterMap(
            BiPredicate<? super A, ? super B> predicate,
            BiFunction<? super A, ? super B, ? extends U> f) {
        return new FilterMapBiStream<>(this, predicate, f);
    }

    default <U> EventStream<U> filterMap(BiFunction<? super A, ? super B, Optional<U>> f) {
        return filterMap(t -> f.apply(t._1, t._2));
    }

    default <U> EventStream<U> flatMap(BiFunction<? super A, ? super B, ? extends EventStream<U>> f) {
        return flatMap(t -> f.apply(t._1, t._2));
    }

    /**
     * @deprecated Since 1.2.1. Renamed to {@link #filterMap(BiFunction)}.
     */
    @Deprecated
    default <U> EventStream<U> flatMapOpt(BiFunction<? super A, ? super B, Optional<U>> f) {
        return flatMapOpt(t -> f.apply(t._1, t._2));
    }

    @Override
    default BiEventStream<A, B> emitOn(EventStream<?> impulse) {
        return new EmitOnBiStream<>(this, impulse);
    }

    @Override
    default BiEventStream<A, B> emitOnEach(EventStream<?> impulse) {
        return new EmitOnEachBiStream<>(this, impulse);
    }

    default <I> TriEventStream<A, B, I> emitAllOnEach(EventStream<I> impulse) {
        return new EmitAll3OnEachStream<>(this, impulse);
    }

    @Override
    default BiEventStream<A, B> repeatOn(EventStream<?> impulse) {
        return new RepeatOnBiStream<>(this, impulse);
    }

    @Override
    default BiEventStream<A, B> threadBridge(
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        return new BiThreadBridge<A, B>(this, sourceThreadExecutor, targetThreadExecutor);
    }

    @Override
    default BiEventStream<A, B> threadBridgeFromFx(Executor targetThreadExecutor) {
        return threadBridge(Platform::runLater, targetThreadExecutor);
    }

    @Override
    default BiEventStream<A, B> threadBridgeToFx(Executor sourceThreadExecutor) {
        return threadBridge(sourceThreadExecutor, Platform::runLater);
    }

    @Override
    default BiEventStream<A, B> guardedBy(Guardian... guardians) {
        return new GuardedBiStream<>(this, guardians);
    }
}

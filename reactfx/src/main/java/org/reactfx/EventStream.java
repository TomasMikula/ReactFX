package org.reactfx;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.concurrent.Task;

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
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                return EventStream.this.subscribe(value -> {
                    if(predicate.test(value)) {
                        emit(value);
                    }
                });
            }
        };
    }

    /**
     * Filters this event stream by the runtime type of the values.
     * {@code filter(SomeClass.class)} is equivalent to
     * {@code filter(x -> x instanceof SomeClass).map(x -> (SomeClass) x)}.
     * @param subtype
     * @return
     */
    default <U extends T> EventStream<U> filter(Class<U> subtype) {
        return filterMap(subtype::isInstance, subtype::cast);
    }

    default <U> EventStream<U> map(Function<T, U> f) {
        return new MappedStream<>(this, f);
    }

    /**
     * Similar to {@link #map(Function)}, but the returned stream is a
     * {@link CompletionStageStream}, which can be used to await the results
     * of asynchronous computation.
     */
    default <U> CompletionStageStream<U> mapToCompletionStage(Function<T, CompletionStage<U>> f) {
        return new MappedToCompletionStageStream<>(this, f);
    }

    /**
     * Similar to {@link #map(Function)}, but the returned stream is a
     * {@link TaskStream}, which can be used to await the results of
     * asynchronous computation.
     */
    default <U> TaskStream<U> mapToTask(Function<T, Task<U>> f) {
        return new MappedToTaskStream<>(this, f);
    }

    /**
     * A more efficient equivalent to
     * {@code filter(predicate).map(f)}.
     * @param predicate
     * @param f
     * @return
     */
    default <U> EventStream<U> filterMap(Predicate<T> predicate, Function<T, U> f) {
        return new LazilyBoundStream<U>() {
            @Override
            protected Subscription subscribeToInputs() {
                return EventStream.this.subscribe(value -> {
                    if(predicate.test(value)) {
                        emit(f.apply(value));
                    }
                });
            }
        };
    }

    default EventStream<T> emitOn(EventStream<?> impulse) {
        return EventStreams.emit(this).on(impulse);
    }

    default InterceptableEventStream<T> interceptable() {
        return EventStreams.interceptable(this);
    }

    default StreamBoundValue<T> toObservableValue(T initialValue) {
        return EventStreams.toObservableValue(this, initialValue);
    }

    /**
     * Returns an event stream that accumulates events emitted from this event
     * stream in close temporal succession. After an event is emitted from this
     * stream, the returned stream waits for up to {@code timeout} for the next
     * event from this stream. If the next event arrives within timeout, it is
     * accumulated to the current event by the {@code reduction} function and
     * the timeout is reset. When the timeout expires, the accumulated event is
     * emitted from the returned stream.
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #reduceCloseSuccessions(BinaryOperator, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param reduction function to reduce two events into one.
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     */
    default EventStream<T> reduceCloseSuccessions(
            BinaryOperator<T> reduction,
            Duration timeout) {

        return reduceCloseSuccessions(t -> t, reduction, timeout);
    }

    /**
     * A more general version of
     * {@link #reduceCloseSuccessions(BinaryOperator, Duration)}
     * that allows the accumulated event to be of different type.
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #reduceCloseSuccessions(Function, BiFunction, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param initialTransformation function to transform a single event
     * from this stream to an event that can be emitted from the returned
     * stream.
     * @param reduction
     * @param timeout
     * @param <U> type of events emitted from the returned stream.
     */
    default <U> EventStream<U> reduceCloseSuccessions(
            Function<T, U> initialTransformation,
            BiFunction<U, T, U> reduction,
            Duration timeout) {

        if(!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Not on FX application thread");
        }

        Timer timer = new TimelineTimer(timeout);
        return new SuccessionReducingStream<T, U>(
                this, initialTransformation, reduction, timer);
    }

    /**
     * A convenient method that can be used when it is more convenient to
     * supply an identity of the type {@code U} than to transform an event
     * of type {@code T} to an event of type {@code U}.
     * This method is equivalent to
     * {@code reduceCloseSuccessions(t -> reduction.apply(identitySupplier.get(), t), reduction, timeout)}.
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #reduceCloseSuccessions(Supplier, BiFunction, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param unitSupplier function that provides a unit element
     * (i.e. initial value for accumulation) of type {@code U}
     * @param reduction
     * @param timeout
     *
     * @see #reduceCloseSuccessions(Function, BiFunction, Duration)
     */
    default <U> EventStream<U> reduceCloseSuccessions(
            Supplier<U> unitSupplier,
            BiFunction<U, T, U> reduction,
            Duration timeout) {

        Function<T, U> map = t -> reduction.apply(unitSupplier.get(), t);
        return reduceCloseSuccessions(map, reduction, timeout);
    }

    /**
     * An analog to
     * {@link #reduceCloseSuccessions(BinaryOperator, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param reduction
     * @param timeout
     * @param scheduler used to schedule timeout expiration
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     * @return
     */
    default EventStream<T> reduceCloseSuccessions(
            BinaryOperator<T> reduction,
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        return reduceCloseSuccessions(
                t -> t, reduction, timeout, scheduler, eventThreadExecutor);
    }

    /**
     * An analog to
     * {@link #reduceCloseSuccessions(Function, BiFunction, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param initialTransformation
     * @param reduction
     * @param timeout
     * @param scheduler
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     * @return
     */
    default <U> EventStream<U> reduceCloseSuccessions(
            Function<T, U> initialTransformation,
            BiFunction<U, T, U> reduction,
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        Timer timer = new ScheduledExecutorServiceTimer(
                timeout, scheduler, eventThreadExecutor);
        return new SuccessionReducingStream<T, U>(
                this, initialTransformation, reduction, timer);
    }

    /**
     * An analog to
     * {@link #reduceCloseSuccessions(Supplier, BiFunction, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param unitSupplier
     * @param reduction
     * @param timeout
     * @param scheduler
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     * @return
     */
    default <U> EventStream<U> reduceCloseSuccessions(
            Supplier<U> unitSupplier,
            BiFunction<U, T, U> reduction,
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        Function<T, U> map = t -> reduction.apply(unitSupplier.get(), t);
        return reduceCloseSuccessions(
                map, reduction, timeout, scheduler, eventThreadExecutor);
    }

    /**
     * Transfers events from one thread to another.
     * Any event stream can only be accessed from a single thread.
     * This method allows to transfer events from one thread to another.
     * Any event emitted by this EventStream will be emitted by the returned
     * stream on a different thread.
     * @param sourceThreadExecutor executor that executes tasks on the thread
     * from which this EventStream is accessed.
     * @param targetThreadExecutor executor that executes tasks on the thread
     * from which the returned EventStream will be accessed.
     * @return Event stream that emits the same events as this EventStream,
     * but uses {@code targetThreadExecutor} to emit the events.
     */
    default EventStream<T> threadBridge(
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        return new ThreadBridge<T>(this, sourceThreadExecutor, targetThreadExecutor);
    }

    /**
     * Transfers events from the JavaFX application thread to another thread.
     * Equivalent to
     * {@code threadBridge(Platform::runLater, targetThreadExecutor)}.
     * @param targetThreadExecutor executor that executes tasks on the thread
     * from which the returned EventStream will be accessed.
     * @return Event stream that emits the same events as this EventStream,
     * but uses {@code targetThreadExecutor} to emit the events.
     * @see #threadBridge(Executor, Executor)
     */
    default EventStream<T> threadBridgeFromFx(Executor targetThreadExecutor) {
        return threadBridge(Platform::runLater, targetThreadExecutor);
    }

    /**
     * Transfers events to the JavaFX application thread.
     * Equivalent to
     * {@code threadBridge(sourceThreadExecutor, Platform::runLater)}.
     * @param sourceThreadExecutor executor that executes tasks on the thread
     * from which this EventStream is accessed.
     * @return Event stream that emits the same events as this EventStream,
     * but emits them on the JavaFX application thread.
     * @see #threadBridge(Executor, Executor)
     */
    default EventStream<T> threadBridgeToFx(Executor sourceThreadExecutor) {
        return threadBridge(sourceThreadExecutor, Platform::runLater);
    }

    /**
     * Returns a clone of this event stream guarded by the given guardians.
     * The returned event stream emits the same events as this event stream.
     * In addition to that, the emission of each event is guarded by the given
     * guardians: before the emission, guards are acquired in the given order;
     * after the emission, previously acquired guards are released in reverse
     * order.
     */
    default EventStream<T> guardedBy(Guardian... guardians) {
        return new GuardedStream<>(this, guardians);
    }
}

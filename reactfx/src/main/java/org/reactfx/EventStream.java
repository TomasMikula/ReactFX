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
        return EventStreams.filter(this, predicate);
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
        return EventStreams.map(this, f);
    }

    /**
     * A more efficient equivalent to
     * {@code filter(predicate).map(f)}.
     * @param predicate
     * @param f
     * @return
     */
    default <U> EventStream<U> filterMap(Predicate<T> predicate, Function<T, U> f) {
        return EventStreams.filterMap(this, predicate, f);
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

    /**
     * Returns a new event stream that emits values emitted from this stream
     * asynchronously mapped by the given function. In other words, for every
     * value emitted from this stream, the returned stream starts asynchronous
     * computation by invoking {@code f} on the emitted value. Once the
     * asynchronous computation completes, its result is emitted from the
     * returned stream on the JavaFX application thread.
     *
     * <p>Note that due to asynchrony events may be emitted from by the returned
     * stream in different order than from this stream.
     *
     * <p>If the asynchronous computation started by {@code f} fails, the error
     * is silently ignored and nothing is emitted from the returned stream. If
     * your computation can fail, you should handle the error in {@code f}.
     */
    default <U> EventStream<U> mapAsync(
            Function<T, CompletionStage<U>> f) {
        return mapAsync(f, Platform::runLater);
    }

    default <U> EventStream<U> mapAsync(
            Function<T, CompletionStage<U>> f,
            Executor clientThreadExecutor) {
        return new AsyncMap<>(this, f, clientThreadExecutor);
    }

    /**
     * Similar to {@link #mapAsync(Function)} with one difference: for events
     * <i>a</i> and <i>b</i> emitted from this stream in this order, if <i>b</i>
     * arrives before the asynchronous transformation of <i>a</i> is completed,
     * then <i>a</i> (and the result of its transformation) is discarded (not
     * emitted from the returned stream).
     * @see #mapAsync(Function)
     */
    default <U> EventStream<U> mapAsyncSkipOutdated(
            Function<T, CompletionStage<U>> f) {
        return mapAsyncSkipOutdated(f, Platform::runLater);
    }

    default <U> EventStream<U> mapAsyncSkipOutdated(
            Function<T, CompletionStage<U>> f,
            Executor clientThreadExecutor) {
        return new LatestOnlyAsyncMap<>(this, f, clientThreadExecutor);
    }

    /**
     * Similar to {@link #mapAsyncSkipOutdated(Function)} with one addition:
     * the result of the asynchronous transformation currently in progress, if
     * any, is discarded (i.e. not emitted from the returned stream) on every
     * event emitted from {@code canceller} (in addition to every event emitted
     * from this stream).
     */
    default <U> EventStream<U> mapAsyncSkipOutdated(
            Function<T, CompletionStage<U>> f,
            EventStream<?> canceller) {
        return mapAsyncSkipOutdated(f, canceller, Platform::runLater);
    }

    default <U> EventStream<U> mapAsyncSkipOutdated(
            Function<T, CompletionStage<U>> f,
            EventStream<?> canceller,
            Executor clientThreadExecutor) {
        return new CancellableLatestOnlyAsyncMap<>(
                this, f, canceller, clientThreadExecutor);
    }

    /**
     * Analog to {@link #mapAsync(Function)} for {@link Task}-based asynchronous
     * computation.
     * <p>Note that {@code f} must return tasks that have already been submitted
     * for execution.
     * @see #mapAsync(Function)
     */
    default <U> EventStream<U> mapInBackground(
            Function<T, Task<U>> f) {
        return new BackgroundMap<>(this, f);
    }

    /**
     * Analog to {@link #mapAsyncSkipOutdated(Function)} for {@link Task}-based
     * asynchronous computation.
     * <p>Note that {@code f} must return tasks that have already been submitted
     * for execution.
     * @see #mapAsyncSkipOutdated(Function)
     */
    default <U> EventStream<U> mapInBackgroundSkipOutdated(
            Function<T, Task<U>> f) {
        return new LatestOnlyBackgroundMap<>(this, f);
    }

    /**
     * Analog to {@link #mapAsyncSkipOutdated(Function, EventStream)} for
     * {@link Task}-based asynchronous computation.
     * <p>Note that {@code f} must return tasks that have already been submitted
     * for execution.
     * @see #mapAsyncSkipOutdated(Function, EventStream)
     */
    default <U> EventStream<U> mapInBackgroundSkipOutdated(
            Function<T, Task<U>> f,
            EventStream<?> canceller) {
        return new CancellableLatestOnlyBackgroundMap<>(this, f, canceller);
    }
}

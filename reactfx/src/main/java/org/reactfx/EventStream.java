package org.reactfx;

import java.time.Duration;
import java.util.Optional;
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
import javafx.beans.binding.Binding;
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
    Subscription subscribe(Consumer<? super T> consumer);

    /**
     * If this stream is a compound stream lazily subscribed to its inputs,
     * that is, subscribed to inputs only when it itself has some subscribers,
     * {@code pin}ning this stream causes it to stay subscribed until the
     * pinning is revoked by calling {@code unsubscribe()} on the returned
     * subscription.
     *
     * <p>Equivalent to {@code subscribe(x -> {})}.
     * @return subscription used to cancel the pinning
     */
    default Subscription pin() {
        return subscribe(x -> {});
    }

    /**
     * Returns an event stream that emits the same<sup>(*)</sup> events as this
     * stream, but before emitting each event performs the given side effect.
     * This is useful for debugging.
     *
     * <p>(*) The returned stream is lazily bound, so it only emits events and
     * performs side effects when it has at least one subscriber.
     */
    default EventStream<T> hook(Consumer<? super T> sideEffect) {
        return new SideEffectStream<>(this, sideEffect);
    }

    /**
     * Returns a new event stream that emits events emitted from this stream
     * that satisfy the given predicate.
     */
    default EventStream<T> filter(Predicate<? super T> predicate) {
        return new FilterStream<>(this, predicate);
    }

    /**
     * Filters this event stream by the runtime type of the values.
     * {@code filter(SomeClass.class)} is equivalent to
     * {@code filter(x -> x instanceof SomeClass).map(x -> (SomeClass) x)}.
     */
    default <U extends T> EventStream<U> filter(Class<U> subtype) {
        return filterMap(subtype::isInstance, subtype::cast);
    }

    /**
     * Returns an event stream that emits a value obtained from the given
     * supplier every time this event stream emits a value.
     */
    default <U> EventStream<U> supply(Supplier<? extends U> f) {
        return map(x -> f.get());
    }

    /**
     * Similar to {@link #supply(Supplier)}, but the returned stream is a
     * {@link CompletionStageStream}, which can be used to await the results
     * of asynchronous computation.
     */
    default <U> CompletionStageStream<U> supplyCompletionStage(Supplier<CompletionStage<U>> f) {
        return mapToCompletionStage(x -> f.get());
    }

    /**
     * Similar to {@link #supply(Supplier)}, but the returned stream is a
     * {@link CompletionStageStream}, which can be used to await the results
     * of asynchronous computation.
     */
    default <U> TaskStream<U> supplyTask(Supplier<Task<U>> f) {
        return mapToTask(x -> f.get());
    }

    /**
     * Returns a new event stream that applies the given function to every
     * value emitted from this stream and emits the result.
     */
    default <U> EventStream<U> map(Function<? super T, ? extends U> f) {
        return new MappedStream<>(this, f);
    }

    /**
     * Returns a new event stream that emits events emitted by this stream
     * cast to the given type.
     * {@code cast(SomeClass.class)} is equivalent to
     * {@code map(x -> (SomeClass) x)}.
     */
    default <U extends T> EventStream<U> cast(Class<U> subtype) {
        return map(subtype::cast);
    }

    /**
     * Similar to {@link #map(Function)}, but the returned stream is a
     * {@link CompletionStageStream}, which can be used to await the results
     * of asynchronous computation.
     */
    default <U> CompletionStageStream<U> mapToCompletionStage(Function<? super T, CompletionStage<U>> f) {
        return new MappedToCompletionStageStream<>(this, f);
    }

    /**
     * Similar to {@link #map(Function)}, but the returned stream is a
     * {@link TaskStream}, which can be used to await the results of
     * asynchronous computation.
     */
    default <U> TaskStream<U> mapToTask(Function<? super T, Task<U>> f) {
        return new MappedToTaskStream<>(this, f);
    }

    /**
     * A more efficient equivalent to
     * {@code filter(predicate).map(f)}.
     */
    default <U> EventStream<U> filterMap(Predicate<? super T> predicate, Function<? super T, ? extends U> f) {
        return new FilterMapStream<>(this, predicate, f);
    }

    /**
     * Returns a new event stream that, for each event <i>x</i> emitted from
     * this stream, obtains the event stream <i>f(x)</i> and keeps emitting its
     * events until the next event is emitted from this stream.
     */
    default <U> EventStream<U> flatMap(Function<? super T, ? extends EventStream<U>> f) {
        return new FlatMapStream<>(this, f);
    }

    /**
     * <pre>
     * {@code
     * stream.flatMapOpt(f)
     * }
     * </pre>
     * is equivalent to
     * <pre>
     * {@code
     * stream.map(f)
     *     .filter(Optional::isPresent)
     *     .map(Optional::get)
     * }
     * </pre>
     * @param f
     * @return
     */
    default <U> EventStream<U> flatMapOpt(Function<? super T, Optional<U>> f) {
        return new FlatMapOptStream<T, U>(this, f);
    }

    /**
     * Returns a new event stream that, when an event arrives from the
     * {@code impulse} stream, emits the most recent event emitted by this
     * stream. Each event is emitted at most once. For example, if events
     * are emitted in this order, [<i>i, a, i, b, c, i, i, d</i>], where <i>
     * a, b, c, d</i> come from this stream and <i>i</i>s come from the
     * {@code impulse} stream, then the returned stream emits [<i>a, c</i>].
     */
    default EventStream<T> emitOn(EventStream<?> impulse) {
        return new EmitOnStream<>(this, impulse);
    }

    /**
     * Returns a new event stream that, when an event arrives from the
     * {@code impulse} stream, emits the most recent event emitted by this
     * stream. The same event may be emitted more than once. For example, if
     * events are emitted in this order, [<i>i, a, i, b, c, i, i, d</i>], where
     * <i>a, b, c, d</i> come from this stream and <i>i</i>s come from the
     * {@code impulse} stream, then the returned stream emits [<i>a, c, c</i>].
     */
    default EventStream<T> emitOnEach(EventStream<?> impulse) {
        return new EmitOnEachStream<>(this, impulse);
    }

    /**
     * Similar to {@link #emitOnEach(EventStream)}, but also includes the
     * impulse in the emitted value. For example, if events are emitted in this
     * order, [<i>i1, a, i2, b, c, i3, i4, d</i>], where <i>a, b, c, d</i> come
     * from this stream and <i>i</i>s come from the {@code impulse} stream, then
     * the returned stream emits [<i>(a, i2), (c, i3), (c, i4)</i>].
     */
    default <I> BiEventStream<T, I> emitBothOnEach(EventStream<I> impulse) {
        return new EmitBothOnEachStream<>(this, impulse);
    }

    /**
     * Returns a new event stream that emits all the events emitted from this
     * stream and in addition to that re-emits the most recent event on every
     * event emitted from {@code impulse}.
     */
    default EventStream<T> repeatOn(EventStream<?> impulse) {
        return new RepeatOnStream<>(this, impulse);
    }

    default InterceptableEventStream<T> interceptable() {
        if(this instanceof InterceptableEventStream) {
            return (InterceptableEventStream<T>) this;
        } else {
            return new InterceptableEventStreamImpl<>(this);
        }
    }

    /**
     * Returns a binding that holds the most recent event emitted from this
     * stream. The returned binding stays subscribed to this stream until its
     * {@code dispose()} method is called.
     * @param initialValue used as the returned binding's value until this
     * stream emits the first value.
     * @return binding reflecting the most recently emitted value.
     */
    default Binding<T> toBinding(T initialValue) {
        return new StreamBinding<>(this, initialValue);
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
     * {@link #reduceSuccessions(BinaryOperator, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param reduction function to reduce two events into one.
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     */
    default AwaitingEventStream<T> reduceSuccessions(
            BinaryOperator<T> reduction,
            Duration timeout) {

        return reduceSuccessions(t -> t, reduction, timeout);
    }

    /**
     * A more general version of
     * {@link #reduceSuccessions(BinaryOperator, Duration)}
     * that allows the accumulated event to be of different type.
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #reduceSuccessions(Function, BiFunction, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param initialTransformation function to transform a single event
     * from this stream to an event that can be emitted from the returned
     * stream.
     * @param reduction function to accumulate an event to the stored value
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     * @param <U> type of events emitted from the returned stream.
     */
    default <U> AwaitingEventStream<U> reduceSuccessions(
            Function<? super T, ? extends U> initialTransformation,
            BiFunction<? super U, ? super T, ? extends U> reduction,
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
     * {@code reduceSuccessions(t -> reduction.apply(identitySupplier.get(), t), reduction, timeout)}.
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #reduceSuccessions(Supplier, BiFunction, Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param unitSupplier function that provides the unit element
     * (i.e. initial value for accumulation) of type {@code U}
     * @param reduction function to accumulate an event to the stored value
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     *
     * @see #reduceSuccessions(Function, BiFunction, Duration)
     */
    default <U> AwaitingEventStream<U> reduceSuccessions(
            Supplier<? extends U> unitSupplier,
            BiFunction<? super U, ? super T, ? extends U> reduction,
            Duration timeout) {

        Function<T, U> map = t -> reduction.apply(unitSupplier.get(), t);
        return reduceSuccessions(map, reduction, timeout);
    }

    /**
     * An analog to
     * {@link #reduceSuccessions(BinaryOperator, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param reduction function to reduce two events into one.
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     * @param scheduler used to schedule timeout expiration
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     */
    default AwaitingEventStream<T> reduceSuccessions(
            BinaryOperator<T> reduction,
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        return reduceSuccessions(
                t -> t, reduction, timeout, scheduler, eventThreadExecutor);
    }

    /**
     * An analog to
     * {@link #reduceSuccessions(Function, BiFunction, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param initialTransformation function to transform a single event
     * from this stream to an event that can be emitted from the returned
     * stream.
     * @param reduction function to accumulate an event to the stored value
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     * @param scheduler used to schedule timeout expiration
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     */
    default <U> AwaitingEventStream<U> reduceSuccessions(
            Function<? super T, ? extends U> initialTransformation,
            BiFunction<? super U, ? super T, ? extends U> reduction,
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
     * {@link #reduceSuccessions(Supplier, BiFunction, Duration)}
     * to use outside of JavaFX application thread.
     *
     * @param unitSupplier function that provides the unit element
     * @param reduction function to accumulate an event to the stored value
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     * @param scheduler used to schedule timeout expiration
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     */
    default <U> AwaitingEventStream<U> reduceSuccessions(
            Supplier<? extends U> unitSupplier,
            BiFunction<? super U, ? super T, ? extends U> reduction,
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        Function<T, U> map = t -> reduction.apply(unitSupplier.get(), t);
        return reduceSuccessions(
                map, reduction, timeout, scheduler, eventThreadExecutor);
    }

    /**
     * Returns an event stream that, when events are emitted from this stream
     * in close temporal succession, emits only the last event of the
     * succession. What is considered a <i>close temporal succession</i> is
     * defined by {@code timeout}: time gap between two successive events must
     * be at most {@code timeout}.
     *
     * <p>This method is a shortcut for
     * {@code reduceSuccessions((a, b) -> b, timeout)}.</p>
     *
     * <p><b>Note:</b> This function can be used only when this stream and
     * the returned stream are used from the JavaFX application thread. If
     * you are using the event streams on a different thread, use
     * {@link #successionEnds(Duration, ScheduledExecutorService, Executor)}
     * instead.</p>
     *
     * @param timeout the maximum time difference between two subsequent events
     * in a <em>close</em> succession.
     */
    default AwaitingEventStream<T> successionEnds(Duration timeout) {
        return reduceSuccessions((a, b) -> b, timeout);
    }

    /**
     * An analog to {@link #successionEnds(Duration)} to use outside of JavaFX
     * application thread.
     * @param timeout the maximum time difference between two subsequent events
     * in a <em>close</em> succession.
     * @param scheduler used to schedule timeout expiration
     * @param eventThreadExecutor executor that executes actions on the
     * thread on which this stream's events are emitted. The returned stream
     * will use this executor to emit events.
     */
    default AwaitingEventStream<T> successionEnds(
            Duration timeout,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {

        return reduceSuccessions((a, b) -> b, timeout, scheduler, eventThreadExecutor);
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

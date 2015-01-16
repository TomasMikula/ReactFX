package org.reactfx;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.concurrent.Task;

import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.Either;
import org.reactfx.util.FxTimer;
import org.reactfx.util.NotificationAccumulator;
import org.reactfx.util.Timer;
import org.reactfx.util.Tuple2;

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
     * Get notified every time this event stream emits a value.
     * @param subscriber handles emitted events.
     * @return subscription that can be used to stop observing this event
     * stream.
     */
    Subscription subscribe(Consumer<? super T> subscriber);

    /**
     * Starts pushing all events emitted by this stream to the given event sink.
     * <p>{@code stream.feedTo(sink)} is equivalent to
     * {@code sink.feedFrom(stream)}
     * @param sink event sink to which this event stream's events will be pushed
     * @return subscription that can be used to stop delivering this stream's
     * events to {@code sink}.
     * @see EventSink#feedFrom(EventStream)
     */
    default Subscription feedTo(EventSink<? super T> sink) {
        return subscribe(sink::push);
    }

    /**
     * Starts setting all events emitted by this stream as the value of the
     * given writable value. This is a shortcut for
     * {@code subscribe(dest::setValue)}.
     */
    default Subscription feedTo(WritableValue<? super T> dest) {
        return subscribe(dest::setValue);
    }

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
     * This is useful for debugging. The side effect is not allowed to cause
     * recursive event emission from this stream. If it does, the returned
     * stream reports an error and does not emit the event whose side effect
     * caused the recursion.
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
     * Returns a new event stream that emits repetitive events only once.
     */
    default EventStream<T> distinct() {
        return new DistinctStream<>(this);
    }

    /**
     * Returns an event stream that emits the given constant value every time
     * this stream emits a value.
     */
    default <U> EventStream<U> supply(U value) {
        return map(x -> value);
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
     * Returns a new event stream that, for event {@code e} emitted from this
     * stream, emits {@code left(e)} if {@code e} passes the given test, and
     * emits {@code right(e)} if {@code e} does not pass the test.
     */
    default EventStream<Either<T, T>> splitBy(Predicate<? super T> test) {
        return map(t -> test.test(t) ? Either.left(t) : Either.right(t));
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
    default <U> EventStream<U> filterMap(
            Predicate<? super T> predicate,
            Function<? super T, ? extends U> f) {
        return new FilterMapStream<>(this, predicate, f);
    }

    /**
     * Equivalent to
     * <pre>
     * {@code
     * map(f)
     *     .filter(Optional::isPresent)
     *     .map(Optional::get)
     * }
     * </pre>
     * with more efficient implementation.
     */
    default <U> EventStream<U> filterMap(Function<? super T, Optional<U>> f) {
        return new FlatMapOptStream<T, U>(this, f);
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
     * Returns an event stream that emits all the events emitted from either
     * this stream or the {@code right} stream. An event <i>t</i> emitted from
     * this stream is emitted as {@code Either.left(t)}. An event <i>u</i>
     * emitted from the {@code right} stream is emitted as
     * {@code Either.right(u)}.
     *
     * @see EventStreams#merge(EventStream...)
     */
    default <U> EventStream<Either<T, U>> or(EventStream<? extends U> right) {
        EventStream<T> left = this;
        return new EventStreamBase<Either<T, U>>() {
            @Override
            protected Subscription observeInputs() {
                return Subscription.multi(
                        left.subscribe(l -> emit(Either.<T, U>left(l))),
                        right.subscribe(r -> emit(Either.<T, U>right(r))));
            }
        };
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
    default <I> EventStream<Tuple2<T, I>> emitBothOnEach(EventStream<I> impulse) {
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

    /**
     * Returns a suspendable event stream that, when suspended, suppresses
     * any events emitted by this event stream.
     */
    default SuspendableEventStream<T> suppressible() {
        return new SuppressibleEventStream<>(this);
    }

    /**
     * Shortcut for {@code suppressible().suspendWhen(condition)}.
     */
    default EventStream<T> suppressWhen(ObservableValue<Boolean> condition) {
        return suppressible().suspendWhen(condition);
    }

    /**
     * Returns a suspendable event stream that, when suspended, stores the
     * events emitted by this event stream and emits them when the returned
     * stream's emission is resumed.
     */
    default SuspendableEventStream<T> pausable() {
        return new PausableEventStream<>(this);
    }

    /**
     * Shortcut for {@code pausable().suspendWhen(condition)}.
     */
    default EventStream<T> pauseWhen(ObservableValue<Boolean> condition) {
        return pausable().suspendWhen(condition);
    }

    /**
     * Returns a suspendable event stream that, when suspended, forgets all but
     * the latest event emitted by this event stream. The remembered event, if
     * any, is emitted from the returned stream upon resume.
     */
    default SuspendableEventStream<T> forgetful() {
        return new ForgetfulEventStream<>(this);
    }

    /**
     * Shortcut for {@code forgetful().suspendWhen(condition)}.
     */
    default EventStream<T> retainLatestWhen(ObservableValue<Boolean> condition) {
        return forgetful().suspendWhen(condition);
    }

    /**
     * Returns a suspendable event stream that, when suspended, reduces incoming
     * events by the given {@code reduction} function into one. The reduced
     * event is emitted from the returned stream upon resume.
     *
     * <p>Note that {@link #forgetful()} is equivalent to
     * {@code reducible((a, b) -> b)}.
     */
    default SuspendableEventStream<T> reducible(BinaryOperator<T> reduction) {
        return new ReducibleEventStream<>(this, reduction);
    }

    /**
     * Shortcut for {@code reducible(reduction).suspendWhen(condition)}.
     */
    default EventStream<T> reduceWhen(
            ObservableValue<Boolean> condition,
            BinaryOperator<T> reduction) {
        return reducible(reduction).suspendWhen(condition);
    }

    /**
     * Returns a suspendable event stream that, when suspended, accumulates
     * incoming events to a cumulative value of type {@code A}. When the
     * returned stream is resumed, the accumulated value is deconstructed into
     * a sequence of events that are emitted from the returned stream.
     *
     * <p>Note that {@link #suppressible()} is equivalent to
     * <pre>
     * {@code
     * accumulative(
     *     t -> (Void) null,                        // use null as accumulator
     *     (a, t) -> a,                             // keep null as accumulator
     *     a -> AccumulatorSize.ZERO,               // no events to be emitted from accumulator
     *     a -> throw new NoSuchElementException(), // head is never called on empty accumulator
     *     a -> throw new NoSuchElementException()) // tail is never called on empty accumulator
     * }
     * </pre>
     *
     * <p>Note that {@code reducible(reduction)} is equivalent to
     * <pre>
     * {@code
     * accumulative(
     *     t -> t,                                // the event itself is the accumulator
     *     reduction,
     *     t -> AccumulatorSize.ONE,              // one event to be emitted
     *     t -> t,                                // head of a single value is the value itself
     *     t -> throw new NoSuchElementException) // tail is never called on accumulator of size one
     * }
     * </pre>
     *
     * @param initialTransformation Used to convert the first event after
     * suspension to the cumulative value.
     * @param accumulation Used to accumulate further incoming events to the
     * cumulative value.
     * @param size determines how many events can be emitted from the current
     * cumulative value.
     * @param head produces the first event off the cumulative value.
     * @param tail returns a cumulative value that produces the same events
     * as the given cumulative value, except the event returned by {@code head}.
     * May be destructive for the given cumulative value.
     * @param <A> type of the cumulative value
     */
    default <A> SuspendableEventStream<T> accumulative(
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        return new AccumulativeEventStream<>(
                this, initialTransformation, accumulation, size, head, tail);
    }

    /**
     * Shortcut for
     * <pre>
     * {@code
     * accumulative(initialTransformation, accumulation, size, head, tail)
     *     .suspendWhen(condition)}
     * </pre>
     */
    default <A> EventStream<T> accumulateWhen(
            ObservableValue<Boolean> condition,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        return accumulative(initialTransformation, accumulation, size, head, tail)
                .suspendWhen(condition);
    }

    /**
     * A variation on
     * {@link #accumulative(Function, BiFunction, Function, Function, Function)}
     * to use when it is more convenient to provide a unit element of the
     * accumulation than to transform the initial event to a cumulative
     * value. It is equivalent to
     * {@code accumulative(t -> accumulation.apply(unit.get(), t), accumulation, size, head, tail)},
     * i.e. the initial transformation is achieved by accumulating the initial
     * event to the unit element.
     *
     * <p>Note that {@link #pausable()} is equivalent to
     * <pre>
     * {@code
     * accumulative(
     *     LinkedList<T>::new,                     // the unit element is an empty queue
     *     (q, t) -> { q.addLast(t); return q; },  // accumulation is addition to the queue
     *     q -> AccumulatorSize.fromInt(q.size()), // size is the size of the queue
     *     Deque::getFirst,                        // head is the first element of the queue
     *     q -> { q.removeFirst(); return q; })    // tail removes the first element from the queue
     * }
     * </pre>
     *
     * @param unit Function that supplies unit element of the accumulation.
     * @param accumulation Used to accumulate further incoming events to the
     * cumulative value.
     * @param size determines how many events can be emitted from the current
     * cumulative value.
     * @param head produces the first event off the cumulative value.
     * @param tail returns a cumulative value that produces the same events
     * as the given cumulative value, except the event returned by {@code head}.
     * May be destructive for the given cumulative value.
     * @param <A> type of the cumulative value
     */
    default <A> SuspendableEventStream<T> accumulative(
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        Function<? super T, ? extends A> initialTransformation =
                t -> accumulation.apply(unit.get(), t);
        return accumulative(
                initialTransformation, accumulation, size, head, tail);
    }

    /**
     * Shortcut for
     * <pre>
     * {@code
     * accumulative(unit, accumulation, size, head, tail)
     *     .suspendWhen(condition)}
     * </pre>
     */
    default <A> EventStream<T> accumulateWhen(
            ObservableValue<Boolean> condition,
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        return accumulative(unit, accumulation, size, head, tail)
                .suspendWhen(condition);
    }

    /**
     * Returns an event stream that, when an event arrives from this stream,
     * transforms it into a cumulative value using the
     * {@code initialTransformation} function. Any further events that arrive
     * from this stream are accumulated to the cumulative value using the
     * {@code accumulation} function. When an event arrives from the
     * {@code ticks} stream, the accumulated value is deconstructed into a
     * sequence of events using the {@code deconstruction} function and the
     * events are emitted from the returned stream.
     *
     * <p>Note that {@code reduceBetween(ticks, reduction)} is equivalent to
     * {@code accumulateBetween(ticks, t -> t, reduction, Collections::singletonList)}.
     */
    default <A> EventStream<T> accumulateBetween(
            EventStream<?> ticks,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        return new AccumulateBetweenStream<>(
                this, ticks, initialTransformation, accumulation, deconstruction);
    }

    /**
     * A variation on
     * {@link #accumulateBetween(EventStream, Function, BiFunction, Function)}
     * to use when it is more convenient to provide a unit element of the
     * accumulation than to transform the initial event to a cumulative
     * value. It is equivalent to
     * {@code accumulateBetween(ticks, t -> accumulation.apply(unit.get(), t), accumulation, deconstruction)},
     * i.e. the initial transformation is achieved by accumulating the initial
     * event to the unit element.
     *
     * <p>Note that {@code queueBetween(ticks)} is equivalent to
     * {@code accumulateBetween(ticks, ArrayList<T>::new, (l, t) -> { l.add(t); return l; }, l -> l)},
     * i.e. the unit element is an empty list, accumulation is addition to the
     * list and deconstruction of the accumulated value is a no-op, since the
     * accumulated value is already a list of events.
     */
    default <A> EventStream<T> accumulateBetween(
            EventStream<?> ticks,
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        Function<? super T, ? extends A> initialTransformation =
                t -> accumulation.apply(unit.get(), t);
        return accumulateBetween(
                ticks, initialTransformation, accumulation, deconstruction);
    }

    /**
     * Returns an event stream that, when an event arrives from this stream,
     * stores it for emission. Any further events that arrive from this stream
     * are reduced into the stored event using the {@code reduction} function.
     * The stored event is emitted from the returned stream when a <i>tick</i>
     * arrives from the {@code ticks} stream.
     *
     * <p>Note that {@code retainLatestBetween(ticks)} is equivalent to
     * {@code reduceBetween(ticks, (a, b) -> b)}.
     */
    default EventStream<T> reduceBetween(
            EventStream<?> ticks,
            BinaryOperator<T> reduction) {
        return accumulateBetween(
                ticks,
                Function.<T>identity(),
                reduction,
                Collections::singletonList);
    }

    /**
     * Returns an event stream that, when an event arrives from this stream,
     * enqueues it for emission. Queued events are emitted from the returned
     * stream when a <i>tick</i> arrives from the {@code ticks} stream.
     */
    default EventStream<T> queueBetween(EventStream<?> ticks) {
        return accumulateBetween(
                ticks,
                (Supplier<List<T>>) ArrayList::new,
                (l, t) -> { l.add(t); return l; },
                Function.identity());
    }

    /**
     * Equivalent to {@link #emitOn(EventStream)}.
     */
    default EventStream<T> retainLatestBetween(EventStream<?> ticks) {
        return emitOn(ticks);
    }

    /**
     * Version of {@link #accumulateUntilLater(Function, BiFunction, Function)}
     * for event streams that don't live on the JavaFX application thread.
     * @param eventThreadExecutor executor that executes actions on the thread
     * from which this event stream is accessed.
     */
    default <A> EventStream<T> accumulateUntilLater(
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction,
            Executor eventThreadExecutor) {
        return new AccumulateUntilLaterStream<>(
                this,
                initialTransformation,
                accumulation,
                deconstruction,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that, when an event is emitted from this stream,
     * transforms the event to a cumulative value using the
     * {@code initialTransformation} function and schedules emission using
     * {@link Platform#runLater(Runnable)}, if not already scheduled. Any new
     * event that arrives from this stream before the scheduled emission is
     * executed is accumulated to the stored cumulative value using the given
     * {@code accumulation} function. When the scheduled emission is finally
     * executed, the accumulated value is deconstructed into a sequence of
     * events using the {@code deconstruction} function and the events are
     * emitted from the returned stream.
     *
     * <p>Note that {@code reduceUntilLater(reduction)} is equivalent to
     * {@code accumulateUntilLater(t -> t, reduction, t -> Collections::singletonList)}.
     *
     * @param <A> type of the cumulative value (accumulator)
     */
    default <A> EventStream<T> accumulateUntilLater(
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        return accumulateUntilLater(
                initialTransformation,
                accumulation,
                deconstruction,
                Platform::runLater);
    }

    /**
     * Version of {@link #accumulateUntilLater(Supplier, BiFunction, Function)}
     * for event streams that don't live on the JavaFX application thread.
     * @param eventThreadExecutor executor that executes actions on the thread
     * from which this event stream is accessed.
     */
    default <A> EventStream<T> accumulateUntilLater(
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction,
            Executor eventThreadExecutor) {
        return accumulateUntilLater(
                t -> accumulation.apply(unit.get(), t),
                accumulation,
                deconstruction,
                eventThreadExecutor);
    }

    /**
     * A variation on
     * {@link #accumulateUntilLater(Function, BiFunction, Function)}
     * to use when it is more convenient to provide a unit element of the
     * accumulation than to transform the initial event to a cumulative
     * value. It is equivalent to
     * {@code accumulateUntilLater(t -> accumulation.apply(unit.get(), t), accumulation, deconstruction)},
     * i.e. the initial transformation is achieved by accumulating the initial
     * event to the unit element.
     *
     * <p>Note that {@link #queueUntilLater()} is equivalent to
     * {@code accumulateUntilLater(ArrayList<T>::new, (l, t) -> { l.add(t); return l; }, l -> l)},
     * i.e. the unit element is an empty list, accumulation is addition to the
     * list and deconstruction of the accumulated value is a no-op, since the
     * accumulated value is already a list of events.
     *
     * @param <A> type of the cumulative value (accumulator)
     */
    default <A> EventStream<T> accumulateUntilLater(
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> accumulation,
            Function<? super A, List<T>> deconstruction) {
        return accumulateUntilLater(
                unit,
                accumulation,
                deconstruction,
                Platform::runLater);
    }

    /**
     * Version of {@link #reduceUntilLater(BinaryOperator)} for event streams
     * that don't live on the JavaFX application thread.
     * @param eventThreadExecutor executor that executes actions on the thread
     * from which this event stream is accessed.
     */
    default EventStream<T> reduceUntilLater(
            BinaryOperator<T> reduction,
            Executor eventThreadExecutor) {
        return accumulateUntilLater(
                Function.<T>identity(),
                reduction,
                Collections::singletonList,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that, when an event is emitted from this stream,
     * stores the event for emission and schedules emission using
     * {@link Platform#runLater(Runnable)}, if not already scheduled. Any new
     * event that arrives from this stream before the scheduled emission is
     * executed is accumulated into the stored event using the given
     * {@code reduction} function. When the scheduled emission is finally
     * executed, the stored event is emitted from the returned stream.
     *
     * <p>Note that {@link #retainLatestUntilLater()} is equivalent to
     * {@code reduceUntilLater((a, b) -> b)}.
     */
    default EventStream<T> reduceUntilLater(BinaryOperator<T> reduction) {
        return reduceUntilLater(reduction, Platform::runLater);
    }

    /**
     * Version of {@link #retainLatestUntilLater()} for event streams that
     * don't live on the JavaFX application thread.
     * @param eventThreadExecutor executor that executes actions on the thread
     * from which this event stream is accessed.
     */
    default EventStream<T> retainLatestUntilLater(Executor eventThreadExecutor) {
        return reduceUntilLater((a, b) -> b, eventThreadExecutor);
    }

    /**
     * Returns an event stream that, when an event is emitted from this stream,
     * stores the event for emission and schedules emission using
     * {@link Platform#runLater(Runnable)}, if not already scheduled. If a new
     * event arrives from this stream before the scheduled emission is executed,
     * the stored event is overwritten by the new event and only the new event is
     * emitted when the scheduled emission is finally executed.
     */
    default EventStream<T> retainLatestUntilLater() {
        return retainLatestUntilLater(Platform::runLater);
    }

    /**
     * Version of {@link #queueUntilLater()} for event streams that don't live
     * on the JavaFX application thread.
     * @param eventThreadExecutor executor that executes actions on the thread
     * from which this event stream is accessed.
     */
    default EventStream<T> queueUntilLater(Executor eventThreadExecutor) {
        return accumulateUntilLater(
                (Supplier<List<T>>) ArrayList::new,
                (l, t) -> { l.add(t); return l; },
                l -> l,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that, when an event is emitted from this stream,
     * enqueues the event for emission and schedules emission using
     * {@link Platform#runLater(Runnable)}, if not already scheduled. Any events
     * that arrive from this stream before a scheduled emission is executed are
     * enqueued as well and emitted (in order) when the scheduled emission is
     * finally executed.
     */
    default EventStream<T> queueUntilLater() {
        return queueUntilLater(Platform::runLater);
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
     * stream and emits the accumulated value every time this stream emits a
     * value.
     * @param reduction function to reduce two events into one.
     */
    default EventStream<T> accumulate(BinaryOperator<T> reduction) {
        return accumulate(reduction, Function.identity());
    }

    /**
     * Returns an event stream that accumulates events emitted from this event
     * stream and emits the accumulated value every time this stream emits a
     * value.
     * @param unit initial value of the accumulated value.
     * @param reduction function to add an event to the accumulated value.
     */
    default <U> EventStream<U> accumulate(
            U unit,
            BiFunction<? super U, ? super T, ? extends U> reduction) {
        return accumulate(reduction, t -> reduction.apply(unit, t));
    }

    /**
     * Returns an event stream that accumulates events emitted from this event
     * stream and emits the accumulated value every time this stream emits a
     * value.
     * @param reduction function to add an event to the accumulated value.
     * @param initialTransformation function to transform the first event from
     * this stream to an event that can be emitted from the returned stream.
     * Subsequent events emitted from this stream are accumulated to the value
     * returned from this function.
     */
    default <U> EventStream<U> accumulate(
            BiFunction<? super U, ? super T, ? extends U> reduction,
            Function<? super T, ? extends U> initialTransformation) {
        return new AccumulatingStream<>(this, initialTransformation, reduction);
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

        return reduceSuccessions(Function.identity(), reduction, timeout);
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
     * @param reduction function to add an event to the accumulated value.
     * @param timeout the maximum time difference between two subsequent
     * events that can still be accumulated.
     * @param <U> type of events emitted from the returned stream.
     */
    default <U> AwaitingEventStream<U> reduceSuccessions(
            Function<? super T, ? extends U> initialTransformation,
            BiFunction<? super U, ? super T, ? extends U> reduction,
            Duration timeout) {

        Function<Runnable, Timer> timerFactory =
                action -> FxTimer.create(timeout, action);
        return new SuccessionReducingStream<T, U>(
                this, initialTransformation, reduction, timerFactory);
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
     * @param reduction function to add an event to the accumulated value.
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
                Function.identity(), reduction, timeout,
                scheduler, eventThreadExecutor);
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

        Function<Runnable, Timer> timerFactory =
                action -> ScheduledExecutorServiceTimer.create(
                        timeout, action, scheduler, eventThreadExecutor);
        return new SuccessionReducingStream<T, U>(
                this, initialTransformation, reduction, timerFactory);
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
     * Returns an event stream that emits the first event emitted from this
     * stream and then, if the next event arrives within the given duration
     * since the last emitted event, it is converted to an accumulator value
     * using {@code initialTransformation}. Any further events that still
     * arrive within {@code duration} are accumulated to the accumulator value
     * using the given reduction function. After {@code duration} has passed
     * since the last emitted event, the accumulator value is deconstructed
     * into a series of events using the given {@code deconstruction} function
     * and these events are emitted, the accumulator value is cleared and any
     * events that arrive within {@code duration} are accumulated, and so on.
     */
    default <A> AwaitingEventStream<T> thenAccumulateFor(
            Duration duration,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, List<T>> deconstruction) {
        return new ThenAccumulateForStream<>(
                this,
                initialTransformation,
                reduction,
                deconstruction,
                action -> FxTimer.create(duration, action));
    }

    default <A> AwaitingEventStream<T> thenAccumulateFor(
            Duration duration,
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, List<T>> deconstruction,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        Function<Runnable, Timer> timerFactory = action ->
                ScheduledExecutorServiceTimer.create(
                        duration, action, scheduler, eventThreadExecutor);
        return new ThenAccumulateForStream<>(
                this,
                initialTransformation,
                reduction,
                deconstruction,
                timerFactory);
    }

    /**
     * A variant of
     * {@link #thenAccumulateFor(Duration, Function, BiFunction, Function)}
     * for cases when it is more convenient to provide a unit element for
     * accumulation than the initial transformation.
     */
    default <A> AwaitingEventStream<T> thenAccumulateFor(
            Duration duration,
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, List<T>> deconstruction) {
        Function<? super T, ? extends A> initialTransformation =
                t -> reduction.apply(unit.get(), t);
        return thenAccumulateFor(
                duration,
                initialTransformation,
                reduction,
                deconstruction);
    }

    default <A> AwaitingEventStream<T> thenAccumulateFor(
            Duration duration,
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, List<T>> deconstruction,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        Function<? super T, ? extends A> initialTransformation =
                t -> reduction.apply(unit.get(), t);
        return thenAccumulateFor(
                duration,
                initialTransformation,
                reduction,
                deconstruction,
                scheduler,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that emits the first event emitted from this
     * stream and then reduces all following events that arrive within the
     * given duration into a single event using the given reduction function.
     * The resulting event, if any, is emitted after {@code duration} has
     * passed. Then again, any events that arrive within {@code duration} are
     * reduced into a single event, that is emitted after {@code duration} has
     * passed, and so on.
     */
    default AwaitingEventStream<T> thenReduceFor(
            Duration duration,
            BinaryOperator<T> reduction) {
        return thenAccumulateFor(
                duration,
                Function.identity(),
                reduction,
                Collections::singletonList);
    }

    default AwaitingEventStream<T> thenReduceFor(
            Duration duration,
            BinaryOperator<T> reduction,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return thenAccumulateFor(
                duration,
                Function.identity(),
                reduction,
                Collections::singletonList,
                scheduler,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that emits the first event emitted from this
     * stream and then remembers, but does not emit, the latest event emitted
     * from this stream. The remembered event is emitted after the given
     * duration from the last emitted event. This repeats after each emitted
     * event.
     */
    default AwaitingEventStream<T> thenRetainLatestFor(Duration duration) {
        return thenReduceFor(duration, (a, b) -> b);
    }

    default AwaitingEventStream<T> thenRetainLatestFor(
            Duration duration,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return thenReduceFor(
                duration,
                (a, b) -> b,
                scheduler,
                eventThreadExecutor);
    }

    /**
     * Returns an event stream that emits the first event emitted from this
     * stream and then ignores the following events for the given duration.
     * The first event that arrives after the given duration is emitted and
     * following events are ignored for the given duration again, and so on.
     */
    default AwaitingEventStream<T> thenIgnoreFor(Duration duration) {
        return thenAccumulateFor(
                duration,
                t -> Collections.<T>emptyList(),
                (l, t) -> l,
                Function.<List<T>>identity());
    }

    default AwaitingEventStream<T> thenIgnoreFor(
            Duration duration,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return thenAccumulateFor(
                duration,
                t -> Collections.<T>emptyList(),
                (l, t) -> l,
                Function.<List<T>>identity(),
                scheduler,
                eventThreadExecutor);
    }

    default <A> EventStream<T> onRecurseAccumulate(
            Function<? super T, ? extends A> initialTransformation,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        return new RecursiveStream<T>(
                this,
                NotificationAccumulator.accumulativeStreamNotifications(
                        size, head, tail, initialTransformation, reduction));
    }

    default <A> EventStream<T> onRecurseAccumulate(
            Supplier<? extends A> unit,
            BiFunction<? super A, ? super T, ? extends A> reduction,
            Function<? super A, AccumulatorSize> size,
            Function<? super A, ? extends T> head,
            Function<? super A, ? extends A> tail) {
        Function<? super T, ? extends A> initialTransformation =
                t -> reduction.apply(unit.get(), t);
        return onRecurseAccumulate(
                initialTransformation, reduction, size, head, tail);
    }

    default EventStream<T> onRecurseReduce(BinaryOperator<T> reduction) {
        return new RecursiveStream<T>(
                this, NotificationAccumulator.reducingStreamNotifications(reduction));
    }

    default EventStream<T> onRecurseQueue() {
        return new RecursiveStream<T>(
                this, NotificationAccumulator.queuingStreamNotifications());
    }

    default EventStream<T> onRecurseRetainLatest() {
        return new RecursiveStream<T>(this, NotificationAccumulator.retainLatestStreamNotifications());
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

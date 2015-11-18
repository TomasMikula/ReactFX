package org.reactfx;

import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import org.reactfx.collection.ListModification;
import org.reactfx.collection.LiveList;
import org.reactfx.util.*;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.reactfx.util.Tuples.t;

public class EventStreams {

    private static final class Never<T>
    extends RigidObservable<Consumer<? super T>>
    implements EventStream<T> {}

    private static final EventStream<?> NEVER = new Never<Object>();

    /**
     * Returns an event stream that never emits any value.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventStream<T> never() {
        return (EventStream<T>) NEVER;
    }

    /**
     * Creates an event stream that emits an impulse on every invalidation
     * of the given observable.
     */
    public static EventStream<Void> invalidationsOf(Observable observable) {
        return new EventStreamBase<Void>() {
            @Override
            protected Subscription observeInputs() {
                InvalidationListener listener = obs -> emit(null);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    /**
     * Creates an event stream that emits the given observable immediately for
     * every subscriber and re-emits it on every subsequent invalidation of the
     * observable.
     */
    public static <O extends Observable>
    EventStream<O> repeatOnInvalidation(O observable) {
        return new EventStreamBase<O>() {
            @Override
            protected Subscription observeInputs() {
                InvalidationListener listener = obs -> emit(observable);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }

            @Override
            protected void newObserver(Consumer<? super O> subscriber) {
                subscriber.accept(observable);
            }
        };
    }

    /**
     * Creates an event stream that emits the value of the given
     * {@code ObservableValue} immediately for every subscriber and then on
     * every change.
     */
    public static <T> EventStream<T> valuesOf(ObservableValue<T> observable) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(val);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }

            @Override
            protected void newObserver(Consumer<? super T> subscriber) {
                subscriber.accept(observable.getValue());
            }
        };
    }

    public static <T> EventStream<T> nonNullValuesOf(ObservableValue<T> observable) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                ChangeListener<T> listener = (obs, old, val) -> {
                    if(val != null) {
                        emit(val);
                    }
                };
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }

            @Override
            protected void newObserver(Consumer<? super T> subscriber) {
                T val = observable.getValue();
                if(val != null) {
                    subscriber.accept(val);
                }
            }
        };
    }

    public static <T> EventStream<Change<T>> changesOf(ObservableValue<T> observable) {
        return new EventStreamBase<Change<T>>() {
            @Override
            protected Subscription observeInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(new Change<>(old, val));
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    /**
     * @see LiveList#changesOf(ObservableList)
     */
    public static <T> EventStream<ListChangeListener.Change<? extends T>> changesOf(ObservableList<T> list) {
        return new EventStreamBase<ListChangeListener.Change<? extends T>>() {
            @Override
            protected Subscription observeInputs() {
                ListChangeListener<T> listener = c -> emit(c);
                list.addListener(listener);
                return () -> list.removeListener(listener);
            }
        };
    }

    /**
     * Use only when the subscriber does not cause {@code list} modification
     * of the underlying list.
     */
    public static <T> EventStream<ListModification<? extends T>> simpleChangesOf(ObservableList<T> list) {
        return new EventStreamBase<ListModification<? extends T>>() {
            @Override
            protected Subscription observeInputs() {
                return LiveList.observeChanges(list, c ->  {
                    for(ListModification<? extends T> mod: c) {
                        emit(mod);
                    }
                });
            }
        };
    }

    public static <T> EventStream<SetChangeListener.Change<? extends T>> changesOf(ObservableSet<T> set) {
        return new EventStreamBase<SetChangeListener.Change<? extends T>>() {
            @Override
            protected Subscription observeInputs() {
                SetChangeListener<T> listener = c -> emit(c);
                set.addListener(listener);
                return () -> set.removeListener(listener);
            }
        };
    }

    public static <K, V> EventStream<MapChangeListener.Change<? extends K, ? extends V>> changesOf(ObservableMap<K, V> map) {
        return new EventStreamBase<MapChangeListener.Change<? extends K, ? extends V>>() {
            @Override
            protected Subscription observeInputs() {
                MapChangeListener<K, V> listener = c -> emit(c);
                map.addListener(listener);
                return () -> map.removeListener(listener);
            }
        };
    }

    public static <C extends Collection<?> & Observable> EventStream<Integer> sizeOf(C collection) {
        return create(() -> collection.size(), collection);
    }

    public static EventStream<Integer> sizeOf(ObservableMap<?, ?> map) {
        return create(() -> map.size(), map);
    }

    private static <T> EventStream<T> create(Supplier<? extends T> computeValue, Observable... dependencies) {
        return new EventStreamBase<T>() {
            private T previousValue;

            @Override
            protected Subscription observeInputs() {
                InvalidationListener listener = obs -> {
                    T value = computeValue.get();
                    if(value != previousValue) {
                        previousValue = value;
                        emit(value);
                    }
                };
                for(Observable dep: dependencies) {
                    dep.addListener(listener);
                }
                previousValue = computeValue.get();

                return () -> {
                    for(Observable dep: dependencies) {
                        dep.removeListener(listener);
                    }
                };
            }

            @Override
            protected void newObserver(Consumer<? super T> subscriber) {
                subscriber.accept(previousValue);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(
            Node node, EventType<T> eventType) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                EventHandler<T> handler = this::emit;
                node.addEventHandler(eventType, handler);
                return () -> node.removeEventHandler(eventType, handler);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(
            Scene scene, EventType<T> eventType) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                EventHandler<T> handler = this::emit;
                scene.addEventHandler(eventType, handler);
                return () -> scene.removeEventHandler(eventType, handler);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(
            MenuItem menuItem, EventType<T> eventType) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                EventHandler<T> handler = this::emit;
                menuItem.addEventHandler(eventType, handler);
                return () -> menuItem.removeEventHandler(eventType, handler);
            }
        };
    }

    /**
     * Returns an event stream that emits periodic <i>ticks</i>. The first tick
     * is emitted after {@code interval} amount of time has passed.
     * The returned stream may only be used on the JavaFX application thread.
     *
     * <p>As with all lazily bound streams, ticks are emitted only when there
     * is at least one subscriber to the returned stream. This means that to
     * release associated resources, it suffices to unsubscribe from the
     * returned stream.
     */
    public static EventStream<?> ticks(Duration interval) {
        return new EventStreamBase<Void>() {
            private final Timer timer = FxTimer.createPeriodic(
                    interval, () -> emit(null));

            @Override
            protected Subscription observeInputs() {
                timer.restart();
                return timer::stop;
            }
        };
    }

    /**
     * Returns an event stream that emits periodic <i>ticks</i>. The first tick
     * is emitted at time 0.
     * The returned stream may only be used on the JavaFX application thread.
     *
     * <p>As with all lazily bound streams, ticks are emitted only when there
     * is at least one subscriber to the returned stream. This means that to
     * release associated resources, it suffices to unsubscribe from the
     * returned stream.
     */
    public static EventStream<?> ticks0(Duration interval) {
        return new EventStreamBase<Void>() {
            private final Timer timer = FxTimer.createPeriodic0(
                    interval, () -> emit(null));

            @Override
            protected Subscription observeInputs() {
                timer.restart();
                return timer::stop;
            }
        };
    }

    /**
     * Returns an event stream that emits periodic <i>ticks</i> on the given
     * {@code eventThreadExecutor}. The returned stream may only be used from
     * that executor's thread.
     *
     * <p>As with all lazily bound streams, ticks are emitted only when there
     * is at least one subscriber to the returned stream. This means that to
     * release associated resources, it suffices to unsubscribe from the
     * returned stream.
     *
     * @param scheduler scheduler used to schedule periodic emissions.
     * @param eventThreadExecutor single-thread executor used to emit the ticks.
     */
    public static EventStream<?> ticks(
            Duration interval,
            ScheduledExecutorService scheduler,
            Executor eventThreadExecutor) {
        return new EventStreamBase<Void>() {
            private final Timer timer = ScheduledExecutorServiceTimer.createPeriodic(
                    interval, () -> emit(null), scheduler, eventThreadExecutor);

            @Override
            protected Subscription observeInputs() {
                timer.restart();
                return timer::stop;
            }
        };
    }

    /**
     * Returns an event stream that emits a timestamp of the current frame in
     * nanoseconds on every frame. The timestamp has the same meaning as the
     * argument of the {@link AnimationTimer#handle(long)} method.
     */
    public static EventStream<Long> animationTicks() {
        return new EventStreamBase<Long>() {
            private final AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    emit(now);
                }
            };

            @Override
            protected Subscription observeInputs() {
                timer.start();
                return timer::stop;
            }
        };
    }

    /**
     * Returns a stream that, on each animation frame, emits the duration
     * elapsed since the previous animation frame, in nanoseconds.
     */
    public static EventStream<Long> animationFrames() {
        return animationTicks()
                .accumulate(t(0L, -1L), (state, now) -> state.map((d, last) -> {
                        return t(last == -1L ? 0L : now - last, now);
                }))
                .map(t -> t._1);
    }

    /**
     * Returns an event stream that emits all the events emitted from any of
     * the {@code inputs}. The event type of the returned stream is the nearest
     * common super-type of all the {@code inputs}.
     *
     * @see EventStream#or(EventStream)
     */
    @SafeVarargs
    public static <T> EventStream<T> merge(EventStream<? extends T>... inputs) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                return Subscription.multi(i -> i.subscribe(this::emit), inputs);
            }
        };
    }

    /**
     * Returns an event stream that emits all the events emitted from any of
     * the event streams in the given observable set. When an event stream is
     * added to the set, the returned stream will start emitting its events.
     * When an event stream is removed from the set, its events will no longer
     * be emitted from the returned stream.
     */
    public static <T> EventStream<T> merge(
            ObservableSet<? extends EventStream<T>> set) {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                return Subscription.dynamic(set, s -> s.subscribe(this::emit));
            }
        };
    }

    /**
     * A more general version of {@link #merge(ObservableSet)} for a set of
     * arbitrary element type and a function to obtain an event stream from
     * the element.
     * @param set observable set of elements
     * @param f function to obtain an event stream from an element
     */
    public static <T, U> EventStream<U> merge(
            ObservableSet<? extends T> set,
            Function<? super T, ? extends EventStream<U>> f) {
        return new EventStreamBase<U>() {
            @Override
            protected Subscription observeInputs() {
                return Subscription.dynamic(
                        set,
                        t -> f.apply(t).subscribe(this::emit));
            }
        };
    }

    public static <L, R> Tuple2<EventStream<L>, EventStream<R>> fork(
            EventStream<? extends Either<L, R>> stream) {
        return t(
                stream.filterMap(Either::asLeft),
                stream.filterMap(Either::asRight));
    }

    public static <A, B> EventStream<Tuple2<A, B>> zip(EventStream<A> srcA, EventStream<B> srcB) {
        return new EventStreamBase<Tuple2<A, B>>() {
            Pocket<A> pocketA = new ExclusivePocket<>();
            Pocket<B> pocketB = new ExclusivePocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }));
            }

            protected void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(t(pocketA.getAndClear(), pocketB.getAndClear()));
                }
            }
        };
    }

    public static <A, B, C> EventStream<Tuple3<A, B, C>> zip(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new EventStreamBase<Tuple3<A, B, C>>() {
            Pocket<A> pocketA = new ExclusivePocket<>();
            Pocket<B> pocketB = new ExclusivePocket<>();
            Pocket<C> pocketC = new ExclusivePocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                pocketC.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }),
                        srcC.subscribe(c -> { pocketC.set(c); tryEmit(); }));
            }

            protected void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                    emit(t(pocketA.getAndClear(), pocketB.getAndClear(), pocketC.getAndClear()));
                }
            }
        };
    }

    public static <A, B> EventStream<Tuple2<A, B>> combine(
            EventStream<A> srcA,
            EventStream<B> srcB) {
        return new EventStreamBase<Tuple2<A, B>>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(t(pocketA.get(), pocketB.get()));
                }
            }
        };
    }

    public static <A, B, C> EventStream<Tuple3<A, B, C>> combine(
            EventStream<A> srcA,
            EventStream<B> srcB,
            EventStream<C> srcC) {
        return new EventStreamBase<Tuple3<A, B, C>>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();
            Pocket<C> pocketC = new Pocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                pocketC.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }),
                        srcC.subscribe(c -> { pocketC.set(c); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                    emit(t(pocketA.get(), pocketB.get(), pocketC.get()));
                }
            }
        };
    }

    public static <A, B, C, D> EventStream<Tuple4<A, B, C, D>> combine(
            EventStream<A> srcA,
            EventStream<B> srcB,
            EventStream<C> srcC,
            EventStream<D> srcD) {
        return new EventStreamBase<Tuple4<A, B, C, D>>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();
            Pocket<C> pocketC = new Pocket<>();
            Pocket<D> pocketD = new Pocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                pocketC.clear();
                pocketD.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }),
                        srcC.subscribe(c -> { pocketC.set(c); tryEmit(); }),
                        srcD.subscribe(d -> { pocketD.set(d); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()
                        && pocketC.hasValue() && pocketD.hasValue()) {

                    emit(t(pocketA.get(), pocketB.get(),
                            pocketC.get(), pocketD.get()));
                }
            }
        };
    }

    public static <A, B, C, D, E> EventStream<Tuple5<A, B, C, D, E>> combine(
            EventStream<A> srcA,
            EventStream<B> srcB,
            EventStream<C> srcC,
            EventStream<D> srcD,
            EventStream<E> srcE) {
        return new EventStreamBase<Tuple5<A, B, C, D, E>>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();
            Pocket<C> pocketC = new Pocket<>();
            Pocket<D> pocketD = new Pocket<>();
            Pocket<E> pocketE = new Pocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                pocketC.clear();
                pocketD.clear();
                pocketE.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }),
                        srcC.subscribe(c -> { pocketC.set(c); tryEmit(); }),
                        srcD.subscribe(d -> { pocketD.set(d); tryEmit(); }),
                        srcE.subscribe(e -> { pocketE.set(e); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()
                        && pocketC.hasValue() && pocketD.hasValue()
                        && pocketE.hasValue()) {

                    emit(t(pocketA.get(), pocketB.get(), pocketC.get(),
                            pocketD.get(), pocketE.get()));
                }
            }
        };
    }

    public static <A, B, C, D, E, F> EventStream<Tuple6<A, B, C, D, E, F>> combine(
            EventStream<A> srcA,
            EventStream<B> srcB,
            EventStream<C> srcC,
            EventStream<D> srcD,
            EventStream<E> srcE,
            EventStream<F> srcF) {
        return new EventStreamBase<Tuple6<A, B, C, D, E, F>>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();
            Pocket<C> pocketC = new Pocket<>();
            Pocket<D> pocketD = new Pocket<>();
            Pocket<E> pocketE = new Pocket<>();
            Pocket<F> pocketF = new Pocket<>();

            @Override
            protected Subscription observeInputs() {
                pocketA.clear();
                pocketB.clear();
                pocketC.clear();
                pocketD.clear();
                pocketE.clear();
                pocketF.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }),
                        srcC.subscribe(c -> { pocketC.set(c); tryEmit(); }),
                        srcD.subscribe(d -> { pocketD.set(d); tryEmit(); }),
                        srcE.subscribe(e -> { pocketE.set(e); tryEmit(); }),
                        srcF.subscribe(f -> { pocketF.set(f); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()
                        && pocketC.hasValue() && pocketD.hasValue()
                        && pocketE.hasValue() && pocketF.hasValue()) {

                    emit(t(pocketA.get(), pocketB.get(), pocketC.get(),
                            pocketD.get(), pocketE.get(), pocketF.get()));
                }
            }
        };
    }


    private static class Pocket<T> {
        private boolean hasValue = false;
        private T value = null;

        public boolean hasValue() { return hasValue; }
        public void set(T value) {
            this.value = value;
            hasValue = true;
        }
        public T get() {
            return value;
        }
        public void clear() {
            hasValue = false;
            value = null;
        }
        public T getAndClear() {
            T res = get();
            clear();
            return res;
        }
    }

    private static class ExclusivePocket<T> extends Pocket<T> {
        @Override
        public final void set(T a) {
            if(hasValue()) {
                throw new IllegalStateException("Value arrived out of order: " + a);
            } else {
                super.set(a);
            }
        };
    }
}

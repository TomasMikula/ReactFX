package org.reactfx;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

public class EventStreams {

    /**
     * Returns an event stream that never emits any value.
     */
    public static <T> EventStream<T> never() {
        return consumer -> Subscription.EMPTY;
    }

    public static EventStream<Void> invalidationsOf(Observable observable) {
        return new LazilyBoundStream<Void>() {
            @Override
            protected Subscription subscribeToInputs() {
                InvalidationListener listener = obs -> emit(null);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    public static <T> EventStream<T> valuesOf(ObservableValue<T> observable) {
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(val);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }

            @Override
            protected void newSubscriber(Consumer<? super T> consumer) {
                consumer.accept(observable.getValue());
            }
        };
    }

    public static <T> EventStream<T> nonNullValuesOf(ObservableValue<T> observable) {
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                ChangeListener<T> listener = (obs, old, val) -> {
                    if(val != null) {
                        emit(val);
                    }
                };
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }

            @Override
            protected void newSubscriber(Consumer<? super T> consumer) {
                T val = observable.getValue();
                if(val != null) {
                    consumer.accept(val);
                }
            }
        };
    }

    public static <T> EventStream<Change<T>> changesOf(ObservableValue<T> observable) {
        return new LazilyBoundStream<Change<T>>() {
            @Override
            protected Subscription subscribeToInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(new Change<>(old, val));
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    public static <T> EventStream<ListChangeListener.Change<? extends T>> changesOf(ObservableList<T> list) {
        return new LazilyBoundStream<ListChangeListener.Change<? extends T>>() {
            @Override
            protected Subscription subscribeToInputs() {
                ListChangeListener<T> listener = c -> emit(c);
                list.addListener(listener);
                return () -> list.removeListener(listener);
            }
        };
    }

    public static <T> EventStream<SetChangeListener.Change<? extends T>> changesOf(ObservableSet<T> set) {
        return new LazilyBoundStream<SetChangeListener.Change<? extends T>>() {
            @Override
            protected Subscription subscribeToInputs() {
                SetChangeListener<T> listener = c -> emit(c);
                set.addListener(listener);
                return () -> set.removeListener(listener);
            }
        };
    }

    public static <K, V> EventStream<MapChangeListener.Change<? extends K, ? extends V>> changesOf(ObservableMap<K, V> map) {
        return new LazilyBoundStream<MapChangeListener.Change<? extends K, ? extends V>>() {
            @Override
            protected Subscription subscribeToInputs() {
                MapChangeListener<K, V> listener = c -> emit(c);
                map.addListener(listener);
                return () -> map.removeListener(listener);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(Node node, EventType<T> eventType) {
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                EventHandler<T> handler = event -> emit(event);
                node.addEventHandler(eventType, handler);
                return () -> node.removeEventHandler(eventType, handler);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(Scene scene, EventType<T> eventType) {
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                EventHandler<T> handler = event -> emit(event);
                scene.addEventHandler(eventType, handler);
                return () -> scene.removeEventHandler(eventType, handler);
            }
        };
    }

    /**
     * Returns an event stream that emits periodic <i>ticks</i>. The returned
     * stream may only be used on the JavaFX application thread.
     *
     * <p>As with all lazily bound streams, ticks are emitted only when there
     * is at least one subscriber to the returned stream. This means that to
     * release associated resources, it suffices to unsubscribe from the
     * returned stream.
     */
    public static EventStream<?> ticks(Duration interval) {
        return new LazilyBoundStream<Void>() {
            private final Timer timer = FxTimer.createPeriodic(
                    interval, () -> emit(null));

            @Override
            protected Subscription subscribeToInputs() {
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
        return new LazilyBoundStream<Void>() {
            private final Timer timer = ScheduledExecutorServiceTimer.createPeriodic(
                    interval, () -> emit(null), scheduler, eventThreadExecutor);

            @Override
            protected Subscription subscribeToInputs() {
                timer.restart();
                return timer::stop;
            }
        };
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
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                Subscription[] subs = Stream.of(inputs)
                        .map(i -> i.subscribe(this::emit))
                        .toArray(n -> new Subscription[n]);
                return Subscription.multi(subs);
            }
        };
    }

    public static <A, B> BiEventStream<A, B> zip(EventStream<A> srcA, EventStream<B> srcB) {
        return new LazilyBoundBiStream<A, B>() {
            Pocket<A> pocketA = new ExclusivePocket<>();
            Pocket<B> pocketB = new ExclusivePocket<>();

            @Override
            protected Subscription subscribeToInputs() {
                pocketA.clear();
                pocketB.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }));
            }

            protected void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(pocketA.getAndClear(), pocketB.getAndClear());
                }
            }
        };
    }

    public static <A, B, C> TriEventStream<A, B, C> zip(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new LazilyBoundTriStream<A, B, C>() {
            Pocket<A> pocketA = new ExclusivePocket<>();
            Pocket<B> pocketB = new ExclusivePocket<>();
            Pocket<C> pocketC = new ExclusivePocket<>();

            @Override
            protected Subscription subscribeToInputs() {
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
                    emit(pocketA.getAndClear(), pocketB.getAndClear(), pocketC.getAndClear());
                }
            }
        };
    }

    public static <A, B> BiEventStream<A, B> combine(EventStream<A> srcA, EventStream<B> srcB) {
        return new LazilyBoundBiStream<A, B>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();

            @Override
            protected Subscription subscribeToInputs() {
                pocketA.clear();
                pocketB.clear();
                return Subscription.multi(
                        srcA.subscribe(a -> { pocketA.set(a); tryEmit(); }),
                        srcB.subscribe(b -> { pocketB.set(b); tryEmit(); }));
            }

            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(pocketA.get(), pocketB.get());
                }
            }
        };
    }

    public static <A, B, C> TriEventStream<A, B, C> combine(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new LazilyBoundTriStream<A, B, C>() {
            Pocket<A> pocketA = new Pocket<>();
            Pocket<B> pocketB = new Pocket<>();
            Pocket<C> pocketC = new Pocket<>();

            @Override
            protected Subscription subscribeToInputs() {
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
                    emit(pocketA.get(), pocketB.get(), pocketC.get());
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

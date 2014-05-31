package org.reactfx;

import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
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

import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuple4;
import org.reactfx.util.Tuples;

public class EventStreams {

    public static interface Combinator2<A, B, R> {
        R combine(A a, B b);
    }

    public static interface Combinator3<A, B, C, R> {
        R combine(A a, B b, C c);
    }

    public static interface Combinator4<A, B, C, D, R> {
        R combine(A a, B b, C c, D d);
    }

    /**
     * Type returned from
     * {@code emit(EventStream<T>)}.
     */
    @Deprecated
    public static final class Emit<T> {
        private final EventStream<T> input;
        Emit(EventStream<T> input) {
            this.input = input;
        }
        public EventStream<T> on(EventStream<?> impulse) {
            return emitOnImpulse(input, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>)}.
     */
    public static final class Combine1<A> {
        private final EventStream<A> srcA;
        Combine1(EventStream<A> srcA) {
            this.srcA = srcA;
        }
        public <I> Combine1On<A, I> on(EventStream<I> impulse) {
            return new Combine1On<A, I>(srcA, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>).on(EventStream<I>)}.
     */
    public static final class Combine1On<A, I> {
        private final EventStream<A> srcA;
        private final EventStream<I> impulse;
        Combine1On(EventStream<A> srcA, EventStream<I> impulse) {
            this.srcA = srcA;
            this.impulse = impulse;
        }
        public <R> EventStream<R> by(Combinator2<A, I, R> combinator) {
            return combineOnImpulse(srcA, impulse, combinator);
        }
        public EventStream<Tuple2<A, I>> asTuple() {
            return combineOnImpulse(srcA, impulse, (a, b) -> Tuples.t(a, b));
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>, EventStream<B>)}.
     */
    public static final class Combine2<A, B> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        Combine2(EventStream<A> srcA, EventStream<B> srcB) {
            this.srcA = srcA;
            this.srcB = srcB;
        }
        public <R> EventStream<R> by(Combinator2<A, B, R> combinator) {
            return combineLatest(srcA, srcB, combinator);
        }
        public EventStream<Tuple2<A, B>> asTuple() {
            return combineLatest(srcA, srcB, (a, b) -> Tuples.t(a, b));
        }
        public <I> Combine2On<A, B, I> on(EventStream<I> impulse) {
            return new Combine2On<A, B, I>(srcA, srcB, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>, EventStream<B>).on(EventStream<I>)}.
     */
    public static final class Combine2On<A, B, I> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<I> impulse;
        Combine2On(EventStream<A> srcA, EventStream<B> srcB, EventStream<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.impulse = impulse;
        }
        public <R> EventStream<R> by(Combinator2<A, B, R> combinator) {
            return combineOnImpulse(srcA, srcB, impulse, combinator);
        }
        public <R> EventStream<R> by(Combinator3<A, B, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, impulse, combinator);
        }
        public EventStream<Tuple3<A, B, I>> asTuple() {
            return combineOnImpulse(srcA, srcB, impulse, (a, b, c) -> Tuples.t(a, b, c));
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>, EventStream<B>, EventStream<C>)}.
     */
    public static final class Combine3<A, B, C> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<C> srcC;
        Combine3(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
        }
        public <R> EventStream<R> by(Combinator3<A, B, C, R> combinator) {
            return combineLatest(srcA, srcB, srcC, combinator);
        }
        public EventStream<Tuple3<A, B, C>> asTuple() {
            return combineLatest(srcA, srcB, srcC, (a, b, c) -> Tuples.t(a, b, c));
        }
        public <I> Combine3On<A, B, C, I> on(EventStream<I> impulse) {
            return new Combine3On<A, B, C, I>(srcA, srcB, srcC, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(EventStream<A>, EventStream<B>, EventStream<C>).on(EventStream<I>)}.
     */
    public static final class Combine3On<A, B, C, I> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<C> srcC;
        private final EventStream<I> impulse;
        Combine3On(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, EventStream<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
            this.impulse = impulse;
        }
        public <R> EventStream<R> by(Combinator3<A, B, C, R> combinator) {
            return combineOnImpulse(srcA, srcB, srcC, impulse, combinator);
        }
        public <R> EventStream<R> by(Combinator4<A, B, C, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, srcC, impulse, combinator);
        }
        public EventStream<Tuple4<A, B, C, I>> asTuple() {
            return combineOnImpulse(srcA, srcB, srcC, impulse, (a, b, c, d) -> Tuples.t(a, b, c, d));
        }
    }


    /**
     * Type returned from
     * {@code zip(EventStream<A>, EventStream<B>)}.
     */
    public static final class Zip2<A, B> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        Zip2(EventStream<A> srcA, EventStream<B> srcB) {
            this.srcA = srcA;
            this.srcB = srcB;
        }
        public <R> EventStream<R> by(Combinator2<A, B, R> combinator) {
            return zip(srcA, srcB, combinator);
        }
        public EventStream<Tuple2<A, B>> asTuple() {
            return zip(srcA, srcB, (a, b) -> Tuples.t(a, b));
        }
    }

    /**
     * Type returned from
     * {@code zip(EventStream<A>, EventStream<B>, EventStream<C>)}.
     */
    public static final class Zip3<A, B, C> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<C> srcC;
        Zip3(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
        }
        public <R> EventStream<R> by(Combinator3<A, B, C, R> combinator) {
            return zip(srcA, srcB, srcC, combinator);
        }
        public EventStream<Tuple3<A, B, C>> asTuple() {
            return zip(srcA, srcB, srcC, (a, b, c) -> Tuples.t(a, b, c));
        }
    }


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

    @SafeVarargs
    public static <T> EventStream<T> merge(EventStream<? extends T>... inputs) {
        return new LazilyBoundStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                Subscription[] subs = new Subscription[inputs.length];
                for(int i = 0; i < inputs.length; ++i) {
                    subs[i] = inputs[i].subscribe(value -> emit(value));
                }
                return Subscription.multi(subs);
            }
        };
    }

    public static <A, B> Zip2<A, B> zip(EventStream<A> srcA, EventStream<B> srcB) {
        return new Zip2<>(srcA, srcB);
    }

    static <A, B, R> EventStream<R> zip(EventStream<A> srcA, EventStream<B> srcB, Combinator2<A, B, R> combinator) {
        return new ZippedStream<R>() {
            Pocket<A> pocketA = new Pocket<A>();
            Pocket<B> pocketB = new Pocket<B>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB));
            }

            @Override
            protected void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(combinator.combine(pocketA.extract(), pocketB.extract()));
                }
            }
        };
    }

    public static <A, B, C> Zip3<A, B, C> zip(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new Zip3<>(srcA, srcB, srcC);
    }

    static <A, B, C, R> EventStream<R> zip(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, Combinator3<A, B, C, R> combinator) {
        return new ZippedStream<R>() {
            Pocket<A> pocketA = new Pocket<A>();
            Pocket<B> pocketB = new Pocket<B>();
            Pocket<C> pocketC = new Pocket<C>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB),
                        pocketC.fillFrom(srcC));
            }

            @Override
            protected void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                    emit(combinator.combine(pocketA.extract(), pocketB.extract(), pocketC.extract()));
                }
            }
        };
    }

    static <A, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<I> impulse, Combinator2<A, I, R> combinator) {
        return new LazilyBoundStream<R>() {
            Pocket<A> pocketA = new OverwritingPocket<A>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        impulse.subscribe(i -> tryEmit(i)));
            }

            private void tryEmit(I i) {
                if(pocketA.hasValue()) {
                    emit(combinator.combine(pocketA.extract(), i));
                }
            }
        };
    }

    public static <A> Combine1<A> combine(EventStream<A> srcA) {
        return new Combine1<A>(srcA);
    }

    static <A, B, R> EventStream<R> combineLatest(EventStream<A> srcA, EventStream<B> srcB, Combinator2<A, B, R> combinator) {
        return new CombineLatestStream<R>() {
            Pocket<A> pocketA = new Pocket<A>();
            Pocket<B> pocketB = new Pocket<B>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB));
            }

            @Override
            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(combinator.combine(pocketA.peek(), pocketB.peek()));
                }
            }
        };
    }

    static <A, B, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<B> srcB, EventStream<I> impulse, Combinator3<A, B, I, R> combinator) {
        return new Combine2OnImpulseStream<A, B, I, R>(srcA, srcB, impulse) {
            @Override
            protected R combine(I impulse) {
                return combinator.combine(pocketA.peek(), pocketB.peek(), impulse);
            }
        };
    }

    static <A, B, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<B> srcB, EventStream<I> impulse, Combinator2<A, B, R> combinator) {
        return new Combine2OnImpulseStream<A, B, I, R>(srcA, srcB, impulse) {
            @Override
            protected R combine(I impulse) {
                return combinator.combine(pocketA.peek(), pocketB.peek());
            }
        };
    }

    public static <A, B> Combine2<A, B> combine(EventStream<A> srcA, EventStream<B> srcB) {
        return new Combine2<A, B>(srcA, srcB);
    }

    static <A, B, C, R> EventStream<R> combineLatest(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, Combinator3<A, B, C, R> combinator) {
        return new CombineLatestStream<R>() {
            Pocket<A> pocketA = new Pocket<A>();
            Pocket<B> pocketB = new Pocket<B>();
            Pocket<C> pocketC = new Pocket<C>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB),
                        pocketC.fillFrom(srcC));
            }

            @Override
            void tryEmit() {
                if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                    emit(combinator.combine(pocketA.peek(), pocketB.peek(), pocketC.peek()));
                }
            }
        };
    }

    public static <A, B, C, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, EventStream<I> impulse, Combinator4<A, B, C, I, R> combinator) {
        return new Combine3OnImpulseStream<A, B, C, I, R>(srcA, srcB, srcC, impulse) {
            @Override
            protected R combine(I impulse) {
                return combinator.combine(pocketA.peek(), pocketB.peek(), pocketC.peek(), impulse);
            }
        };
    }

    public static <A, B, C, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, EventStream<I> impulse, Combinator3<A, B, C, R> combinator) {
        return new Combine3OnImpulseStream<A, B, C, I, R>(srcA, srcB, srcC, impulse) {
            @Override
            protected R combine(I impulse) {
                return combinator.combine(pocketA.peek(), pocketB.peek(), pocketC.peek());
            }
        };
    }

    public static <A, B, C> Combine3<A, B, C> combine(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new Combine3<A, B, C>(srcA, srcB, srcC);
    }

    /**
     * @deprecated Use {@code input.emitOn(impulse)} instead.
     */
    @Deprecated
    public static <T> Emit<T> emit(EventStream<T> input) {
        return new Emit<T>(input);
    }

    static <T> EventStream<T> emitOnImpulse(EventStream<T> input, EventStream<?> impulse) {
        return new LazilyBoundStream<T>() {
            private boolean hasValue = false;
            private T value = null;

            @Override
            protected Subscription subscribeToInputs() {
                Subscription s1 = input.subscribe(v -> {
                    hasValue = true;
                    value = v;
                });

                Subscription s2 = impulse.subscribe(i -> {
                    if(hasValue) {
                        T val = value;
                        hasValue = false;
                        value = null;
                        emit(val);
                    }
                });

                return s1.and(s2);
            }
        };
    }

    static <T> EventStream<T> repeatOnImpulse(EventStream<T> input, EventStream<?> impulse) {
        return new LazilyBoundStream<T>() {
            private boolean hasValue = false;
            private T value = null;

            @Override
            protected Subscription subscribeToInputs() {
                Subscription s1 = input.subscribe(v -> {
                    hasValue = true;
                    value = v;
                    emit(v);
                });

                Subscription s2 = impulse.subscribe(i -> {
                    if(hasValue) {
                        emit(value);
                    }
                });

                return s1.and(s2);
            }
        };
    }

    public static <T> InterceptableEventStream<T> interceptable(EventStream<T> input) {
        if(input instanceof InterceptableEventStream) {
            return (InterceptableEventStream<T>) input;
        } else {
            return new InterceptableEventStreamImpl<>(input);
        }
    }

    @Deprecated
    static <T> StreamBoundValue<T> toObservableValue(EventStream<T> input, T initialValue) {
        return new StreamBoundValueImpl<T>(input, initialValue);
    }


    private static abstract class ZippedStream<T> extends LazilyBoundStream<T> {

        class Pocket<A> extends ExclusivePocket<A> {
            @Override
            protected void valueUpdated(A value) {
                tryEmit();
            }
        }

        abstract void tryEmit();
    }

    private static abstract class CombineLatestStream<T> extends LazilyBoundStream<T> {

        class Pocket<A> extends OverwritingPocket<A> {
            @Override
            protected void valueUpdated(A value) {
                tryEmit();
            }
        }

        abstract void tryEmit();
    }

    private static abstract class Combine2OnImpulseStream<A, B, I, R> extends LazilyBoundStream<R> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<I> impulse;
        protected final Pocket<A> pocketA = new OverwritingPocket<A>();
        protected final Pocket<B> pocketB = new OverwritingPocket<B>();

        public Combine2OnImpulseStream(EventStream<A> srcA, EventStream<B> srcB, EventStream<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.impulse = impulse;
        }

        @Override
        protected Subscription subscribeToInputs() {
            return Subscription.multi(
                    pocketA.fillFrom(srcA),
                    pocketB.fillFrom(srcB),
                    impulse.subscribe(i -> tryEmit(i)));
        }

        private void tryEmit(I impulse) {
            if(pocketA.hasValue() && pocketB.hasValue()) {
                emit(combine(impulse));
            }
        }

        protected abstract R combine(I impulse);
    }

    private static abstract class Combine3OnImpulseStream<A, B, C, I, R> extends LazilyBoundStream<R> {
        private final EventStream<A> srcA;
        private final EventStream<B> srcB;
        private final EventStream<C> srcC;
        private final EventStream<I> impulse;
        protected final Pocket<A> pocketA = new OverwritingPocket<A>();
        protected final Pocket<B> pocketB = new OverwritingPocket<B>();
        protected final Pocket<C> pocketC = new OverwritingPocket<C>();

        public Combine3OnImpulseStream(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, EventStream<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
            this.impulse = impulse;
        }

        @Override
        protected Subscription subscribeToInputs() {
            return Subscription.multi(
                    pocketA.fillFrom(srcA),
                    pocketB.fillFrom(srcB),
                    pocketC.fillFrom(srcC),
                    impulse.subscribe(i -> tryEmit(i)));
        }

        private void tryEmit(I impulse) {
            if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                emit(combine(impulse));
            }
        }

        protected abstract R combine(I impulse);
    }

    private static abstract class Pocket<T> implements Sink<T> {
        private boolean hasValue = false;
        private T value = null;

        public boolean hasValue() { return hasValue; }
        protected void setValue(T value) {
            this.value = value;
            hasValue = true;
            valueUpdated(value);
        }
        public T peek() {
            return value;
        }
        public T extract() {
            T res = value;
            hasValue = false;
            value = null;
            return res;
        }

        public Subscription fillFrom(EventStream<T> src) {
            hasValue = false;
            value = null;
            return src.subscribe(a -> push(a));
        }

        protected void valueUpdated(T value) {};
    }

    private static class ExclusivePocket<T> extends Pocket<T> {
        @Override
        public final void push(T a) {
            if(hasValue()) {
                throw new IllegalStateException("Value arrived out of order: " + a);
            } else {
                setValue(a);
            }
        };
    }

    private static class OverwritingPocket<T> extends Pocket<T> {
        @Override
        public final void push(T a) {
            setValue(a);
        };
    }

    @Deprecated
    private static class StreamBoundValueImpl<T> extends ObservableValueBase<T> implements StreamBoundValue<T> {
        private T value;
        private final Subscription subscription;

        public StreamBoundValueImpl(EventStream<T> input, T initialValue) {
            value = initialValue;
            subscription = input.subscribe(evt -> {
                value = evt;
                fireValueChangedEvent();
            });
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public void unsubscribe() {
            subscription.unsubscribe();
        }

    }
}

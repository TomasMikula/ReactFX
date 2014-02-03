package reactfx;

import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;


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
     * {@code release(EventStream<T>)}.
     */
    public static final class Release<T> {
        private final EventStream<T> input;
        Release(EventStream<T> input) {
            this.input = input;
        }
        public EventStream<T> on(EventStream<?> impulse) {
            return releaseOnImpulse(input, impulse);
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
        public <R> EventStream<R> by(Combinator3<A, B, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, impulse, combinator);
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
        public <R> EventStream<R> by(Combinator4<A, B, C, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, srcC, impulse, combinator);
        }
    }

    public static EventStream<Void> invalidationsOf(Observable observable) {
        return new CombinedStream<Void>() {
            private final InvalidationListener listener = obs -> emit(null);
            private final Subscription subscription = () -> observable.removeListener(listener);

            @Override
            protected Subscription subscribeToInputs() {
                observable.addListener(listener);
                return subscription;
            }

        };
    }

    public static <T> EventStream<T> valuesOf(ObservableValue<T> observable) {
        return new CombinedStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(val);
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    public static <T> EventStream<Change<T>> changesOf(ObservableValue<T> observable) {
        return new CombinedStream<Change<T>>() {
            @Override
            protected Subscription subscribeToInputs() {
                ChangeListener<T> listener = (obs, old, val) -> emit(new Change<>(old, val));
                observable.addListener(listener);
                return () -> observable.removeListener(listener);
            }
        };
    }

    public static <T extends Event> EventStream<T> eventsOf(Node node, EventType<T> eventType) {
        return new CombinedStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                EventHandler<T> handler = event -> emit(event);
                node.addEventHandler(eventType, handler);
                return () -> node.removeEventHandler(eventType, handler);
            }
        };
    }

    public static <T> EventStream<T> filter(EventStream<? extends T> input, Predicate<T> predicate) {
        return new CombinedStream<T>() {
            @Override
            protected Subscription subscribeToInputs() {
                return input.subscribe(value -> {
                    if(predicate.test(value)) {
                        emit(value);
                    }
                });
            }
        };
    }

    public static <T, U> EventStream<U> map(EventStream<T> input, Function<T, U> f) {
        return new CombinedStream<U>() {
            @Override
            protected Subscription subscribeToInputs() {
                return input.subscribe(value -> {
                    emit(f.apply(value));
                });
            }
        };
    }

    @SafeVarargs
    public static <T> EventStream<T> merge(EventStream<? extends T>... inputs) {
        return new CombinedStream<T>() {
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

    public static <A, B, R> EventStream<R> zip(EventStream<A> srcA, EventStream<B> srcB, Combinator2<A, B, R> combinator) {
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

    public static <A, B, C, R> EventStream<R> zip(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, Combinator3<A, B, C, R> combinator) {
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

    public static <A, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<I> impulse, Combinator2<A, I, R> combinator) {
        return new CombinedStream<R>() {
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

    public static <A, B, R> EventStream<R> combineLatest(EventStream<A> srcA, EventStream<B> srcB, Combinator2<A, B, R> combinator) {
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

    public static <A, B, I, R> EventStream<R> combineOnImpulse(EventStream<A> srcA, EventStream<B> srcB, EventStream<I> impulse, Combinator3<A, B, I, R> combinator) {
        return new CombinedStream<R>() {
            Pocket<A> pocketA = new OverwritingPocket<A>();
            Pocket<B> pocketB = new OverwritingPocket<B>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB),
                        impulse.subscribe(i -> tryEmit(i)));
            }

            private void tryEmit(I i) {
                if(pocketA.hasValue() && pocketB.hasValue()) {
                    emit(combinator.combine(pocketA.peek(), pocketB.peek(), i));
                }
            }
        };
    }

    public static <A, B> Combine2<A, B> combine(EventStream<A> srcA, EventStream<B> srcB) {
        return new Combine2<A, B>(srcA, srcB);
    }

    public static <A, B, C, R> EventStream<R> combineLatest(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC, Combinator3<A, B, C, R> combinator) {
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
        return new CombinedStream<R>() {
            Pocket<A> pocketA = new OverwritingPocket<A>();
            Pocket<B> pocketB = new OverwritingPocket<B>();
            Pocket<C> pocketC = new OverwritingPocket<C>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        pocketA.fillFrom(srcA),
                        pocketB.fillFrom(srcB),
                        pocketC.fillFrom(srcC),
                        impulse.subscribe(i -> tryEmit(i)));
            }

            private void tryEmit(I i) {
                if(pocketA.hasValue() && pocketB.hasValue() && pocketC.hasValue()) {
                    emit(combinator.combine(pocketA.peek(), pocketB.peek(), pocketC.peek(), i));
                }
            }
        };
    }

    public static <A, B, C> Combine3<A, B, C> combine(EventStream<A> srcA, EventStream<B> srcB, EventStream<C> srcC) {
        return new Combine3<A, B, C>(srcA, srcB, srcC);
    }

    public static <T> Release<T> release(EventStream<T> input) {
        return new Release<T>(input);
    }

    public static <T> EventStream<T> releaseOnImpulse(EventStream<T> input, EventStream<?> impulse) {
        return new CombinedStream<T>() {
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
                        emit(value);
                        hasValue = false;
                        value = null;
                    }
                });

                return Subscription.multi(s1, s2);
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

    public static <T> StreamBoundValue<T> toObservableValue(EventStream<T> input, T initialValue) {
        return new StreamBoundValueImpl<T>(input, initialValue);
    }


    static abstract class CombinedStream<T> extends EventStreamBase<T> {
        private Subscription subscription = null;

        protected abstract Subscription subscribeToInputs();

        @Override
        protected final void firstSubscriber() {
            subscription = subscribeToInputs();
        }

        @Override
        protected final void noSubscribers() {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private static abstract class ZippedStream<T> extends CombinedStream<T> {

        class Pocket<A> extends ExclusivePocket<A> {
            @Override
            protected void valueUpdated(A value) {
                tryEmit();
            }
        }

        abstract void tryEmit();
    }

    private static abstract class CombineLatestStream<T> extends CombinedStream<T> {

        class Pocket<A> extends OverwritingPocket<A> {
            @Override
            protected void valueUpdated(A value) {
                tryEmit();
            }
        }

        abstract void tryEmit();
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

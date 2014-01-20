package reactfx;

import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class Sources {

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
     * {@code release(Source<T>)}.
     */
    public static final class Release<T> {
        private final Source<T> input;
        Release(Source<T> input) {
            this.input = input;
        }
        public Source<T> on(Source<?> impulse) {
            return releaseOnImpulse(input, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>)}.
     */
    public static final class Combine1<A> {
        private final Source<A> srcA;
        Combine1(Source<A> srcA) {
            this.srcA = srcA;
        }
        public <I> Combine1On<A, I> on(Source<I> impulse) {
            return new Combine1On<A, I>(srcA, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>).on(Source<I>)}.
     */
    public static final class Combine1On<A, I> {
        private final Source<A> srcA;
        private final Source<I> impulse;
        Combine1On(Source<A> srcA, Source<I> impulse) {
            this.srcA = srcA;
            this.impulse = impulse;
        }
        public <R> Source<R> by(Combinator2<A, I, R> combinator) {
            return combineOnImpulse(srcA, impulse, combinator);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>, Source<B>)}.
     */
    public static final class Combine2<A, B> {
        private final Source<A> srcA;
        private final Source<B> srcB;
        Combine2(Source<A> srcA, Source<B> srcB) {
            this.srcA = srcA;
            this.srcB = srcB;
        }
        public <R> Source<R> by(Combinator2<A, B, R> combinator) {
            return combineLatest(srcA, srcB, combinator);
        }
        public <I> Combine2On<A, B, I> on(Source<I> impulse) {
            return new Combine2On<A, B, I>(srcA, srcB, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>, Source<B>).on(Source<I>)}.
     */
    public static final class Combine2On<A, B, I> {
        private final Source<A> srcA;
        private final Source<B> srcB;
        private final Source<I> impulse;
        Combine2On(Source<A> srcA, Source<B> srcB, Source<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.impulse = impulse;
        }
        public <R> Source<R> by(Combinator3<A, B, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, impulse, combinator);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>, Source<B>, Source<C>)}.
     */
    public static final class Combine3<A, B, C> {
        private final Source<A> srcA;
        private final Source<B> srcB;
        private final Source<C> srcC;
        Combine3(Source<A> srcA, Source<B> srcB, Source<C> srcC) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
        }
        public <R> Source<R> by(Combinator3<A, B, C, R> combinator) {
            return combineLatest(srcA, srcB, srcC, combinator);
        }
        public <I> Combine3On<A, B, C, I> on(Source<I> impulse) {
            return new Combine3On<A, B, C, I>(srcA, srcB, srcC, impulse);
        }
    }

    /**
     * Type returned from
     * {@code combine(Source<A>, Source<B>, Source<C>).on(Source<I>)}.
     */
    public static final class Combine3On<A, B, C, I> {
        private final Source<A> srcA;
        private final Source<B> srcB;
        private final Source<C> srcC;
        private final Source<I> impulse;
        Combine3On(Source<A> srcA, Source<B> srcB, Source<C> srcC, Source<I> impulse) {
            this.srcA = srcA;
            this.srcB = srcB;
            this.srcC = srcC;
            this.impulse = impulse;
        }
        public <R> Source<R> by(Combinator4<A, B, C, I, R> combinator) {
            return combineOnImpulse(srcA, srcB, srcC, impulse, combinator);
        }
    }

    public static Source<Void> fromInvalidations(Observable observable) {
        return new CombinedSource<Void>() {
            private final InvalidationListener listener = obs -> emit(null);
            private final Subscription subscription = () -> observable.removeListener(listener);

            @Override
            protected Subscription subscribeToInputs() {
                observable.addListener(listener);
                return subscription;
            }

        };
    }

    public static <T> Source<T> fromChanges(ObservableValue<T> observable) {
        return new CombinedSource<T>() {
            private final ChangeListener<T> listener = (obs, old, val) -> emit(val);
            private final Subscription subscription = () -> observable.removeListener(listener);

            @Override
            protected Subscription subscribeToInputs() {
                observable.addListener(listener);
                return subscription;
            }
        };
    }

    public static <T> Source<T> filter(Source<? extends T> input, Predicate<T> predicate) {
        return new CombinedSource<T>() {
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

    public static <T, U> Source<U> map(Source<T> input, Function<T, U> f) {
        return new CombinedSource<U>() {
            @Override
            protected Subscription subscribeToInputs() {
                return input.subscribe(value -> {
                    emit(f.apply(value));
                });
            }
        };
    }

    @SafeVarargs
    public static <T> Source<T> merge(Source<? extends T>... inputs) {
        return new CombinedSource<T>() {
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

    public static <A, B, R> Source<R> zip(Source<A> srcA, Source<B> srcB, Combinator2<A, B, R> combinator) {
        return new ZippedSource<R>() {
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

    public static <A, B, C, R> Source<R> zip(Source<A> srcA, Source<B> srcB, Source<C> srcC, Combinator3<A, B, C, R> combinator) {
        return new ZippedSource<R>() {
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

    public static <A, I, R> Source<R> combineOnImpulse(Source<A> srcA, Source<I> impulse, Combinator2<A, I, R> combinator) {
        return new CombinedSource<R>() {
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

    public static <A> Combine1<A> combine(Source<A> srcA) {
        return new Combine1<A>(srcA);
    }

    public static <A, B, R> Source<R> combineLatest(Source<A> srcA, Source<B> srcB, Combinator2<A, B, R> combinator) {
        return new CombineLatestSource<R>() {
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

    public static <A, B, I, R> Source<R> combineOnImpulse(Source<A> srcA, Source<B> srcB, Source<I> impulse, Combinator3<A, B, I, R> combinator) {
        return new CombinedSource<R>() {
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

    public static <A, B> Combine2<A, B> combine(Source<A> srcA, Source<B> srcB) {
        return new Combine2<A, B>(srcA, srcB);
    }

    public static <A, B, C, R> Source<R> combineLatest(Source<A> srcA, Source<B> srcB, Source<C> srcC, Combinator3<A, B, C, R> combinator) {
        return new CombineLatestSource<R>() {
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

    public static <A, B, C, I, R> Source<R> combineOnImpulse(Source<A> srcA, Source<B> srcB, Source<C> srcC, Source<I> impulse, Combinator4<A, B, C, I, R> combinator) {
        return new CombinedSource<R>() {
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

    public static <A, B, C> Combine3<A, B, C> combine(Source<A> srcA, Source<B> srcB, Source<C> srcC) {
        return new Combine3<A, B, C>(srcA, srcB, srcC);
    }

    public static <T> Release<T> release(Source<T> input) {
        return new Release<T>(input);
    }

    public static <T> Source<T> releaseOnImpulse(Source<T> input, Source<?> impulse) {
        return new CombinedSource<T>() {
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


    private static abstract class CombinedSource<T> extends SourceBase<T> {
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

    private static abstract class ZippedSource<T> extends CombinedSource<T> {

        class Pocket<A> extends ExclusivePocket<A> {
            @Override
            protected void valueUpdated(A value) {
                tryEmit();
            }
        }

        abstract void tryEmit();
    }

    private static abstract class CombineLatestSource<T> extends CombinedSource<T> {

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

        public Subscription fillFrom(Source<T> src) {
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
}

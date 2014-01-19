package reactfx;

import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class Sources {

    public static interface OnBuilder<T, R> {
        R on(T t);
    }

    public static interface Combinator2<A, B, R> {
        R combine(A a, B b);
    }

    public static interface Combinator3<A, B, C, R> {
        R combine(A a, B b, C c);
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
            SourceObserver<A> obsA = new SourceObserver<A>();
            SourceObserver<B> obsB = new SourceObserver<B>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        obsA.subscribeTo(srcA),
                        obsB.subscribeTo(srcB));
            }

            @Override
            protected void tryEmit() {
                if(obsA.hasValue() && obsB.hasValue()) {
                    emit(combinator.combine(obsA.getValue(), obsB.getValue()));
                    obsA.clearValue();
                    obsB.clearValue();
                }
            }
        };
    }

    public static <A, B, C, R> Source<R> zip(Source<A> srcA, Source<B> srcB, Source<C> srcC, Combinator3<A, B, C, R> combinator) {
        return new ZippedSource<R>() {
            SourceObserver<A> obsA = new SourceObserver<A>();
            SourceObserver<B> obsB = new SourceObserver<B>();
            SourceObserver<C> obsC = new SourceObserver<C>();

            @Override
            protected Subscription subscribeToInputs() {
                return Subscription.multi(
                        obsA.subscribeTo(srcA),
                        obsB.subscribeTo(srcB),
                        obsC.subscribeTo(srcC));
            }

            @Override
            protected void tryEmit() {
                if(obsA.hasValue() && obsB.hasValue() && obsC.hasValue()) {
                    emit(combinator.combine(obsA.getValue(), obsB.getValue(), obsC.getValue()));
                    obsA.clearValue();
                    obsB.clearValue();
                    obsC.clearValue();
                }
            }
        };
    }

    public static <T> OnBuilder<Source<?>, Source<T>> release(Source<T> input) {
        return impulse -> releaseOnImpulse(impulse, input);
    }

    public static <T> Source<T> releaseOnImpulse(Source<?> impulse, Source<T> input) {
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

        class SourceObserver<A> {
            private boolean hasValue = false;
            private A value = null;

            public boolean hasValue() { return hasValue; }
            public A getValue() { return value; }
            public void clearValue() { hasValue = false; value = null; }

            public Subscription subscribeTo(Source<A> src) {
                clearValue();
                return src.subscribe(a -> {
                    if(hasValue) {
                        throw new IllegalStateException("Value arrived out of order: " + a);
                    } else {
                        hasValue = true;
                        value = a;
                        tryEmit();
                    }
                });
            }
        }

        abstract void tryEmit();
    }
}

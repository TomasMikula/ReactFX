package reactfx;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class Sources {

    public static interface OnBuilder<T, R> {
        R on(T t);
    }

    public static Source<Void> fromInvalidations(Observable observable) {
        return new CombinedSourceBase<Void>() {
            private final InvalidationListener listener = obs -> emitValue(null);
            private final Subscription subscription = () -> observable.removeListener(listener);

            @Override
            protected Subscription subscribeToInputs() {
                observable.addListener(listener);
                return subscription;
            }

        };
    }

    public static <T> Source<T> fromChanges(ObservableValue<T> observable) {
        return new CombinedSourceBase<T>() {
            private final ChangeListener<T> listener = (obs, old, val) -> emitValue(val);
            private final Subscription subscription = () -> observable.removeListener(listener);

            @Override
            protected Subscription subscribeToInputs() {
                observable.addListener(listener);
                return subscription;
            }
        };
    }

    @SafeVarargs
    public static <U> Source<U> merge(Source<? extends U>... inputs) {
        return new CombinedSourceBase<U>() {
            @Override
            protected Subscription subscribeToInputs() {
                Subscription[] subs = new Subscription[inputs.length];
                for(int i = 0; i < inputs.length; ++i) {
                    subs[i] = inputs[i].subscribe(value -> emitValue(value));
                }
                return Subscription.multi(subs);
            }
        };
    }

    public static <T> OnBuilder<Source<?>, Source<T>> release(Source<T> input) {
        return impulse -> releaseOnImpulse(impulse, input);
    }

    public static <T> Source<T> releaseOnImpulse(Source<?> impulse, Source<T> input) {
        return new CombinedSourceBase<T>() {
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
                        emitValue(value);
                        hasValue = false;
                        value = null;
                    }
                });

                return Subscription.multi(s1, s2);
            }
        };
    }


    private static abstract class CombinedSourceBase<T> extends SourceBase<T> {
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
}

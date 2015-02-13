package org.reactfx.value;

import java.time.Duration;
import java.util.function.BiFunction;

import javafx.animation.Transition;
import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;
import org.reactfx.util.Interpolator;

class AnimatedVal<T> extends ValBase<T> {
    private final class FractionTransition extends Transition {

        @Override
        protected void interpolate(double frac) {
            fraction = frac;
            invalidate();
        }

        void setDuration(Duration d) {
            setCycleDuration(javafx.util.Duration.millis(d.toMillis()));
        }
    }

    private final ObservableValue<T> src;
    private final BiFunction<? super T, ? super T, Duration> duration;
    private final Interpolator<T> interpolator;
    private final FractionTransition transition = new FractionTransition();

    private double fraction = 1.0;
    private T oldValue = null;

    AnimatedVal(
            ObservableValue<T> src,
            BiFunction<? super T, ? super T, Duration> duration,
            Interpolator<T> interpolator) {
        this.src = src;
        this.duration = duration;
        this.interpolator = interpolator;
    }

    @Override
    protected Subscription connect() {
        oldValue = src.getValue();
        return Val.observeChanges(src, (obs, oldVal, newVal) -> {
            oldValue = getValue();
            Duration d = duration.apply(oldValue, newVal);
            transition.setDuration(d);
            transition.playFromStart();
        });
    }

    @Override
    protected T computeValue() {
        return fraction == 1.0
                ? src.getValue()
                : interpolator.interpolate(oldValue, src.getValue(), fraction);
    }

}

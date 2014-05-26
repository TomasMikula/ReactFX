package org.reactfx;

import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;


public class Indicator implements ObservableBooleanValue, Guardian {

    private ListHelper<InvalidationListener> iListeners;
    private ListHelper<ChangeListener<? super Boolean>> cListeners;

    private int on = 0;

    /**
     * Turns this indicator on.
     * @return a Guard that, when closed, resets this indicator to the
     * original state.
     */
    public Guard on() {
        if(++on == 1) {
            notifyListeners(true);
        }

        return ((Guard) this::release).closeableOnce();
    }

    private void release() {
        assert on > 0;
        if(--on == 0) {
            notifyListeners(false);
        }
    }

    /**
     * Equivalent to {@link #on()}.
     */
    @Override
    public Guard guard() {
        return on();
    }

    /**
     * Runs the given computation, making sure this indicator is on.
     * When done, this indicator is reset to the previous state.
     *
     * <p>Equivalent to
     * <pre>
     * try(Guard g = on()) {
     *     r.run();
     * }
     * </pre>
     */
    public void onWhile(Runnable r) {
        try(Guard g = on()) {
            r.run();
        }
    }

    /**
     * Runs the given computation, making sure this indicator is on.
     * When done, this indicator is reset to the previous state.
     *
     * <pre>
     * T t = indicator.onWhile(f);
     * </pre>
     *
     * is equivalent to
     *
     * <pre>
     * T t;
     * try(Guard g = on()) {
     *     t = f.get();
     * }
     * </pre>
     */
    public <T> T onWhile(Supplier<T> f) {
        try(Guard g = on()) {
            return f.get();
        }
    }

    public boolean isOn() {
        return on > 0;
    }

    public boolean isOff() {
        return on == 0;
    }

    @Override
    public boolean get() {
        return on > 0;
    }

    @Override
    public Boolean getValue() {
        return on > 0;
    }

    public EventStream<Void> ons() {
        return EventStreams.valuesOf(this).filterMap(on -> on, on -> null);
    }

    public EventStream<Void> offs() {
        return EventStreams.valuesOf(this).filterMap(on -> !on, on -> null);
    }

    private void notifyListeners(boolean value) {
        ListHelper.forEach(iListeners, l -> l.invalidated(this));
        ListHelper.forEach(cListeners, l -> l.changed(this, !value, value));
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        cListeners = ListHelper.add(cListeners, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        cListeners = ListHelper.remove(cListeners, listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        iListeners = ListHelper.add(iListeners, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        iListeners = ListHelper.remove(iListeners, listener);
    }
}

package org.reactfx;

import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;


public class Indicator implements ObservableBooleanValue {

    private ListHelper<InvalidationListener> iListeners;
    private ListHelper<ChangeListener<? super Boolean>> cListeners;

    private boolean on = false;

    public Hold on() {
        if(on) {
            return Hold.EMPTY_HOLD;
        } else {
            set(true);
            return this::release;
        }
    }

    /**
     * Runs the given computation, making sure this indicator is on.
     * When done, this indicator is reset to the previous state.
     *
     * <p>Equivalent to
     * <pre>
     * try(Hold h = on()) {
     *     r.run();
     * }
     * </pre>
     */
    public void onWhile(Runnable r) {
        try(Hold h = on()) {
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
     * try(Hold h = on()) {
     *     t = f.get();
     * }
     * </pre>
     */
    public <T> T onWhile(Supplier<T> f) {
        try(Hold h = on()) {
            return f.get();
        }
    }

    public boolean isOn() {
        return on;
    }

    public boolean isOff() {
        return !on;
    }

    @Override
    public boolean get() {
        return on;
    }

    @Override
    public Boolean getValue() {
        return on;
    }

    public EventStream<Void> ons() {
        return EventStreams.valuesOf(this).filterMap(on -> on, on -> null);
    }

    public EventStream<Void> offs() {
        return EventStreams.valuesOf(this).filterMap(on -> !on, on -> null);
    }

    private void release() {
        assert on;
        set(false);
    }

    private void set(boolean value) {
        on = value;
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

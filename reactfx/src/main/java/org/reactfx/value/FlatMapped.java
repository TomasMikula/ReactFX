package org.reactfx.value;

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;

abstract class FlatMapped<T, U, O extends ObservableValue<U>>
extends ValBase<U> {
    final Val<O> src;

    private Subscription selectedSubscription = null; // irrelevant when not connected

    public FlatMapped(ObservableValue<T> src, Function<? super T, O> f) {
        this.src = Val.map(src, f);
    }

    @Override
    protected final Subscription connect() {
        return Val.observeInvalidations(src, obs -> srcInvalidated())
                .and(this::stopObservingSelected);
    }

    @Override
    protected final U computeValue() {
        if(isObservingInputs()) {
            startObservingSelected();
        }
        return src.getOpt().map(O::getValue).orElse(null);
    }

    private void startObservingSelected() {
        assert isObservingInputs();
        if(selectedSubscription == null) {
            src.ifPresent(sel -> {
                selectedSubscription = Val.observeInvalidations(
                        sel, obs -> selectedInvalidated());
            });
        }
    }

    private void stopObservingSelected() {
        if(selectedSubscription != null) {
            selectedSubscription.unsubscribe();
            selectedSubscription = null;
        }
    }

    private void selectedInvalidated() {
        invalidate();
    }

    private void srcInvalidated() {
        stopObservingSelected();
        invalidate();
    }
}

class FlatMappedVal<T, U, O extends ObservableValue<U>>
extends FlatMapped<T, U, O> {

    public FlatMappedVal(ObservableValue<T> src, Function<? super T, O> f) {
        super(src, f);
    }
}

class FlatMappedVar<T, U, O extends Property<U>>
extends FlatMapped<T, U, O>
implements Var<U> {
    private final ChangeListener<O> srcListenerWhenBound;

    private ObservableValue<? extends U> boundTo = null;

    public FlatMappedVar(
            ObservableValue<T> src,
            Function<? super T, O> f,
            U resetToOnUnbind) {
        this(src, f, oldProperty -> oldProperty.setValue(resetToOnUnbind));
    }

    public FlatMappedVar(ObservableValue<T> src, Function<? super T, O> f) {
        this(src, f, oldProperty -> {});
    }

    private FlatMappedVar(
            ObservableValue<T> src,
            Function<? super T, O> f,
            Consumer<O> onUnbind) {
        super(src, f);
        srcListenerWhenBound = (obs, oldProperty, newProperty) -> {
            assert boundTo != null;
            if(oldProperty != null) {
                oldProperty.unbind();
                onUnbind.accept(oldProperty);
            }
            if(newProperty != null) {
                newProperty.bind(boundTo);
            }
        };
    }

    @Override
    public void setValue(U value) {
        src.ifPresent(sel -> {
            sel.setValue(value);
            invalidate();
        });
    }

    @Override
    public void bind(ObservableValue<? extends U> other) {
        if(other == null) {
            throw new IllegalArgumentException("Cannot bind to null");
        }

        if(boundTo == null) {
            src.addListener(srcListenerWhenBound);
        }
        src.ifPresent(sel -> sel.bind(other));
        boundTo = other;
    }

    @Override
    public void unbind() {
        if(boundTo != null) {
            src.removeListener(srcListenerWhenBound);
            src.ifPresent(Property::unbind);
            boundTo = null;
        }
    }

    @Override
    public boolean isBound() {
        return boundTo != null ||
                (src.isPresent() && src.getOrThrow().isBound());
    }
}
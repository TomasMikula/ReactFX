package org.reactfx.inhibeans.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.reactfx.Hold;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * Inhibitory version of {@link javafx.beans.binding.IntegerBinding}.
 */
public abstract class IntegerBinding
extends javafx.beans.binding.IntegerBinding
implements Binding<Number> {

    public static IntegerBinding wrap(ObservableValue<? extends Number> source) {
        return new IntegerBinding() {
            { bind(source); }

            @Override
            protected int computeValue() { return source.getValue().intValue(); }
        };
    }

    private ExpressionHelper<Number> helper = null;
    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public Hold block() {
        if(blocked) {
            return Hold.EMPTY_HOLD;
        } else {
            blocked = true;
            return this::release;
        }
    }

    private void release() {
        blocked = false;
        if(fireOnRelease) {
            fireOnRelease = false;
            ExpressionHelper.fireValueChangedEvent(helper);
        }
    }

    @Override
    protected final void onInvalidating() {
        if(blocked)
            fireOnRelease = true;
        else
            ExpressionHelper.fireValueChangedEvent(helper);
    }


    /*******************************************
     *** Override adding/removing listeners. ***
     *******************************************/

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super Number> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }
}

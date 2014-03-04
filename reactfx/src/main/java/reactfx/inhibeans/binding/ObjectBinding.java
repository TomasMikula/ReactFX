package reactfx.inhibeans.binding;

import reactfx.Hold;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * Inhibitory version of {@link javafx.beans.binding.ObjectBinding}.
 */
public abstract class ObjectBinding<T>
extends javafx.beans.binding.ObjectBinding<T>
implements Binding<T> {

    public static <T> ObjectBinding<T> wrap(ObservableObjectValue<T> source) {
        return new ObjectBinding<T>() {
            { bind(source); }

            @Override
            protected T computeValue() { return source.get(); }
        };
    }

    private ExpressionHelper<T> helper = null;
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
    public void addListener(ChangeListener<? super T> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }
}

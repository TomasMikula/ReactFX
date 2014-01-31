package inhibeans.binding;

import reactfx.Hold;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * Inhibitory version of {@link javafx.beans.binding.StringBinding}.
 */
public abstract class StringBinding
extends javafx.beans.binding.StringBinding
implements Binding<String> {

    public static StringBinding wrap(ObservableStringValue source) {
        return new StringBinding() {
            { bind(source); }

            @Override
            protected String computeValue() { return source.get(); }
        };
    }

    private ExpressionHelper<String> helper = null;
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
    public void addListener(ChangeListener<? super String> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super String> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

}

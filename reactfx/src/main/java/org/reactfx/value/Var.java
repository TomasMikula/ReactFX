package org.reactfx.value;

import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

public interface Var<T> extends Val<T>, Property<T> {
    static <T> Var<T> newSimpleVar(T initialValue) {
        return new SimpleVar<>(initialValue);
    }

    /**
     * Creates a {@linkplain Var} from {@linkplain ObservableValue}, using the
     * given {@code setValue} function in place of the {@link #setValue(Object)}
     * method.
     * @param obs {@linkplain ObservableValue} whose value can be changed by
     * the {@code setValue} function.
     * @param setValue function used to set the value of {@code obs}. When
     * invoked with a value {@code x}, it should perform an action that may
     * or may not result in a change of {@code obs}'s value to {@code x}.
     */
    static <T> Var<T> fromVal(ObservableValue<T> obs, Consumer<T> setValue) {
        if(obs instanceof Val) {
            return ((Val<T>) obs).asVar(setValue);
        } else {
            return Val.wrap(obs).asVar(setValue);
        }
    }

    static <T> SuspendableVar<T> suspendable(Property<T> p) {
        if(p instanceof SuspendableVar) {
            return (SuspendableVar<T>) p;
        } else {
            Var<T> var = p instanceof Var
                    ? (Var<T>) p
                    : new VarWrapper<>(p);
            return new SuspendableVarWrapper<>(var);
        }
    }

    @Override
    default void bindBidirectional(Property<T> other) {
        Bindings.bindBidirectional(this, other);
    }

    @Override
    default void unbindBidirectional(Property<T> other) {
        Bindings.unbindBidirectional(this, other);
    }

    @Override
    default Object getBean() {
        return null;
    }

    @Override
    default String getName() {
        return null;
    }

    @Override
    default SuspendableVar<T> suspendable() {
        return suspendable(this);
    }
}
package org.reactfx.value;

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
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

    /**
     * Converts {@linkplain DoubleProperty} to {@code Var<Double>} to help deal
     * with the consequences of {@linkplain DoubleProperty} not being a subtype
     * of {@code Property<Double>}.
     */
    static Var<Double> doubleVar(DoubleProperty p) {
        return mapBidirectional(p, Number::doubleValue, Function.identity());
    }

    /**
     * Converts {@linkplain FloatProperty} to {@code Var<Float>} to help deal
     * with the consequences of {@linkplain FloatProperty} not being a subtype
     * of {@code Property<Float>}.
     */
    static Var<Float> floatVar(FloatProperty p) {
        return mapBidirectional(p, Number::floatValue, Function.identity());
    }

    /**
     * Converts {@linkplain IntegerProperty} to {@code Var<Integer>} to help
     * deal with the consequences of {@linkplain IntegerProperty} not being a
     * subtype of {@code Property<Integer>}.
     */
    static Var<Integer> integerVar(IntegerProperty p) {
        return mapBidirectional(p, Number::intValue, Function.identity());
    }

    /**
     * Converts {@linkplain LongProperty} to {@code Var<Long>} to help deal
     * with the consequences of {@linkplain LongProperty} not being a subtype
     * of {@code Property<Long>}.
     */
    static Var<Long> longVar(LongProperty p) {
        return mapBidirectional(p, Number::longValue, Function.identity());
    }

    static <T, U> Var<U> mapBidirectional(
            Property<T> src,
            Function<? super T, ? extends U> f,
            Function<? super U, ? extends T> g) {
        return Val.<T, U>map(src, f).asVar(u -> src.setValue(g.apply(u)));
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

    default <U> Var<U> mapBidirectional(
            Function<? super T, ? extends U> f,
            Function<? super U, ? extends T> g) {
        return mapBidirectional(this, f, g);
    }
}
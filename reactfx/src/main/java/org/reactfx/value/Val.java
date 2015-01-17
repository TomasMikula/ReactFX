package org.reactfx.value;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.reactfx.Subscription;
import org.reactfx.util.WrapperBase;

/**
 * Adds more operations to {@link ObservableValue}.
 */
public interface Val<T> extends ObservableValue<T> {

    /* **************** *
     * Abstract methods *
     * **************** */

    void addObserver(Consumer<? super T> oldValueObserver);
    void removeObserver(Consumer<? super T> oldValueObserver);


    /* *************** *
     * Default methods *
     * *************** */

    default Subscription observe(Consumer<? super T> oldValueObserver) {
        addObserver(oldValueObserver);
        return () -> removeObserver(oldValueObserver);
    }

    default Subscription pin() {
        return observe(oldVal -> {});
    }

    @Override
    default void addListener(InvalidationListener listener) {
        addObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(InvalidationListener listener) {
        removeObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    @Override
    default void addListener(ChangeListener<? super T> listener) {
        addObserver(new ChangeListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(ChangeListener<? super T> listener) {
        removeObserver(new ChangeListenerWrapper<>(this, listener));
    }

    /**
     * Adds an invalidation listener and returns a Subscription that can be
     * used to remove that listener.
     *
     * <pre>
     * {@code
     * Subscription s = observable.observeInvalidations(obs -> doSomething());
     *
     * // later
     * s.unsubscribe();
     * }</pre>
     *
     * is equivalent to
     *
     * <pre>
     * {@code
     * InvalidationListener l = obs -> doSomething();
     * observable.addListener(l);
     *
     * // later
     * observable.removeListener(l);
     * }</pre>
     */
    default Subscription observeInvalidations(InvalidationListener listener) {
        return observe(oldValue -> listener.invalidated(this));
    }

    /**
     * Adds a change listener and returns a Subscription that can be
     * used to remove that listener. See the example at
     * {@link #observeInvalidations(InvalidationListener)}.
     */
    default Subscription observeChanges(ChangeListener<? super T> listener) {
        return observe(oldValue -> listener.changed(this, oldValue, getValue()));
    }

    /**
     * Checks whether this {@linkplain Val} holds a (non-null) value.
     * @return {@code true} if this {@linkplain Val} holds a (non-null) value,
     * {@code false} otherwise.
     */
    default boolean isPresent() {
        return getValue() != null;
    }

    /**
     * Inverse of {@link #isPresent()}.
     */
    default boolean isEmpty() {
        return getValue() == null;
    }

    /**
     * Invokes the given function if this {@linkplain Val} holds a (non-null)
     * value.
     * @param f function to invoke on the value currently held by this
     * {@linkplain Val}.
     */
    default void ifPresent(Consumer<? super T> f) {
        T val = getValue();
        if(val != null) {
            f.accept(val);
        }
    }

    /**
     * Returns the value currently held by this {@linkplain Val}.
     * @throws NoSuchElementException if there is no value present.
     */
    default T getOrThrow() {
        T res = getValue();
        if(res != null) {
            return res;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the value currently held by this {@linkplain Val}. If this
     * {@linkplain Val} is empty, {@code defaultValue} is returned instead.
     * @param defaultValue value to return if there is no value present in
     * this {@linkplain Val}.
     */
    default T getOrElse(T defaultValue) {
        T res = getValue();
        if(res != null) {
            return res;
        } else {
            return defaultValue;
        }
    }

    /**
     * Like {@link #getOrElse(Object)}, except the default value is computed
     * by {@code defaultSupplier} only when necessary.
     * @param defaultSupplier computation to produce default value, if this
     * {@linkplain Val} is empty.
     */
    default T getOrSupply(Supplier<? extends T> defaultSupplier) {
        T res = getValue();
        if(res != null) {
            return res;
        } else {
            return defaultSupplier.get();
        }
    }

    /**
     * Returns an {@code Optional} describing the value currently held by this
     * {@linkplain Val}, or and empty {@code Optional} if this {@linkplain Val}
     * is empty.
     */
    default Optional<T> getOpt() {
        return Optional.ofNullable(getValue());
    }

    /**
     * Returns a new {@linkplain Val} that holds the value held by this
     * {@linkplain Val}, or {@code other} when this {@linkplain Val} is empty.
     */
    default Val<T> orElseConst(T other) {
        return orElseConst(this, other);
    }

    /**
     * Returns a new {@linkplain Val} that holds the value held by this
     * {@linkplain Val}, or the value held by {@code other} when this
     * {@linkplain Val} is empty.
     */
    default Val<T> orElse(ObservableValue<T> other) {
        return orElse(this, other);
    }

    /**
     * Returns a new {@linkplain Val} that holds the same value
     * as this {@linkplain Val} when the value satisfies the predicate
     * and is empty when this {@linkplain Val} is empty or its value
     * does not satisfy the given predicate.
     */
    default Val<T> filter(Predicate<? super T> p) {
        return filter(this, p);
    }

    /**
     * Returns a new {@linkplain Val} that holds a mapping of the value held by
     * this {@linkplain Val}, and is empty when this {@linkplain Val} is empty.
     * @param f function to map the value held by this {@linkplain Val}.
     */
    default <U> Val<U> map(Function<? super T, ? extends U> f) {
        return map(this, f);
    }

    /**
     * Returns a new {@linkplain Val} that, when this {@linkplain Val} holds
     * value {@code x}, holds the value held by {@code f(x)}, and is empty
     * when this {@linkplain Val} is empty.
     */
    default <U> Val<U> flatMap(
            Function<? super T, ? extends ObservableValue<U>> f) {
        return flatMap(this, f);
    }

    /**
     * Similar to {@link #flatMap(Function)}, except the returned Binding is
     * also a Property. This means you can call {@code setValue()} and
     * {@code bind()} methods on the returned value, which delegate to the
     * currently selected Property.
     *
     * <p>As the value of this {@linkplain Val} changes, so does the selected
     * Property. When the Property returned from this method is bound, as the
     * selected Property changes, the previously selected property is unbound
     * and the newly selected property is bound.
     *
     * <p>Note that if the currently selected property is {@code null}, then
     * calling {@code getValue()} on the returned value will return {@code null}
     * regardless of any prior call to {@code setValue()} or {@code bind()}.
     */
    default <U> Var<U> selectVar(
            Function<? super T, ? extends Property<U>> f) {
        return selectVar(this, f);
    }

    default <U> Var<U> selectVar(
            Function<? super T, ? extends Property<U>> f,
            U resetToOnUnbind) {
        return selectVar(this, f, resetToOnUnbind);
    }


    /* ************** *
     * Static methods *
     * ************** */

    static <T> Subscription observeInvalidations(
            ObservableValue<? extends T> obs,
            InvalidationListener listener) {
        if(obs instanceof Val) {
            return ((Val<? extends T>) obs).observeInvalidations(listener);
        } else {
            obs.addListener(listener);
            return () -> obs.removeListener(listener);
        }
    }

    static <T> Val<T> orElseConst(ObservableValue<? extends T> src, T other) {
        return new OrElseConst<>(src, other);
    }

    static <T> Val<T> orElse(
            ObservableValue<? extends T> src,
            ObservableValue<? extends T> other) {
        return new OrElse<>(src, other);
    }

    static <T> Val<T> filter(
            ObservableValue<T> src,
            Predicate<? super T> p) {
        return new FilteredVal<>(src, p);
    }

    static <T, U> Val<U> map(
            ObservableValue<T> src,
            Function<? super T, ? extends U> f) {
        return new MappedVal<>(src, f);
    }

    static <T, U> Val<U> flatMap(
            ObservableValue<T> src,
            Function<? super T, ? extends ObservableValue<U>> f) {
        return new FlatMappedVal<>(src, f);
    }

    static <T, U> Var<U> selectVar(
            ObservableValue<T> src,
            Function<? super T, ? extends Property<U>> f) {
        return new FlatMappedVar<>(src, f);
    }

    static <T, U> Var<U> selectVar(
            ObservableValue<T> src,
            Function<? super T, ? extends Property<U>> f,
            U resetToOnUnbind) {
        return new FlatMappedVar<>(src, f, resetToOnUnbind);
    }
}


class InvalidationListenerWrapper<T>
extends WrapperBase<InvalidationListener>
implements Consumer<T> {
    private final ObservableValue<T> obs;

    public InvalidationListenerWrapper(
            ObservableValue<T> obs,
            InvalidationListener listener) {
        super(listener);
        this.obs = obs;
    }

    @Override
    public void accept(T oldValue) {
        getWrappedValue().invalidated(obs);
    }
}


class ChangeListenerWrapper<T>
extends WrapperBase<ChangeListener<? super T>>
implements Consumer<T> {
    private final ObservableValue<T> obs;

    public ChangeListenerWrapper(
            ObservableValue<T> obs,
            ChangeListener<? super T> listener) {
        super(listener);
        this.obs = obs;
    }

    @Override
    public void accept(T oldValue) {
        getWrappedValue().changed(obs, oldValue, obs.getValue());
    }
}
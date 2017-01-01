package org.reactfx.value;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.animation.Interpolatable;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.EventStreams;
import org.reactfx.Observable;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.util.HexaFunction;
import org.reactfx.util.Interpolator;
import org.reactfx.util.PentaFunction;
import org.reactfx.util.TetraFunction;
import org.reactfx.util.TriFunction;
import org.reactfx.util.WrapperBase;

/**
 * Adds more operations to {@link ObservableValue}.
 *
 * <p>Canonical observer of {@code Val<T>} is an <em>invalidation observer</em>
 * of type {@code Consumer<? super T>}, which accepts the _invalidated_ value.
 * This is different from {@linkplain InvalidationListener}, which does not
 * accept the invalidated value.
 */
public interface Val<T>
extends ObservableValue<T>, Observable<Consumer<? super T>> {

    /* *************** *
     * Default methods *
     * *************** */

    default void addInvalidationObserver(Consumer<? super T> observer) {
        addObserver(observer);
    }

    default void removeInvalidationObserver(Consumer<? super T> observer) {
        removeObserver(observer);
    }

    default Subscription observeInvalidations(
            Consumer<? super T> oldValueObserver) {
        return observe(oldValueObserver);
    }

    default Subscription pin() {
        return observeInvalidations(oldVal -> {});
    }

    @Override
    default void addListener(InvalidationListener listener) {
        addInvalidationObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(InvalidationListener listener) {
        removeInvalidationObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    @Override
    default void addListener(ChangeListener<? super T> listener) {
        addInvalidationObserver(new ChangeListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(ChangeListener<? super T> listener) {
        removeInvalidationObserver(new ChangeListenerWrapper<>(this, listener));
    }

    /**
     * Adds a change listener and returns a Subscription that can be
     * used to remove that listener.
     */
    default Subscription observeChanges(ChangeListener<? super T> listener) {
        return observeInvalidations(new ChangeListenerWrapper<>(this, listener));
    }

    /**
     * Returns a stream of invalidated values, which emits the invalidated value
     * (i.e. the old value) on each invalidation of this observable value.
     */
    default EventStream<T> invalidations() {
        return new EventStreamBase<T>() {
            @Override
            protected Subscription observeInputs() {
                return observeInvalidations(this::emit);
            }
        };
    }

    /**
     * Returns a stream of changed values, which emits the changed value
     * (i.e. the old and the new value) on each change of this observable value.
     */
    default EventStream<Change<T>> changes() {
        return EventStreams.changesOf(this);
    }

    /**
     * Returns a stream of values of this {@linkplain Val}. The returned stream
     * emits the current value of this {@linkplain Val} for each new subscriber
     * and then the new value whenever the value changes.
     */
    default EventStream<T> values() {
        return EventStreams.valuesOf(this);
    }

    /**
     * Returns a stream of non-null values of this {@linkplain Val}. The returned stream
     * emits the current value of this {@linkplain Val} if it is not null for each new subscriber
     * and then the new value if it is not null whenever the value changes.
     */
    default EventStream<T> nonNullValues() {
        return EventStreams.nonNullValuesOf(this);
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
     * Like {@link #map(Function)}, but also allows dynamically changing
     * map function.
     */
    default <U> Val<U> mapDynamic(
            ObservableValue<? extends Function<? super T, ? extends U>> f) {
        return mapDynamic(this, f);
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
     * Similar to {@link #flatMap(Function)}, except the returned Val is also
     * a Var. This means you can call {@code setValue()} and {@code bind()}
     * methods on the returned value, which delegate to the currently selected
     * Property.
     *
     * <p>As the value of this {@linkplain Val} changes, so does the selected
     * Property. When the Var returned from this method is bound, as the
     * selected Property changes, the previously selected Property is unbound
     * and the newly selected Property is bound.
     *
     * <p>Note that if the currently selected Property is {@code null}, then
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

    /**
     * Returns a new {@linkplain Val} that only observes this {@linkplain Val}
     * when {@code condition} is {@code true}. More precisely, the returned
     * {@linkplain Val} observes {@code condition} whenever it itself has at
     * least one observer and observes {@code this} {@linkplain Val} whenever
     * it itself has at least one observer <em>and</em> the value of
     * {@code condition} is {@code true}. When {@code condition} is
     * {@code true}, the returned {@linkplain Val} has the same value as this
     * {@linkplain Val}. When {@code condition} is {@code false}, the returned
     * {@linkplain Val} has the value that was held by this {@linkplain Val} at
     * the time when {@code condition} changed to {@code false}.
     */
    default Val<T> conditionOn(ObservableValue<Boolean> condition) {
        return conditionOn(this, condition);
    }

    /**
     * Equivalent to {@link #conditionOn(ObservableValue)} where the condition
     * is that {@code node} is <em>showing</em>: it is part of a scene graph
     * ({@link Node#sceneProperty()} is not {@code null}), its scene is part of
     * a window ({@link Scene#windowProperty()} is not {@code null}) and the
     * window is showing ({@link Window#showingProperty()} is {@code true}).
     */
    default Val<T> conditionOnShowing(Node node) {
        return conditionOnShowing(this, node);
    }

    default SuspendableVal<T> suspendable() {
        return suspendable(this);
    }

    /**
     * Returns a new {@linkplain Val} that gradually transitions to the value
     * of this {@linkplain Val} every time this {@linkplain Val} changes.
     *
     * <p>When the returned {@linkplain Val} has no observer, there is no
     * gradual transition taking place. This means that there is no animation
     * running in the background that would consume system resources. This also
     * means that in that case {@link #getValue()} always returns the target
     * value (i.e. the current value of this {@linkplain Val}), instead of any
     * intermediate interpolated value.
     *
     * @param duration function that calculates the desired duration of the
     * transition for two boundary values.
     * @param interpolator calculates the interpolated value between two
     * boundary values, given a fraction.
     */
    default Val<T> animate(
            BiFunction<? super T, ? super T, Duration> duration,
            Interpolator<T> interpolator) {
        return animate(this, duration, interpolator);
    }

    /**
     * Returns a new {@linkplain Val} that gradually transitions to the value
     * of this {@linkplain Val} every time this {@linkplain Val} changes.
     *
     * <p>When the returned {@linkplain Val} has no observer, there is no
     * gradual transition taking place. This means that there is no animation
     * running in the background that would consume system resources. This also
     * means that in that case {@link #getValue()} always returns the target
     * value (i.e. the current value of this {@linkplain Val}), instead of any
     * intermediate interpolated value.
     *
     * @param duration the desired duration of the transition
     * @param interpolator calculates the interpolated value between two
     * boundary values, given a fraction.
     */
    default Val<T> animate(
            Duration duration,
            Interpolator<T> interpolator) {
        return animate(this, duration, interpolator);
    }

    /**
     * Let's this {@linkplain Val} be viewed as a {@linkplain Var}, with the
     * given {@code setValue} function serving the purpose of
     * {@link Var#setValue(Object)}.
     * @see Var#fromVal(ObservableValue, Consumer)
     */
    default Var<T> asVar(Consumer<T> setValue) {
        return new VarFromVal<>(this, setValue);
    }

    /**
     * Returns a {@linkplain LiveList} view of this {@linkplain Val}. The
     * returned list will have size 1 when this {@linkplain Val} is present
     * (i.e. not {@code null}) and size 0 otherwise.
     */
    default LiveList<T> asList() {
        return LiveList.wrapVal(this);
    }


    /* ************** *
     * Static methods *
     * ************** */

    /**
     * Returns a {@linkplain Val} wrapper around {@linkplain ObservableValue}.
     * If the argument is already a {@code Val<T>}, no wrapping occurs and the
     * argument is returned as is.
     * Note that one rarely needs to use this method, because most of the time
     * one can use the appropriate static method directly to get the desired
     * result. For example, instead of
     *
     * <pre>
     * {@code
     * Val.wrap(obs).orElse(other)
     * }
     * </pre>
     *
     * one can write
     *
     * <pre>
     * {@code
     * Val.orElse(obs, other)
     * }
     * </pre>
     *
     * However, an explicit wrapper is necessary if access to
     * {@link #observeInvalidations(Consumer)}, or {@link #invalidations()}
     * is needed, since there is no direct static method equivalent for them.
     */
    static <T> Val<T> wrap(ObservableValue<T> obs) {
        return obs instanceof Val
                ? (Val<T>) obs
                : new ValWrapper<>(obs);
    }

    static <T> Subscription observeChanges(
            ObservableValue<? extends T> obs,
            ChangeListener<? super T> listener) {
        if(obs instanceof Val) {
            return ((Val<? extends T>) obs).observeChanges(listener);
        } else {
            obs.addListener(listener);
            return () -> obs.removeListener(listener);
        }
    }

    static Subscription observeInvalidations(
            ObservableValue<?> obs,
            InvalidationListener listener) {
        obs.addListener(listener);
        return () -> obs.removeListener(listener);
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
        return map(src, t -> p.test(t) ? t : null);
    }

    static <T, U> Val<U> map(
            ObservableValue<T> src,
            Function<? super T, ? extends U> f) {
        return new MappedVal<>(src, f);
    }

    static <T, U> Val<U> mapDynamic(
            ObservableValue<T> src,
            ObservableValue<? extends Function<? super T, ? extends U>> f) {
        return combine(
                src, f,
                (t, fn) -> t == null || fn == null ? null : fn.apply(t));
    }

    static <T, U> Val<U> flatMap(
            ObservableValue<T> src,
            Function<? super T, ? extends ObservableValue<U>> f) {
        return new FlatMappedVal<>(src, (Function) f);
    }

    static <T, U> Var<U> selectVar(
            ObservableValue<T> src,
            Function<? super T, ? extends Property<U>> f) {
        return new FlatMappedVar<>(src, (Function) f);
    }

    static <T, U> Var<U> selectVar(
            ObservableValue<T> src,
            Function<? super T, ? extends Property<U>> f,
            U resetToOnUnbind) {
        return new FlatMappedVar<>(src, f, resetToOnUnbind);
    }

    static <T> Val<T> conditionOn(
            ObservableValue<T> obs,
            ObservableValue<Boolean> condition) {
        return flatMap(condition, con -> con ? obs : constant(obs.getValue()));
    }

    static <T> Val<T> conditionOnShowing(ObservableValue<T> obs, Node node) {
        return conditionOn(obs, showingProperty(node));
    }

    static <T> SuspendableVal<T> suspendable(ObservableValue<T> obs) {
        if(obs instanceof SuspendableVal) {
            return (SuspendableVal<T>) obs;
        } else {
            Val<T> val = obs instanceof Val
                    ? (Val<T>) obs
                    : new ValWrapper<>(obs);
            return new SuspendableValWrapper<>(val);
        }
    }

    /**
     * Creates a new {@linkplain Val} that gradually transitions to the value
     * of the given {@linkplain ObservableValue} {@code obs} every time
     * {@code obs} changes.
     *
     * <p>When the returned {@linkplain Val} has no observer, there is no
     * gradual transition taking place. This means that there is no animation
     * running in the background that would consume system resources. This also
     * means that in that case {@link #getValue()} always returns the target
     * value (i.e. the current value of {@code obs}), instead of any intermediate
     * interpolated value.
     *
     * @param obs observable value to animate
     * @param duration function that calculates the desired duration of the
     * transition for two boundary values.
     * @param interpolator calculates the interpolated value between two
     * boundary values, given a fraction.
     */
    static <T> Val<T> animate(
            ObservableValue<T> obs,
            BiFunction<? super T, ? super T, Duration> duration,
            Interpolator<T> interpolator) {
        return new AnimatedVal<>(obs, duration, interpolator);
    }

    /**
     * Creates a new {@linkplain Val} that gradually transitions to the value
     * of the given {@linkplain ObservableValue} {@code obs} every time
     * {@code obs} changes.
     *
     * <p>When the returned {@linkplain Val} has no observer, there is no
     * gradual transition taking place. This means that there is no animation
     * running in the background that would consume system resources. This also
     * means that in that case {@link #getValue()} always returns the target
     * value (i.e. the current value of {@code obs}), instead of any intermediate
     * interpolated value.
     *
     * @param obs observable value to animate
     * @param duration the desired duration of the transition
     * @param interpolator calculates the interpolated value between two
     * boundary values, given a fraction.
     */
    static <T> Val<T> animate(
            ObservableValue<T> obs,
            Duration duration,
            Interpolator<T> interpolator) {
        return animate(obs, (a, b) -> duration, interpolator);
    }

    /**
     * Like {@link #animate(ObservableValue, BiFunction, Interpolator)}, but
     * uses the interpolation defined by the {@linkplain Interpolatable} type
     * {@code T}.
     */
    static <T extends Interpolatable<T>> Val<T> animate(
            ObservableValue<T> obs,
            BiFunction<? super T, ? super T, Duration> duration) {
        return animate(obs, duration, Interpolator.get());
    }

    /**
     * Like {@link #animate(ObservableValue, Duration, Interpolator)}, but
     * uses the interpolation defined by the {@linkplain Interpolatable} type
     * {@code T}.
     */
    static <T extends Interpolatable<T>> Val<T> animate(
            ObservableValue<T> obs,
            Duration duration) {
        return animate(obs, duration, Interpolator.get());
    }


    static <A, B, R> Val<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            BiFunction<? super A, ? super B, ? extends R> f) {
        return create(
                () -> {
                    if(src1.getValue() != null && src2.getValue() != null) {
                        return f.apply(src1.getValue(), src2.getValue());
                    } else {
                        return null;
                    }
                },
                src1, src2);
    }

    static <A, B, C, R> Val<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            TriFunction<? super A, ? super B, ? super C,  ? extends R> f) {
        return create(
                () -> {
                    if(src1.getValue() != null &&
                            src2.getValue() != null &&
                            src3.getValue() != null) {
                        return f.apply(
                            src1.getValue(), src2.getValue(), src3.getValue());
                    } else {
                        return null;
                    }
                },
                src1, src2, src3);
    }

    static <A, B, C, D, R> Val<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            TetraFunction<? super A, ? super B, ? super C, ? super D,  ? extends R> f) {
        return create(
                () -> {
                    if(src1.getValue() != null &&
                            src2.getValue() != null &&
                            src3.getValue() != null &&
                            src4.getValue() != null) {
                        return f.apply(
                            src1.getValue(), src2.getValue(),
                            src3.getValue(), src4.getValue());
                    } else {
                        return null;
                    }
                },
                src1, src2, src3, src4);
    }

    static <A, B, C, D, E, R> Val<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            ObservableValue<E> src5,
            PentaFunction<? super A, ? super B, ? super C, ? super D, ? super E,  ? extends R> f) {
        return create(
                () -> {
                    if(src1.getValue() != null &&
                            src2.getValue() != null &&
                            src3.getValue() != null &&
                            src4.getValue() != null &&
                            src5.getValue() != null) {
                        return f.apply(
                            src1.getValue(), src2.getValue(), src3.getValue(),
                            src4.getValue(), src5.getValue());
                    } else {
                        return null;
                    }
                },
                src1, src2, src3, src4, src5);
    }

    static <A, B, C, D, E, F, R> Val<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            ObservableValue<E> src5,
            ObservableValue<F> src6,
            HexaFunction<? super A, ? super B, ? super C, ? super D, ? super E, ? super F,  ? extends R> f) {
        return create(
                () -> {
                    if(src1.getValue() != null &&
                            src2.getValue() != null &&
                            src3.getValue() != null &&
                            src4.getValue() != null &&
                            src5.getValue() != null &&
                            src6.getValue() != null) {
                        return f.apply(
                            src1.getValue(), src2.getValue(), src3.getValue(),
                            src4.getValue(), src5.getValue(), src6.getValue());
                    } else {
                        return null;
                    }
                },
                src1, src2, src3, src4, src5, src6);
    }

    static <T> Val<T> create(
            Supplier<? extends T> computeValue,
            javafx.beans.Observable... dependencies) {
        return new ValBase<T>() {

            @Override
            protected Subscription connect() {
                InvalidationListener listener = obs -> invalidate();
                for(javafx.beans.Observable dep: dependencies) {
                    dep.addListener(listener);
                }

                return () -> {
                    for(javafx.beans.Observable dep: dependencies) {
                        dep.removeListener(listener);
                    }
                };
            }

            @Override
            protected T computeValue() {
                return computeValue.get();
            }
        };
    }

    static <T> Val<T> create(
            Supplier<? extends T> computeValue,
            EventStream<?> invalidations) {
        return new ValBase<T>() {

            @Override
            protected Subscription connect() {
                return invalidations.subscribe(x -> invalidate());
            }

            @Override
            protected T computeValue() {
                return computeValue.get();
            }
        };
    }

    /**
     * Returns a constant {@linkplain Val} that holds the given value.
     * The value never changes and no notifications are ever produced.
     */
    static <T> Val<T> constant(T value) {
        return new ConstVal<>(value);
    }

    /**
     * Returns a {@linkplain Val} whose value is {@code true} when {@code node}
     * is <em>showing</em>:  it is part of a scene graph
     * ({@link Node#sceneProperty()} is not {@code null}), its scene is part of
     * a window ({@link Scene#windowProperty()} is not {@code null}) and the
     * window is showing ({@link Window#showingProperty()} is {@code true}).
     */
    static Val<Boolean> showingProperty(Node node) {
        return Val
                .flatMap(node.sceneProperty(), Scene::windowProperty)
                .flatMap(Window::showingProperty)
                .orElseConst(false);
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
        T newValue = obs.getValue();
        if(!Objects.equals(oldValue, newValue)) {
            getWrappedValue().changed(obs, oldValue, newValue);
        }
    }
}

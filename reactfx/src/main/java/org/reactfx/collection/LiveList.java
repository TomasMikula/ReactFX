package org.reactfx.collection;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Observable;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList.QuasiChangeObserver;
import org.reactfx.collection.LiveList.QuasiModificationObserver;
import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.Experimental;
import org.reactfx.util.WrapperBase;
import org.reactfx.value.Val;

/**
 * Adds additional methods to {@link ObservableList} for observing its changes and for composition (similar to
 * {@link EventStream}).
 *
 * <p>
 *     In observing its changes, there are two types of more helpful objects/observers to use instead of
 *     {@link ListChangeListener.Change}. There are the regular types (the first pair listed below) that
 *     better adhere to how one thinks. There are also the {@code Quasi}-types (the second pair below) that
 *     only which items were removed and how many were added, but which doesn't specify which items were added.
 *     The two types allow more flexibility as to what is needed or appropriate in various cases:
 * </p>
 * <ul>
 *     <li>{@link ListChange} (e.g. {@link #observeChanges(Consumer)}). It is similar to
 *     {@link ListChangeListener.Change} in that it stores a list of modifications but it is more straight-forward
 *     than its counterpart.</li>
 *     <li>{@link ListModification} (e.g. {@link #observeModifications(Consumer)}). It is an actual modification
 *     that occurred in the list. </li>
 *     <li>{@link QuasiListChange} (e.g. {@link #observeQuasiChanges(QuasiChangeObserver)})</li>
 *     <li>{@link QuasiListModification} (e.g. {@link #observeQuasiModifications(QuasiModificationObserver)})</li>
 * </ul>
 *
 * <p>As for composition, one can use methods like:</p>
 * <ul>
 *     <li>{@link #map(Function)}</li>
 *     <li>{@link #mapDynamic(ObservableValue)}</li>
 *     <li>{@link #filtered(Predicate)}</li>
 *     <li>{@link #sizeProperty()}</li>
 *     <li>{@link #reduce(BinaryOperator)}</li>
 *     <li>
 *         or methods that determine what items are stored in the returned list, such as
 *         {@link #suspendable()} and {@link #memoize()}.
 *     </li>
 * </ul>
 *
 * @param <E> type of list elements
 */
public interface LiveList<E>
extends ObservableList<E>, Observable<LiveList.Observer<? super E, ?>> {

    /* ************ *
     * Nested Types *
     * ************ */

    public interface Observer<E, O> {
        AccumulatorSize sizeOf(ListModificationSequence<? extends E> mods);
        O headOf(ListModificationSequence<? extends E> mods);
        <F extends E> ListModificationSequence<F> tailOf(ListModificationSequence<F> mods);

        void onChange(O change);
    }

    /**
     * Observes the {@link QuasiListChange}s (the list of {@link QuasiListModification}) of this list
     */
    @FunctionalInterface
    public interface QuasiChangeObserver<E>
    extends Observer<E, QuasiListChange<? extends E>> {

        @Override
        default AccumulatorSize sizeOf(
                ListModificationSequence<? extends E> mods) {
            return AccumulatorSize.ONE;
        }

        @Override
        default QuasiListChange<? extends E> headOf(
                ListModificationSequence<? extends E> mods) {
            return mods.asListChange();
        }

        @Override
        default <F extends E> ListModificationSequence<F> tailOf(
                ListModificationSequence<F> mods) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Observes the individual {@link QuasiListModification}s of this list
     */
    @FunctionalInterface
    public interface QuasiModificationObserver<E>
    extends Observer<E, QuasiListModification<? extends E>> {

        @Override
        default AccumulatorSize sizeOf(
                ListModificationSequence<? extends E> mods) {
            return AccumulatorSize.fromInt(mods.getModificationCount());
        }

        @Override
        default QuasiListModification<? extends E> headOf(
                ListModificationSequence<? extends E> mods) {
            return mods.getModifications().get(0);
        }

        @Override
        default <F extends E> ListModificationSequence<F> tailOf(
                ListModificationSequence<F> mods) {
            return mods.asListChangeAccumulator().drop(1);
        }
    }


    /* *************** *
     * Default Methods *
     * *************** */

    /**
     * See {@link QuasiChangeObserver}
     */
    default void addQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        addObserver(observer);
    }

    /**
     * See {@link QuasiChangeObserver}
     */
    default void removeQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        removeObserver(observer);
    }

    /**
     * See {@link QuasiModificationObserver}
     */
    default void addQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        addObserver(observer);
    }

    /**
     * See {@link QuasiModificationObserver}
     */
    default void removeQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        removeObserver(observer);
    }

    /**
     * See {@link ListChange}
     */
    default void addChangeObserver(Consumer<? super ListChange<? extends E>> observer) {
        addQuasiChangeObserver(new ChangeObserverWrapper<>(this, observer));
    }

    /**
     * See {@link ListChange}
     */
    default void removeChangeObserver(Consumer<? super ListChange<? extends E>> observer) {
        removeQuasiChangeObserver(new ChangeObserverWrapper<>(this, observer));
    }

    /**
     * See {@link ListModification}
     */
    default void addModificationObserver(Consumer<? super ListModification<? extends E>> observer) {
        addQuasiModificationObserver(new ModificationObserverWrapper<>(this, observer));
    }

    /**
     * See {@link ListModification}
     */
    default void removeModificationObserver(Consumer<? super ListModification<? extends E>> observer) {
        removeQuasiModificationObserver(new ModificationObserverWrapper<>(this, observer));
    }

    /**
     * See {@link QuasiChangeObserver}
     */
    default Subscription observeQuasiChanges(QuasiChangeObserver<? super E> observer) {
        addQuasiChangeObserver(observer);
        return () -> removeQuasiChangeObserver(observer);
    }

    /**
     * See {@link QuasiModificationObserver}
     */
    default Subscription observeQuasiModifications(QuasiModificationObserver<? super E> observer) {
        addQuasiModificationObserver(observer);
        return () -> removeQuasiModificationObserver(observer);
    }

    /**
     * See {@link ListChange}
     */
    default Subscription observeChanges(Consumer<? super ListChange<? extends E>> observer) {
        addChangeObserver(observer);
        return () -> removeChangeObserver(observer);
    }

    /**
     * See {@link ListModification}
     */
    default Subscription observeModifications(Consumer<? super ListModification<? extends E>> observer) {
        addModificationObserver(observer);
        return () -> removeModificationObserver(observer);
    }

    @Override
    default void addListener(ListChangeListener<? super E> listener) {
        addQuasiChangeObserver(new ChangeListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(ListChangeListener<? super E> listener) {
        removeQuasiChangeObserver(new ChangeListenerWrapper<>(this, listener));
    }

    @Override
    default void addListener(InvalidationListener listener) {
        addQuasiChangeObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    @Override
    default void removeListener(InvalidationListener listener) {
        removeQuasiChangeObserver(new InvalidationListenerWrapper<>(this, listener));
    }

    default Subscription pin() {
        return observeQuasiChanges(qc -> {});
    }

    /**
     * Calls {@link #sizeOf(ObservableList)} using this list as the observable list in that method
     */
    default Val<Integer> sizeProperty() {
        return sizeOf(this);
    }

    /**
     * Calls {@link #map(ObservableList, Function)} using this list as the observable list in that method
     */
    default <F> LiveList<F> map(Function<? super E, ? extends F> f) {
        return map(this, f);
    }

    /**
     * Calls {@link #mapDynamic(ObservableList, ObservableValue)} using this list as the observable list in that method
     */
    default <F> LiveList<F> mapDynamic(
            ObservableValue<? extends Function<? super E, ? extends F>> f) {
        return mapDynamic(this, f);
    }

    /**
     * Calls {@link #suspendable(ObservableList)} using this list as the observable list in that method
     */
    default SuspendableList<E> suspendable() {
        return suspendable(this);
    }

    /**
     * Calls {@link #memoize(ObservableList)} using this list as the observable list in that method
     */
    default MemoizationList<E> memoize() {
        return memoize(this);
    }

    /**
     * Calls {@link #reduce(ObservableList, BinaryOperator)} using this list as the observable list in that method.
     */
    default Val<E> reduce(BinaryOperator<E> reduction) {
        return reduce(this, reduction);
    }

    @Experimental
    default Val<E> reduceRange(
            ObservableValue<IndexRange> range, BinaryOperator<E> reduction) {
        return reduceRange(this, range, reduction);
    }

    @Experimental
    default <T> Val<T> collapse(Function<? super List<E>, ? extends T> f) {
        return collapse(this, f);
    }

    @Experimental
    default <T> Val<T> collapseDynamic(
            ObservableValue<? extends Function<? super List<E>, ? extends T>> f) {
        return collapseDynamic(this, f);
    }

    /**
     * Returns an {@link EventStream} that emits a {@link QuasiListChange} event every time this list changes.
     */
    default EventStream<QuasiListChange<? extends E>> quasiChanges() {
        return new EventStreamBase<QuasiListChange<? extends E>>() {
            @Override
            protected Subscription observeInputs() {
                return observeQuasiChanges(this::emit);
            }
        };
    }

    /**
     * Returns an {@link EventStream} that emits a {@link ListChange} event every time this list changes.
     */
    default EventStream<ListChange<? extends E>> changes() {
        return quasiChanges().map(qc -> QuasiListChange.instantiate(qc, this));
    }

    /**
     * Returns an {@link EventStream} that emits a {@link QuasiListModification} event every time this list changes.
     */
    default EventStream<QuasiListModification<? extends E>> quasiModifications() {
        return new EventStreamBase<QuasiListModification<? extends E>>() {
            @Override
            protected Subscription observeInputs() {
                return observeQuasiModifications(this::emit);
            }
        };
    }

    /**
     * Returns an {@link EventStream} that emits a {@link ListModification} event every time this list changes.
     */
    default EventStream<ListModification<? extends E>> modifications() {
        return quasiModifications().map(qm -> QuasiListModification.instantiate(qm, this));
    }


    /* ************** *
     * Static Methods *
     * ************** */

    /**
     * See {@link QuasiChangeObserver}
     */
    static <E> Subscription observeQuasiChanges(
            ObservableList<? extends E> list,
            QuasiChangeObserver<? super E> observer) {
        if(list instanceof LiveList) {
            LiveList<? extends E> lst = (LiveList<? extends E>) list;
            return lst.observeQuasiChanges(observer);
        } else {
            ListChangeListener<E> listener = ch -> {
                QuasiListChange<? extends E> change = QuasiListChange.from(ch);
                observer.onChange(change);
            };
            list.addListener(listener);
            return () -> list.removeListener(listener);
        }
    }

    static <E> Subscription observeChanges(
            ObservableList<E> list,
            Consumer<? super ListChange<? extends E>> observer) {

        return observeQuasiChanges(
                list, qc -> observer.accept(QuasiListChange.instantiate(qc, list)));
    }

    static <E> EventStream<QuasiListChange<? extends E>> quasiChangesOf(
            ObservableList<E> list) {
        if(list instanceof LiveList) {
            LiveList<E> lst = (LiveList<E>) list;
            return lst.quasiChanges();
        } else {
            return new EventStreamBase<QuasiListChange<? extends E>>() {
                @Override
                protected Subscription observeInputs() {
                    return LiveList.<E>observeQuasiChanges(list, this::emit);
                }
            };
        }
    }

    /**
     * Returns an {@link EventStream} that emits a {@link ListChange} every time a change occurs in the {@code list}.
     */
    static <E> EventStream<ListChange<? extends E>> changesOf(ObservableList<E> list) {
        return quasiChangesOf(list).map(qc -> QuasiListChange.instantiate(qc, list));
    }

    /**
     * Returns a {@link Val} whose value always equals the size of the given {@code list}.
     */
    static Val<Integer> sizeOf(ObservableList<?> list) {
        return Val.create(() -> list.size(), list);
    }

    /**
     * Returns a {@link LiveList} whose items are the result of applying the mapping function, {@code f}, to
     * every item in {@code list} when the returned list is first created and then only to any replacements or
     * additions after that. The size of the returned list always equals the size of {@code list}.
     */
    static <E, F> LiveList<F> map(
            ObservableList<? extends E> list,
            Function<? super E, ? extends F> f) {
        return new MappedList<>(list, f);
    }

    /**
     * Returns a {@link LiveList} whose items are the result of applying the mapping function currently stored in
     * {@code f} to every item in {@code list} when the returned list is first created and then only to any
     * replacements or additions after that. The size of the returned list always equals the size of {@code list}.
     */
    static <E, F> LiveList<F> mapDynamic(
            ObservableList<? extends E> list,
            ObservableValue<? extends Function<? super E, ? extends F>> f) {
        return new DynamicallyMappedList<>(list, f);
    }

    /**
     * Returns a {@link LiveList} whose items are the same as {@code list} when unsuspended; once suspended,
     * any updates to {@code list} will not propagate to the returned list until the returned list is unsuspended again.
     */
    static <E> SuspendableList<E> suspendable(ObservableList<E> list) {
        if(list instanceof SuspendableList) {
            return (SuspendableList<E>) list;
        } else {
            return new SuspendableListWrapper<>(list);
        }
    }

    /**
     * Returns a {@link MemoizationList} that wraps the {@code list}, so that the returned list contains all of
     * the given list's items when it hasn't been memorized or only some of the items in the list when
     * {@link MemoizationList#isMemoized(int)} is true.
     */
    static <E> MemoizationList<E> memoize(ObservableList<E> list) {
        if(list instanceof MemoizationList) {
            return (MemoizationList<E>) list;
        } else {
            return new MemoizationListImpl<>(list);
        }
    }

    /**
     * Returns a {@link Val} whose value is the result returned from applying the {@link BinaryOperator} on every
     * item in the list each time the list changes. The first value passed into the {@code reduction} is the
     * accumulated value returned from the last reduction, and the second value is the next item in the {@code list}.
     * For more clarity on how this works, see {@link EventStream#reducible(BinaryOperator)}
     */
    static <E> Val<E> reduce(
            ObservableList<E> list, BinaryOperator<E> reduction) {
        return new ListReduction<>(list, reduction);
    }

    @Experimental
    static <E> Val<E> reduceRange(
            ObservableList<E> list,
            ObservableValue<IndexRange> range,
            BinaryOperator<E> reduction) {
        return new ListRangeReduction<>(list, range, reduction);
    }

    @Experimental
    static <E, T> Val<T> collapse(
            ObservableList<? extends E> list,
            Function<? super List<E>, ? extends T> f) {
        return Val.create(() -> f.apply(Collections.unmodifiableList(list)), list);
    }

    @Experimental
    static <E, T> Val<T> collapseDynamic(
            ObservableList<? extends E> list,
            ObservableValue<? extends Function<? super List<E>, ? extends T>> f) {
        return Val.create(
                () -> f.getValue().apply(Collections.unmodifiableList(list)),
                list, f);
    }

    /**
     * Returns a {@linkplain LiveList} view of the given
     * {@linkplain ObservableValue} {@code obs}. The returned list will have
     * size 1 when the given observable value is not {@code null} and size 0
     * otherwise.
     */
    static <E> LiveList<E> wrapVal(ObservableValue<E> obs) {
        return new ValAsList<>(obs);
    }
}


class ChangeObserverWrapper<T>
extends WrapperBase<Consumer<? super ListChange<? extends T>>>
implements QuasiChangeObserver<T> {
    private final ObservableList<T> list;

    ChangeObserverWrapper(
            ObservableList<T> list,
            Consumer<? super ListChange<? extends T>> delegate) {
        super(delegate);
        this.list = list;
    }

    @Override
    public void onChange(QuasiListChange<? extends T> change) {
        getWrappedValue().accept(QuasiListChange.instantiate(change, list));
    }
}

class ModificationObserverWrapper<T>
extends WrapperBase<Consumer<? super ListModification<? extends T>>>
implements QuasiModificationObserver<T> {
    private final ObservableList<T> list;

    ModificationObserverWrapper(
            ObservableList<T> list,
            Consumer<? super ListModification<? extends T>> delegate) {
        super(delegate);
        this.list = list;
    }

    @Override
    public void onChange(QuasiListModification<? extends T> change) {
        getWrappedValue().accept(QuasiListModification.instantiate(change, list));
    }
}

class InvalidationListenerWrapper<T>
extends WrapperBase<InvalidationListener>
implements QuasiChangeObserver<T> {
    private final ObservableList<T> list;

    public InvalidationListenerWrapper(
            ObservableList<T> list,
            InvalidationListener listener) {
        super(listener);
        this.list = list;
    }

    @Override
    public void onChange(QuasiListChange<? extends T> change) {
        getWrappedValue().invalidated(list);
    }
}

class ChangeListenerWrapper<T>
extends WrapperBase<ListChangeListener<? super T>>
implements QuasiChangeObserver<T> {
    private final ObservableList<T> list;

    public ChangeListenerWrapper(
            ObservableList<T> list,
            ListChangeListener<? super T> listener) {
        super(listener);
        this.list = list;
    }

    @Override
    public void onChange(QuasiListChange<? extends T> change) {

        List<? extends QuasiListModification<? extends T>> modifications =
                change.getModifications();

        if(modifications.isEmpty()) {
            return;
        }

        getWrappedValue().onChanged(new ListChangeListener.Change<T>(list) {

            private int current = -1;

            @Override
            public int getFrom() {
                return modifications.get(current).getFrom();
            }

            @Override
            protected int[] getPermutation() {
                return new int[0]; // not a permutation
            }

            /* Can change to List<? extends E> and remove unsafe cast when
             * https://javafx-jira.kenai.com/browse/RT-39683 is resolved. */
            @Override
            @SuppressWarnings("unchecked")
            public List<T> getRemoved() {
                // cast is safe, because the list is unmodifiable
                return (List<T>) modifications.get(current).getRemoved();
            }

            @Override
            public int getTo() {
                return modifications.get(current).getTo();
            }

            @Override
            public boolean next() {
                if(current + 1 < modifications.size()) {
                    ++current;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void reset() {
                current = -1;
            }
        });
    }
}

package org.reactfx.collection;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

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
import org.reactfx.util.Tuple2;
import org.reactfx.util.WrapperBase;
import org.reactfx.value.Val;

/**
 * Adds additional methods to {@link ObservableList}.
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

    default void addQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        addObserver(observer);
    }

    default void removeQuasiChangeObserver(QuasiChangeObserver<? super E> observer) {
        removeObserver(observer);
    }

    default void addQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        addObserver(observer);
    }

    default void removeQuasiModificationObserver(QuasiModificationObserver<? super E> observer) {
        removeObserver(observer);
    }

    default void addChangeObserver(Consumer<? super ListChange<? extends E>> observer) {
        addQuasiChangeObserver(new ChangeObserverWrapper<>(this, observer));
    }

    default void removeChangeObserver(Consumer<? super ListChange<? extends E>> observer) {
        removeQuasiChangeObserver(new ChangeObserverWrapper<>(this, observer));
    }

    default void addModificationObserver(Consumer<? super ListModification<? extends E>> observer) {
        addQuasiModificationObserver(new ModificationObserverWrapper<>(this, observer));
    }

    default void removeModificationObserver(Consumer<? super ListModification<? extends E>> observer) {
        removeQuasiModificationObserver(new ModificationObserverWrapper<>(this, observer));
    }

    default Subscription observeQuasiChanges(QuasiChangeObserver<? super E> observer) {
        addQuasiChangeObserver(observer);
        return () -> removeQuasiChangeObserver(observer);
    }

    default Subscription observeQuasiModifications(QuasiModificationObserver<? super E> observer) {
        addQuasiModificationObserver(observer);
        return () -> removeQuasiModificationObserver(observer);
    }

    default Subscription observeChanges(Consumer<? super ListChange<? extends E>> observer) {
        addChangeObserver(observer);
        return () -> removeChangeObserver(observer);
    }

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

    default Val<Integer> sizeProperty() {
        return sizeOf(this);
    }

    default <F> LiveList<F> map(Function<? super E, ? extends F> f) {
        return map(this, f);
    }

    default <F> LiveList<F> map(BiFunction<Integer, ? super E, ? extends F> f) {
        return map(this, f);
    }

    default <F> LiveList<F> mapDynamic(
            ObservableValue<? extends Function<? super E, ? extends F>> f) {
        return mapDynamic(this, f);
    }

    default SuspendableList<E> suspendable() {
        return suspendable(this);
    }

    default MemoizationList<E> memoize() {
        return memoize(this);
    }

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

    default EventStream<QuasiListChange<? extends E>> quasiChanges() {
        return new EventStreamBase<QuasiListChange<? extends E>>() {
            @Override
            protected Subscription observeInputs() {
                return observeQuasiChanges(this::emit);
            }
        };
    }

    default EventStream<ListChange<? extends E>> changes() {
        return quasiChanges().map(qc -> QuasiListChange.instantiate(qc, this));
    }

    default EventStream<QuasiListModification<? extends E>> quasiModifications() {
        return new EventStreamBase<QuasiListModification<? extends E>>() {
            @Override
            protected Subscription observeInputs() {
                return observeQuasiModifications(this::emit);
            }
        };
    }

    default EventStream<ListModification<? extends E>> modifications() {
        return quasiModifications().map(qm -> QuasiListModification.instantiate(qm, this));
    }


    /* ************** *
     * Static Methods *
     * ************** */

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

    static <E> EventStream<ListChange<? extends E>> changesOf(ObservableList<E> list) {
        return quasiChangesOf(list).map(qc -> QuasiListChange.instantiate(qc, list));
    }

    static Val<Integer> sizeOf(ObservableList<?> list) {
        return Val.create(() -> list.size(), list);
    }

    static <E, F> LiveList<F> map(
            ObservableList<? extends E> list,
            Function<? super E, ? extends F> f) {
        return new MappedList<>(list, f);
    }

    static <E, F> LiveList<F> map(
            ObservableList<? extends E> list,
            BiFunction<Integer, ? super E, ? extends F> f) {
        return new IndexedMappedList<>(list, f);
    }

    static <E, F> LiveList<F> mapDynamic(
            ObservableList<? extends E> list,
            ObservableValue<? extends Function<? super E, ? extends F>> f) {
        return new DynamicallyMappedList<>(list, f);
    }

    static <E> SuspendableList<E> suspendable(ObservableList<E> list) {
        if(list instanceof SuspendableList) {
            return (SuspendableList<E>) list;
        } else {
            return new SuspendableListWrapper<>(list);
        }
    }

    static <E> MemoizationList<E> memoize(ObservableList<E> list) {
        if(list instanceof MemoizationList) {
            return (MemoizationList<E>) list;
        } else {
            return new MemoizationListImpl<>(list);
        }
    }

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

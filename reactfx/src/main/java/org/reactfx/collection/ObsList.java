package org.reactfx.collection;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import org.reactfx.collection.ObsList.QuasiChangeObserver;
import org.reactfx.collection.ObsList.QuasiModificationObserver;
import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.WrapperBase;

/**
 * Adds additional methods to {@link ObservableList}.
 *
 * @param <E> type of list elements
 */
public interface ObsList<E> extends ObservableList<E> {

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

    /* **************** *
     * Abstract Methods *
     * **************** */

    void addQuasiChangeObserver(QuasiChangeObserver<? super E> observer);
    void removeQuasiChangeObserver(QuasiChangeObserver<? super E> observer);
    void addQuasiModificationObserver(QuasiModificationObserver<? super E> observer);
    void removeQuasiModificationObserver(QuasiModificationObserver<? super E> observer);


    /* *************** *
     * Default Methods *
     * *************** */

    default Subscription pin() {
        return observeQuasiChanges(qc -> {});
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

    default <F> ObsList<F> map(Function<? super E, ? extends F> f) {
        return map(this, f);
    }

    default SuspendableList<E> suspendable() {
        return suspendable(this);
    }

    default MemoizationList<E> memoize() {
        return memoize(this);
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
        if(list instanceof ObsList) {
            ObsList<? extends E> lst = (ObsList<? extends E>) list;
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
        if(list instanceof ObsList) {
            ObsList<E> lst = (ObsList<E>) list;
            return lst.quasiChanges();
        } else {
            return new EventStreamBase<QuasiListChange<? extends E>>() {
                @Override
                protected Subscription observeInputs() {
                    return ObsList.<E>observeQuasiChanges(list, this::emit);
                }
            };
        }
    }

    static <E> EventStream<ListChange<? extends E>> changesOf(ObservableList<E> list) {
        return quasiChangesOf(list).map(qc -> QuasiListChange.instantiate(qc, list));
    }

    static <E, F> ObsList<F> map(
            ObservableList<? extends E> list,
            Function<? super E, ? extends F> f) {
        return new MappedList<>(list, f);
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

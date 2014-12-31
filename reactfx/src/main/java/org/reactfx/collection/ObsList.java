package org.reactfx.collection;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import org.reactfx.collection.ObsList.ChangeObserver;
import org.reactfx.util.AccumulatorSize;

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
    public interface ChangeObserver<E>
    extends Observer<E, ListChange<? extends E>> {

        @Override
        default AccumulatorSize sizeOf(
                ListModificationSequence<? extends E> mods) {
            return AccumulatorSize.ONE;
        }

        @Override
        default ListChange<? extends E> headOf(
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
    public interface ModificationObserver<E>
    extends Observer<E, TransientListModification<? extends E>> {

        @Override
        default AccumulatorSize sizeOf(
                ListModificationSequence<? extends E> mods) {
            return AccumulatorSize.fromInt(mods.getModificationCount());
        }

        @Override
        default TransientListModification<? extends E> headOf(
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

    void addChangeObserver(ChangeObserver<? super E> observer);
    void removeChangeObserver(ChangeObserver<? super E> observer);
    void addModificationObserver(ModificationObserver<? super E> observer);
    void removeModificationObserver(ModificationObserver<? super E> observer);


    /* *************** *
     * Default Methods *
     * *************** */

    default Subscription observeChanges(ChangeObserver<? super E> observer) {
        addChangeObserver(observer);
        return () -> removeChangeObserver(observer);
    }

    default Subscription observeModifications(ModificationObserver<? super E> observer) {
        addModificationObserver(observer);
        return () -> removeModificationObserver(observer);
    }

    @Override
    default void addListener(ListChangeListener<? super E> listener) {
        addChangeObserver(new ChangeListenerWrapper<>(listener));
    }

    @Override
    default void removeListener(ListChangeListener<? super E> listener) {
        removeChangeObserver(new ChangeListenerWrapper<>(listener));
    }

    @Override
    default void addListener(InvalidationListener listener) {
        addChangeObserver(new InvalidationListenerWrapper<>(listener));
    }

    @Override
    default void removeListener(InvalidationListener listener) {
        removeChangeObserver(new InvalidationListenerWrapper<>(listener));
    }

    default <F> ObsList<F> map(Function<? super E, ? extends F> f) {
        return map(this, f);
    }

    default EventStream<ListChange<? extends E>> changes() {
        return new EventStreamBase<ListChange<? extends E>>() {
            @Override
            protected Subscription bindToInputs() {
                return observeChanges(this::emit);
            }
        };
    }

    default EventStream<TransientListModification<? extends E>> modifications() {
        return new EventStreamBase<TransientListModification<? extends E>>() {
            @Override
            protected Subscription bindToInputs() {
                return observeModifications(this::emit);
            }
        };
    }


    /* ************** *
     * Static Methods *
     * ************** */

    static <E> Subscription observeChanges(
            ObservableList<? extends E> list,
            ChangeObserver<? super E> observer) {

        if(list instanceof ObsList) {
            ObsList<? extends E> lst = (ObsList<? extends E>) list;
            return lst.observeChanges(observer);
        } else {
            ListChangeListener<E> listener = ch -> {
                ListChange<? extends E> change = ListChange.from(ch);
                observer.onChange(change);
            };
            list.addListener(listener);
            return () -> list.removeListener(listener);
        }
    }

    static <E> EventStream<ListChange<? extends E>> changesOf(ObservableList<E> list) {
        if(list instanceof ObsList) {
            ObsList<E> lst = (ObsList<E>) list;
            return lst.changes();
        } else {
            return new EventStreamBase<ListChange<? extends E>>() {
                @Override
                protected Subscription bindToInputs() {
                    return ObsList.<E>observeChanges(list, this::emit);
                }
            };
        }
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
            return new SuspendableListWrapper<E>(list);
        }
    }
}


class InvalidationListenerWrapper<T> implements ChangeObserver<T> {
    private final InvalidationListener delegate;

    public InvalidationListenerWrapper(InvalidationListener listener) {
        if(listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }

        this.delegate = listener;
    }

    @Override
    public void onChange(ListChange<? extends T> change) {
        delegate.invalidated(change.getModifications().get(0).getList());
    }

    @Override
    public boolean equals(Object that) {
        if(that instanceof InvalidationListenerWrapper) {
            return Objects.equals(
                    ((InvalidationListenerWrapper<?>) that).delegate,
                    this.delegate);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}

class ChangeListenerWrapper<T> implements ChangeObserver<T> {
    private final ListChangeListener<? super T> delegate;

    public ChangeListenerWrapper(ListChangeListener<? super T> listener) {
        if(listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }

        this.delegate = listener;
    }

    @Override
    public void onChange(ListChange<? extends T> change) {
        change.toJavaFx().ifPresent(delegate::onChanged);
    }

    @Override
    public boolean equals(Object that) {
        if(that instanceof ChangeListenerWrapper) {
            return Objects.equals(
                    ((ChangeListenerWrapper<?>) that).delegate,
                    this.delegate);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
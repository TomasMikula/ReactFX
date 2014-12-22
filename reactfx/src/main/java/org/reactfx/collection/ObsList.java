package org.reactfx.collection;

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;
import org.reactfx.collection.ObsList.ChangeObserver;

/**
 * Adds additional methods to {@link ObservableList}.
 *
 * @param <E> type of list elements
 */
public interface ObsList<E> extends ObservableList<E> {

    /* ************ *
     * Nested Types *
     * ************ */

    @FunctionalInterface
    public interface ChangeObserver<E> {
        void onChange(ListChange<? extends E> change);
    }

    /* **************** *
     * Abstract Methods *
     * **************** */

    void addChangeObserver(ChangeObserver<? super E> observer);
    void removeChangeObserver(ChangeObserver<? super E> observer);


    /* *************** *
     * Default Methods *
     * *************** */

    default Subscription observeChanges(ChangeObserver<? super E> observer) {
        addChangeObserver(observer);
        return () -> removeChangeObserver(observer);
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

    static <E, F> ObsList<F> map(
            ObservableList<? extends E> list,
            Function<? super E, ? extends F> f) {
        return new MappedList<>(list, f);
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
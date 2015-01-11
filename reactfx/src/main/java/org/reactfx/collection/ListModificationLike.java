package org.reactfx.collection;

import java.util.List;

import javafx.collections.ObservableList;

/**
 * Describes an elementary modification made to a list. Elementary modification
 * is represented by the position in the list where the change occurred
 * ({@link #getFrom()}), a list of elements removed by the modification
 * ({@link #getRemoved()}), and the number of added elements replacing the
 * removed elements ({@link #getAddedSize()}).
 *
 * <p>Subtypes of this interface differ in how they refer to the added elements:
 *   <ul>
 *     <li>{@link ListModification} holds a reference to the observable list
 *     in which the modification occurred and the
 *     {@link ListModification#getAddedSubList()} method returns a sublist view
 *     of the original observable list. Such sublist is valid only until the
 *     next list modification.
 *     <li>{@link MaterializedListModification} has its own copy of added
 *     elements ({@link MaterializedListModification#getAdded()}), thus the
 *     validity of the list of added elements returned from {@code getAdded()}
 *     is not restricted.
 *     <li>{@link QuasiListModification} does not provide a way to get the
 *     added elements directly, but has to be combined with the observable list
 *     to obtain {@linkplain ListModification}
 *     ({@link QuasiListModification#instantiate(QuasiListModification, ObservableList)})
 *     or {@linkplain MaterializedListModification}
 *     ({@link QuasiListModification#materialize(QuasiListModification, ObservableList)}).
 *   </ul>
 *
 * @param <E> type of list elements.
 */
interface ListModificationLike<E> {
    /**
     * Returns the position in the list where this change occurred.
     */
    int getFrom();

    /**
     * Returns the number of items removed by this change.
     */
    default int getRemovedSize() { return getRemoved().size(); }

    /**
     * Returns the number of items added by this change.
     */
    int getAddedSize();

    /**
     * Returns the end position of the change in the modified list.
     * The returned value is equal to {@code getFrom() + getAddedSize()}.
     */
    default int getTo() { return getFrom() + getAddedSize(); }

    /**
     * Returns an <em>immutable</em> list of elements removed by this change.
     * Before the change occurred, the first element of the returned list
     * was at index {@link #getFrom()} in the original list.
     * If no elements were removed by this change, returns an empty list.
     * The size of the returned list is equal to the value returned by
     * {@link #getRemovedSize()}.
     */
    List<? extends E> getRemoved();
}

abstract class ListModificationBase<E> implements ListModificationLike<E> {
    private final QuasiListModification<? extends E> template;

    ListModificationBase(QuasiListModification<? extends E> template) {
        this.template = template;
    }

    @Override
    public int getFrom() {
        return template.getFrom();
    }

    @Override
    public List<? extends E> getRemoved() {
        return template.getRemoved();
    }

    @Override
    public int getAddedSize() {
        return template.getAddedSize();
    }

    QuasiListModification<? extends E> getTemplate() {
        return template;
    }
}
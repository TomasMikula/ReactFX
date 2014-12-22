package org.reactfx.collection;

import java.util.List;

/**
 * Represents a single modification made to a list. Each modification is
 * represented by the position in the list where the change occurred
 * ({@link #getFrom()}), a list of elements removed by the modification
 * ({@link #getRemoved()}, and a list of elements added by the modification.
 * Depending on whether this is {@link TransientListModification} or
 * {@link MaterializedListModification}, the list of added elements links to the
 * original list, or is completely independent of the original list,
 * respectively.
 *
 * <p>This is a simpler model of a list change than
 * {@link javafx.collections.ListChangeListener.Change}.
 *
 * @param <E> type of list elements.
 */
interface ListModification<E> {
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

abstract class ListModificationBase<E> implements ListModification<E> {
    private final int from;
    private final List<? extends E> removed;

    ListModificationBase(int from, List<? extends E> removed) {
        this.from = from;
        this.removed = removed;
    }

    @Override
    public int getFrom() {
        return from;
    }

    @Override
    public List<? extends E> getRemoved() {
        return removed;
    }

}
package org.reactfx.util;

import java.util.List;

/**
 * Represents a change made to a list. Each change is represented by the
 * position in the list where the change occurred ({@link #getFrom()}),
 * a list of elements removed by this change ({@link #getRemoved()}, and
 * a list of elements added by this change. Depending on whether this is
 * {@link TransientListChange} or {@link MaterializedListChange}, the list
 * of added elements links to the original list, or is completely independent
 * of the original list, respectively.
 *
 * <p>This is a simpler model of a list change than
 * {@link javafx.collections.ListChangeListener.Change}.
 *
 * @param <E> type of list elements.
 */
interface ListChange<E> {
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

abstract class ListChangeBase<E> implements ListChange<E> {
    private final int from;
    private final List<? extends E> removed;

    ListChangeBase(int from, List<? extends E> removed) {
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
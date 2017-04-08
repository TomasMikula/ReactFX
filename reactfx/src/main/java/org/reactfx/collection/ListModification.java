package org.reactfx.collection;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;

import org.reactfx.util.Lists;

/**
 * A sub-interface of {@link ListModificationLike} that holds a reference to the observable list in which the
 * modification occurred and whose {@link #getAddedSubList()} method returns a sublist view of
 * the original observable list. Such sublist is valid only until the next list modification.
 */
public interface ListModification<E> extends ListModificationLike<E> {

    List<? extends E> getAddedSubList();

    /**
     * Trims the common prefix and suffix of the {@link #getRemoved()} and {@link #getAddedSubList()} using
     * {@link Lists#commonPrefixSuffixLengths(List, List)}.
     */
    ListModification<E> trim();

    /**
     * Converts this into a {@link MaterializedListModification}.
     */
    default MaterializedListModification<E> materialize() {
        return MaterializedListModification.create(
                getFrom(),
                getRemoved(),
                new ArrayList<>(getAddedSubList()));
    }
}

final class ListModificationImpl<E>
implements ListModification<E> {
    private int position;
    private List<? extends E> removed;
    private int addedSize;
    private final ObservableList<E> list;

    ListModificationImpl(
            int position,
            List<? extends E> removed,
            int addedSize,
            ObservableList<E> list) {
        this.position = position;
        this.removed = removed;
        this.addedSize = addedSize;
        this.list = list;
    }

    @Override
    public int getFrom() {
        return position;
    }

    @Override
    public List<? extends E> getRemoved() {
        return removed;
    }

    @Override
    public List<? extends E> getAddedSubList() {
        return list.subList(position, position + addedSize);
    }

    @Override
    public int getAddedSize() {
        return addedSize;
    }

    @Override
    public String toString() {
        return "[modification at: " + getFrom() +
                ", removed: " + getRemoved() +
                ", added size: " + getAddedSize() + "]";
    }

    @Override
    public ListModification<E> trim()  {
        return Lists.commonPrefixSuffixLengths(removed, getAddedSubList()).map((pref, suff) -> {
            if(pref == 0 && suff == 0) {
                return this;
            } else {
                return new ListModificationImpl<>(
                        position + pref,
                        removed.subList(pref, getRemovedSize() - suff),
                        addedSize - pref - suff,
                        list);
            }
        });
    }
}
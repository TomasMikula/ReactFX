package org.reactfx.collection;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;

public interface ListModification<E> extends ListModificationLike<E> {

    List<? extends E> getAddedSubList();

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
}
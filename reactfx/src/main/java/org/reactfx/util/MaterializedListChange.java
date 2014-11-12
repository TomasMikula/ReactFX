package org.reactfx.util;

import java.util.List;

public interface MaterializedListChange<E> extends ListChange<E> {
    List<E> getAdded();

    @Override
    default int getAddedSize() { return getAdded().size(); }
}

final class MaterializedListChangeImpl<E> extends ListChangeBase<E> implements MaterializedListChange<E> {
    private final List<E> added;

    MaterializedListChangeImpl(int from, List<? extends E> removed, List<E> added) {
        super(from, removed);
        this.added = added;
    }

    @Override
    public List<E> getAdded() {
        return added;
    }
}
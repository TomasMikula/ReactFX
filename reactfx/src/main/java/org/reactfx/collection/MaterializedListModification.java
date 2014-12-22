package org.reactfx.collection;

import java.util.List;

public interface MaterializedListModification<E> extends ListModification<E> {
    List<E> getAdded();

    @Override
    default int getAddedSize() { return getAdded().size(); }
}

final class MaterializedListModificationImpl<E>
extends ListModificationBase<E>
implements MaterializedListModification<E> {
    private final List<E> added;

    MaterializedListModificationImpl(
            int from,
            List<? extends E> removed,
            List<E> added) {
        super(from, removed);
        this.added = added;
    }

    @Override
    public List<E> getAdded() {
        return added;
    }
}
package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface MaterializedListModification<E> extends ListModificationLike<E> {
    List<E> getAdded();

    @Override
    default int getAddedSize() { return getAdded().size(); }
}

final class MaterializedListModificationImpl<E>
extends ListModificationBase<E>
implements MaterializedListModification<E> {
    private final List<E> added;

    MaterializedListModificationImpl(
            QuasiListModification<? extends E> template,
            List<E> addedSublist) {
        super(template);
        this.added = Collections.unmodifiableList(new ArrayList<>(addedSublist));
    }

    @Override
    public List<E> getAdded() {
        return added;
    }
}
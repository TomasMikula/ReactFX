package org.reactfx.collection;

import java.util.List;

import javafx.collections.ObservableList;

public interface ListModification<E> extends ListModificationLike<E> {

    List<? extends E> getAddedSubList();

    MaterializedListModification<E> materialize();
}

final class ListModificationImpl<E>
extends ListModificationBase<E>
implements ListModification<E> {
    private final ObservableList<E> list;

    ListModificationImpl(
            QuasiListModification<? extends E> template,
            ObservableList<E> list) {
        super(template);
        this.list = list;
    }

    @Override
    public List<? extends E> getAddedSubList() {
        return list.subList(getFrom(), getTo());
    }

    @Override
    public MaterializedListModification<E> materialize() {
        return QuasiListModification.materialize(getTemplate(), list);
    }
}
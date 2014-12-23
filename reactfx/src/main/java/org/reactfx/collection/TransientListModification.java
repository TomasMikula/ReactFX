package org.reactfx.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

public interface TransientListModification<E> extends ListModification<E> {

    ObservableList<? extends E> getList();

    default List<? extends E> getAddedSubList() {
        return getList().subList(getFrom(), getTo());
    }

    default ListChange<E> asListChange() {
        return () -> Collections.singletonList(TransientListModification.this);
    }

    default MaterializedListModification<E> materialize() {
        List<E> added = new ArrayList<>(getAddedSubList());
        added = Collections.unmodifiableList(added);
        return new MaterializedListModificationImpl<>(getFrom(), getRemoved(), added);
    }

    static <E, F extends E> TransientListModification<E> fromCurrentStateOf(Change<F> ch) {
        List<F> list = ch.getList();
        int from = ch.getFrom();
        int to = ch.getTo();

        List<F> removed;
        if(ch.wasPermutated()) {
            int len = to - from;
            removed = new ArrayList<>(len);
            for(int i = 0; i < len; ++i) {
                int pi = ch.getPermutation(from + i);
                removed.add(list.get(pi));
            }
        } else {
            removed = ch.getRemoved();
        }
        return new TransientListModificationImpl<>(ch.getList(), from, to, removed);
    }

    @SuppressWarnings("unchecked")
    static <E> TransientListModification<E> safeCast(
            TransientListModification<? extends E> mod) {
        // the cast is safe, because instances are immutable
        return (TransientListModification<E>) mod;
    }
}

final class TransientListModificationImpl<E>
extends ListModificationBase<E>
implements TransientListModification<E> {
    private final ObservableList<? extends E> list;
    private final int to;

    TransientListModificationImpl(
            ObservableList<? extends E> list,
            int from, int to,
            List<? extends E> removed) {
        super(from, removed);
        this.list = list;
        this.to = to;
    }

    @Override
    public int getAddedSize() {
        return to - getFrom();
    }

    @Override
    public int getTo() {
        return to;
    }

    @Override
    public ObservableList<? extends E> getList() {
        return list;
    }
}
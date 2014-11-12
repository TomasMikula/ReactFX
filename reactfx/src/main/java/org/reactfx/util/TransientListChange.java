package org.reactfx.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener.Change;

public interface TransientListChange<E> extends ListChange<E> {
    List<? extends E> getList();

    default List<? extends E> getAddedSublist() {
        return getList().subList(getFrom(), getTo());
    }

    default MaterializedListChange<E> materialize() {
        List<E> added = new ArrayList<>(getAddedSize());
        Collections.copy(added, getAddedSublist());
        added = Collections.unmodifiableList(added);
        return new MaterializedListChangeImpl<>(getFrom(), getRemoved(), added);
    }

    static <E, F extends E> TransientListChange<E> fromCurrentStateOf(Change<F> ch) {
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
        return new TransientListChangeImpl<>(ch.getList(), from, to, removed);
    }
}

final class TransientListChangeImpl<E> extends ListChangeBase<E> implements TransientListChange<E> {
    private final List<? extends E> list;
    private final int to;

    TransientListChangeImpl(List<? extends E> list, int from, int to, List<? extends E> removed) {
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
    public List<? extends E> getList() {
        return list;
    }
}